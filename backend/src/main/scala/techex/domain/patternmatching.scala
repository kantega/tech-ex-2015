package techex.domain

import org.joda.time.{ReadableInstant, Duration}
import techex.data.{windows, Window}
import patternmatching._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scalaz._, Scalaz._

object patternmatching {

  type Pred[A] = A => Boolean

  def await[A](f: Fact => Matcher[A]) = {
    Await(MF(f))
  }

  def await[A](desc: String, f: Fact => Matcher[A]) = {
    Await(MF(f, desc))
  }

  def halt[A](m: Matcher[A]) =
    m.haltOn

  def on[FF <: Fact](implicit evidence: ClassTag[FF]): Matcher[FF] = {
    onFact("on" + evidence, {
      case f: FF => true
      case _     => false
    }).map(a => a.asInstanceOf[FF])
  }

  def onFact(pred: Pred[Fact]): Matcher[Fact] =
    await("a(" + pred + ")", p =>
      if (pred(p)) {
        Emit(p, onFact(pred))
      }
      else
        onFact(pred))

  def onFact(desc: String, pred: Pred[Fact]): Matcher[Fact] =
    await("a(" + desc + ")", p =>
      if (pred(p)) {
        Emit(p, onFact(desc, pred))
      }
      else
        onFact(desc, pred))

  implicit def toMatcher(pred: Pred[Fact]): Matcher[Fact] =
    onFact(pred)

}


object preds {

  implicit class PredOps[A](pred: Pred[A]) {
    def and(other: Pred[A]) =
      preds.and(pred, other)

    def or(other: Pred[A]) =
      preds.or(pred, other)
  }

  def not[A](pred: Pred[A]): Pred[A] =
    ctx => !pred(ctx)

  def and[A](one: Pred[A], other: Pred[A]): Pred[A] =
    ctx => one(ctx) && other(ctx)

  def or[A](one: Pred[A], other: Pred[A]): Pred[A] =
    ctx => one(ctx) || other(ctx)

  def fact(f: PartialFunction[Fact, Boolean]): Fact => Boolean = {
    update =>
      if (f.isDefinedAt(update))
        f(update)
      else
        false
  }
}


trait Matcher[A] {

  import patternmatching._

  def map[B](f: A => B): Matcher[B] =
    this match {
      case Halt()     => Halt()
      case Emit(m, n) => Emit(f(m), n.map(f))
      case Await(nf)  => await(nf + ".mapped", fact => nf(fact).map(f))
    }


  def check(fact: Fact): (Matcher[A], Option[A]) = {
    step(fact) match {
      case Halt()      => (Halt(), None)
      case Emit(m, n)  => (n, Some(m))
      case a: Await[A] => (a, None)
    }
  }


  def step(fact: Fact): Matcher[A] = {
    def go(p: Matcher[A]): Matcher[A] = {
      p match {
        case Halt()              => Halt()
        case Emit(matches, next) => throw new Exception("You cannot step a match")
        case Await(f)            => f(fact)
      }
    }
    val ntx = go(this)
    ntx
  }

  def ++[B](a: Matcher[B]) =
    and(a)


  def until[B](m: Matcher[B]): Matcher[(A, B)] =
    and(m.haltAfter)

  def before[B](m: Matcher[B]): Matcher[A] =
    xor(m.haltOn).collect { case -\/(a) => a}

  def ||(m: Matcher[A]) =
    or(m)


  def end: Matcher[A] = this match {
    case Halt()      => Halt()
    case Emit(p, n)  => Emit[A](p, Halt())
    case a: Await[A] => await("end", p => a.f(p).end)
  }


  def collect[B](pf: PartialFunction[A, B]): Matcher[B] = {
    filter(pf.isDefinedAt).map(pf)
  }

  def filter(f: Pred[A]): Matcher[A] =
    this match {
      case Halt()      => Halt()
      case Emit(p, n)  => if (f(p)) Emit(p, n.filter(f)) else n.filter(f)
      case a: Await[A] => await("filter", p => a.f(p).filter(f))
    }

  def last: Matcher[A] =
    this match {
      case Halt()      => Halt()
      case Emit(m, n)  => Emit(m, n.lastC(m))
      case a: Await[A] => await("last(" + a + ")", p => a.f(p).last)
    }

  def lastC(cache: A): Matcher[A] = {
    this match {
      case Halt()      => await("lastC(" + cache + ")", p => valueForever(cache))
      case Emit(m, n)  => Emit(m, n.lastC(m))
      case a: Await[A] => await("lastC(" + a.f + "," + cache + ")", p => {
        a.f(p) match {
          case an: Await[A]   => Emit(cache, an.lastC(cache))
          case nn: Matcher[A] => nn.lastC(cache)
        }
      })
    }
  }

  def first: Matcher[A] =
    this match {
      case Halt()     => Halt()
      case Emit(m, n) => Emit(m, await("forever(" + m + ")", p => valueForever(m)))
      case Await(f)   => await("first(" + f + ")", p => f(p).first)
    }

  private def valueForever(matched: A): Matcher[A] =
    Emit(matched, await("forever(" + matched + ")", p => valueForever(matched)))


  def accumD(duration: Duration)(implicit ev: A => ReadableInstant): Matcher[List[A]] = {
    this match {
      case Halt()     => Halt()
      case Emit(p, n) =>
        val acs = p :: windows.time[A](duration, ev)
        Emit(acs.contents.toList, await("awaitAccum(" + acs + ")", p => n.step(p).accumNonEmpty(acs)))
      case Await(f)   => await("awaitAccum(" + f + ")", p => f(p).accumD(duration))
    }
  }

  def accumN(size: Int): Matcher[List[A]] = {
    this match {
      case Halt()     => Halt()
      case Emit(p, n) =>
        val acs = p :: windows.sized(size)
        Emit(acs.contents.toList, await("awaitAccum(" + acs + ")", p => n.step(p).accumNonEmpty(acs)))
      case Await(f)   => await("awaitAccum(" + f + ")", p => f(p).accumN(size))
    }
  }

  def accumNonEmpty(w: Window[A]): Matcher[List[A]] = {
    this match {
      case Halt()     => Halt()
      case Emit(p, n) =>
        val acs = p :: w
        Emit(acs.contents.toList, await("awaitAccum(" + acs + ")", p => n.step(p).accumNonEmpty(acs)))
      case Await(f)   => Emit(w.contents.toList, await("awaitAccum(" + f + "," + w + ")", p => f(p).accumNonEmpty(w)))
    }
  }

  def repeat: Matcher[A] =
    repeatForever(this)

  private def repeatForever(original: Matcher[A]): Matcher[A] = {
    this match {
      case Halt()     => original.repeatForever(original)
      case Emit(m, n) => Emit(m, original.repeat)
      case Await(f)   => await("repeat(" + f + ")", p => f(p).repeatForever(original))
    }
  }


  def times(times: Int): Matcher[List[A]] =
    repeat(this, times, nil[A])


  private def repeat(original: Matcher[A], times: Int, matched: List[A]): Matcher[List[A]] = {
    if (times == 0)
      Emit(matched, Halt())
    else
      this match {
        case Halt()     => original.repeat(original, times, matched)
        case Emit(m, n) => original.repeat(original, times - 1, m :: matched)
        case Await(f)   => await("repeat(" + f + ")", p => f(p).repeat(original, times, matched))
      }
  }

  def xor[B](other: Matcher[B]): Matcher[A \/ B] =
    (this, other) match {
      case (Halt(), b)                  => b.map(x => \/-(x))
      case (a, Halt())                  => a.map(x => -\/(x))
      case (Emit(m1, n1), Emit(m2, n2)) => await(p => n1 xor n2)
      case (Emit(m1, n1), b: Await[B])  => Emit(m1.left, n1 xor b)
      case (a: Await[A], Emit(m2, n2))  => Emit(m2.right, a xor n2)
      case (a: Await[A], b: Await[B])   => await(a + " xor " + b, p => a.f(p) xor b.f(p))
    }


  def or[B](other: Matcher[B]): Matcher[(Option[A], Option[B])] =
    (this, other) match {
      case (Halt(), b)                  => b.map(x => (none, x.some))
      case (a, Halt())                  => a.map(x => (x.some, none))
      case (Emit(m1, n1), Emit(m2, n2)) => Emit((m1.some, m2.some), n1 or n2)
      case (Emit(m1, n1), b: Await[B])  => Emit((m1.some, none[B]), n1 or b)
      case (a: Await[A], Emit(m2, n2))  => Emit((none[A], m2.some), a or n2)
      case (a: Await[A], b: Await[B])   => await(p => a.f(p) or b.f(p))
    }

  def and[B](other: Matcher[B]): Matcher[(A, B)] = {
    (this, other) match {
      case (Halt(), _) | (_, Halt())    => Halt()
      case (Emit(m1, n1), Emit(m2, n2)) => Emit((m1, m2), n1 and n2)
      case (Emit(m1, n1), b: Await[B])  => n1 and b
      case (a: Await[A], Emit(m2, n2))  => a and n2
      case (a: Await[A], b: Await[B])   => await(a + " and " + b, p => a.f(p) and b.f(p))
    }
  }

  def haltAfter: Matcher[A] =
    this match {
      case Halt()     => Halt()
      case Emit(p, n) => Emit(p, Halt())
      case Await(f)   => await("haltAfter(" + f + ")", p => f(p).haltAfter)
    }

  def haltOn: Matcher[A] =
    this match {
      case Halt()     => Halt()
      case Emit(p, n) => Halt()
      case Await(f)   => await(p => f(p).haltOn)
    }


  def fby[B](other: Matcher[B]): Matcher[(A, B)] =
    (this, other) match {
      case (Halt(), _) | (_, Halt())    => Halt()
      case (Emit(m1, n1), Emit(m2, n2)) => n1.fby2(n2, m1)
      case (Emit(m1, n1), a2: Await[B]) => n1.fby2(a2, m1)
      case (a1: Await[A], Emit(m2, n2)) => a1.fby(n2)
      case (a1: Await[A], a2: Await[B]) => await(p => a1.f(p) fby a2)
    }

  def fby2[B](other: Matcher[B], cached: A): Matcher[(A, B)] =
    (this, other) match {
      case (Halt(), _) | (_, Halt())    => Halt()
      case (Emit(m1, n1), Emit(m2, n2)) => Emit((cached, m2), n1.fby2(n2, m1))
      case (Emit(m1, n1), a2: Await[B]) => n1.fby2(a2, m1)
      case (a1: Await[A], Emit(m2, n2)) => Emit((cached, m2), a1.fby2(n2, cached))
      case (a1: Await[A], a2: Await[B]) => await(p => a1.f(p).fby2(a2.f(p), cached))
    }
}


trait HaltOrAwait[A] extends Matcher[A]

object Pattern {
  def apply(facts: List[Fact]): Pattern = Pattern(facts, Nil)
}

case class Pattern(facts: List[Fact], history: List[Fact]) {
  def ++(m: Pattern) = Pattern(facts ::: m.facts, history)

  def withHistory(history: Pattern) = Pattern(facts, history.facts)

  def factsAndHistory = facts ::: history

  def latest = facts.head.instant
}

trait MatchFunc[A] {
  def apply(fact: Fact): Matcher[A]
}
case class MF[A](f: Fact => Matcher[A], desc: String = "f(?)") extends MatchFunc[A] {
  def apply(fact: Fact) = f(fact)

  override def toString = desc
}
case class Halt[A]() extends HaltOrAwait[A]
case class Emit[A](p: A, next: Matcher[A]) extends Matcher[A]
case class Await[A](f: MatchFunc[A]) extends HaltOrAwait[A] {
  override def toString = f.toString
}

/*
case class FBy(one: Matcher, other: Matcher) extends MatchFunc {
  def apply(p: Pattern) = {
    (one.step(p), other.step(p)) match {
      case (Halt(), _) | (_, Halt()) => {println(" xxx fby halting"); Halt()}
      case (Match(m, n), _)          => Await(MatchedFBy(n, other, p))
      case (a: Await, _)             => Await(FBy(a, other))

    }
  }

  override def toString = one + " ~> " + other
}

case class MatchedFBy(one: Matcher, other: Matcher, consumed: Pattern) extends MatchFunc {
  def apply(p: Pattern) = {

    (other.step(p withHistory consumed), one.step(p)) match {
      case (Halt(), _) | (_, Halt())      => {println(" xxx fby halting"); Halt()}
      case (Match(m2, n2), Match(m1, n1)) => Match(m2 ++ consumed, Await(MatchedFBy(n1, n2, m1)))
      case (a2: Await, Match(m1, n1))     => Await(MatchedFBy(n1, a2, m1))
      case (Match(m2, n2), a1: Await)     => Match(m2 ++ consumed, Await(FBy(a1, n2)))
      case (a2: Await, a1: Await)         => Await(FBy(a1, a2))
    }
  }

  override def toString = "m(" + consumed.facts.mkString(",") + ") ~> " + other
}
*/