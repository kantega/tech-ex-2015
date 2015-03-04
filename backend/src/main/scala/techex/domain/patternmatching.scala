package techex.domain

import org.joda.time.Duration
import techex.data.{windows, Window}
import patternmatching._
import scalaz._, Scalaz._

object patternmatching {

  type Pred = Pattern => Boolean

  def await(f: Pattern => Matcher) = {
    Await(MF(f))
  }

  def await(desc: String, f: Pattern => Matcher) = {
    Await(MF(f, desc))
  }

  def halt(m: Matcher) =
    m.haltOn


  def on(pred: Pred): Matcher =
    await("a(" + pred + ")", p =>
      if (pred(p)) {
        Match(p, on(pred))
      }
      else
        on(pred))

  def on(desc: String, pred: Pred): Matcher =
    await("a(" + desc + ")", p =>
      if (pred(p)) {
        Match(p, on(desc, pred))
      }
      else
        on(desc, pred))

  implicit def toMatcher(pred: Pred): Matcher =
    on(pred)

}


object preds {

  implicit class PredOps(pred: Pred) {
    def and(other: Pred) =
      preds.and(pred, other)

    def or(other: Pred) =
      preds.or(pred, other)
  }

  def not(pred: Pred): Pred =
    ctx => !pred(ctx)

  def and(one: Pred, other: Pred): Pred =
    ctx => one(ctx) && other(ctx)

  def or(one: Pred, other: Pred): Pred =
    ctx => one(ctx) || other(ctx)


  def pattern(f: PartialFunction[(Fact, List[Fact]), Boolean]): Pred =
    ctx => {
      if (f.isDefinedAt((ctx.facts.head, ctx.facts.tail)))
        f((ctx.facts.head, ctx.facts.tail))
      else
        false
    }

  def head(f: PartialFunction[Fact, Boolean]): Pred =
    ctx => {
      if (f.isDefinedAt(ctx.facts.head))
        f(ctx.facts.head)
      else
        false
    }

  def history(f: PartialFunction[List[Fact], Boolean]): Pred = {
    ctx => {
      if (f.isDefinedAt(ctx.factsAndHistory))
        f(ctx.factsAndHistory)
      else
        false
    }
  }

  def fact(f: PartialFunction[Fact, Boolean]): Fact => Boolean = {
    update =>
      if (f.isDefinedAt(update))
        f(update)
      else
        false
  }
}

trait Matcher {

  import patternmatching._

  def check(fact: Fact): (Matcher, Option[Pattern]) = {
    step(Pattern(List(fact))) match {
      case Halt()      => (Halt(), None)
      case Match(m, n) => (n, Some(m))
      case a: Await    => (a, None)
    }
  }


  def step(fact: Pattern): Matcher = {
    def go(p: Matcher): Matcher = {
      p match {
        case Halt()               => Halt()
        case Match(matches, next) => go(next) match {
          case Match(m2, n2) => Match(m2 ++ matches, n2)
          case a@_           => Match(matches, a)
        }
        case Await(f)             => f(fact)
      }
    }
    val ntx = go(this)
    ntx
  }

  def ++(a: Matcher) =
    and(a)


  def ~>(m: Matcher) =
    fby(m)

  def ~><(m: Matcher) =
    and(m.haltAfter)

  def ||(m: Matcher) =
    or(m)


  def end: Matcher = this match {
    case Halt()      => Halt()
    case Match(p, n) => Match(p, Halt())
    case a: Await    => await("end", p => a.step(p).end)
  }


  def last: Matcher =
    this match {
      case Halt()      => Halt()
      case Match(m, n) => Match(m, n.lastC(m))
      case a: Await    => await("last(" + a + ")", p => a.step(p).last)
    }

  def filter(f: Pattern => Boolean): Matcher =
    this match {
      case Halt()      => Halt()
      case Match(p, n) => if (f(p)) Match(p, n) else n
      case a: Await    => await("end", p => a.step(p).filter(f))
    }

  private def lastC(cache: Pattern): Matcher = {
    this match {
      case Halt()      => Match(cache, await("lastC(" + cache + ")", p => valueForever(cache)))
      case Match(m, n) => Match(m, n.lastC(m))
      case a: Await    => Match(cache, await("lastC(" + a.f + "," + cache + ")", p => a.f(p).lastC(cache)))
    }
  }

  def first: Matcher =
    this match {
      case Halt()      => Halt()
      case Match(m, n) => Match(m, await("forever(" + m.facts.head + ")", p => valueForever(m)))
      case Await(f)    => await("first(" + f + ")", p => f(p).first)
    }

  private def valueForever(matched: Pattern): Matcher =
    Match(matched, await("forever(" + matched.facts.head + ")", p => valueForever(matched)))


  def accumD(duration: Duration): Matcher = {
    this match {
      case Halt()      => Halt()
      case Match(p, n) =>
        val acs = p.facts ::: windows.time(duration, (fact: Fact) => fact.instant)
        Match(Pattern(acs.contents.toList), await("awaitAccum(" + acs + ")", p => n.step(p).accumNonEmpty(acs)))
      case Await(f)    => await("awaitAccum(" + f + ")", p => f(p).accumD(duration))
    }
  }

  def accumN(size: Int): Matcher = {
    this match {
      case Halt()      => Halt()
      case Match(p, n) =>
        val acs = p.facts ::: windows.sized(size)
        Match(Pattern(acs.contents.toList), await("awaitAccum(" + acs + ")", p => n.step(p).accumNonEmpty(acs)))
      case Await(f)    => await("awaitAccum(" + f + ")", p => f(p).accumN(size))
    }
  }

  def accumNonEmpty(w: Window[Fact]): Matcher = {
    this match {
      case Halt()      => Halt()
      case Match(p, n) => {
        val acs = p.facts ::: w
        Match(Pattern(acs.contents.toList), await("awaitAccum(" + acs + ")", p => n.step(p).accumNonEmpty(acs)))
      }
      case Await(f)    => Match(Pattern(w.contents.toList), await("awaitAccum(" + f + "," + w + ")", p => f(p).accumNonEmpty(w)))
    }
  }

  def repeat: Matcher =
    repeatForever(this)

  private def repeatForever(original: Matcher): Matcher = {
    this match {
      case Halt()      => original.repeatForever(original)
      case Match(m, n) => Match(m, original.repeat)
      case Await(f)    => await("repeat(" + f + ")", p => f(p).repeatForever(original))
    }
  }


  def times(times: Int): Matcher =
    repeat(this, times, none)


  private def repeat(original: Matcher, times: Int, matched: Option[Pattern]): Matcher = {
    if (times == 0)
      matched.fold[Matcher](Halt())(m => Match(m, Halt()))
    else
      this match {
        case Halt()      => original.repeat(original, times - 1, matched)
        case Match(m, n) => original.repeat(original, times - 1, matched.fold(Some(m))(x => Some(m ++ x)))
        case Await(f)    => await("repeat(" + f + ")", p => f(p).repeat(original, times, matched))
      }
  }

  def xor(other: Matcher): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => await(p => n1 xor n2)
      case (Match(m1, n1), b: Await)      => Match(m1, n1 xor b)
      case (a: Await, Match(m2, n2))      => Match(m2, a xor n2)
      case (a: Await, b: Await)           => await(a + " xor " + b, p => a.f(p) xor b.f(p))
    }


  def or(other: Matcher): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m2 ++ m1, n1 or n2)
      case (Match(m1, n1), b: Await)      => Match(m1, n1 or b) // await(p => n1.step(p) or b.step(p)))
      case (a: Await, Match(m2, n2))      => Match(m2, a or n2) // await(p => a.step(p) or n2.step(p)))
      case (a: Await, b: Await)           => await(p => a.f(p) or b.f(p))
    }

  def and(other: Matcher): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m2 ++ m1, n1 and n2)
      case (Match(m1, n1), b: Await)      => n1 and b //await(n1 + " *and " + b, p => n1.step(p) and b.step(p))
      case (a: Await, Match(m2, n2))      => a and n2 //await(a + " and* " + n2, p => a.step(p) and n2.step(p))
      case (a: Await, b: Await)           => await(a + " and " + b, p => a.f(p) and b.f(p))
    }

  def haltAfter: Matcher =
    this match {
      case Halt()      => Halt()
      case Match(p, n) => Match(p, Halt())
      case Await(f)    => await("haltAfter(" + f + ")", p => f(p).haltAfter)
    }

  def haltOn: Matcher =
    this match {
      case Halt()      => Halt()
      case Match(p, n) => Halt()
      case Await(f)    => await(p => f(p).haltOn)
    }

  def fby(other: Matcher): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m2 ++ m1, n1.fby(n2, m1))
      case (Match(m1, n1), a2: Await)     => n1.fby(a2, m1)
      case (a1: Await, Match(m2, n2))     => a1.fby(n2)
      case (a1: Await, a2: Await)         => await(a1 + "~>" + a2, p => a1.f(p).fby(a2)) //Await(FBy(this, other))//await(p => a1.step(p).fby(a2))  //await(p => a1.step(p).fby(a2))
    }


  def fby(other: Matcher, cached: Pattern): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m2 ++ m1, n1.fby2(n2, m1))
      case (Match(m1, n1), a2: Await)     => n1.fby(a2, m1)
      case (a1: Await, Match(m2, n2))     => Match(m2 ++ cached, a1.fby2(n2, cached))
      case (a1: Await, a2: Await)         => await(a1 + "/" + cached.facts.head + "~>" + a2, p => a1.f(p).fby(a2.f(p withHistory cached), cached)) //Await(FBy(this, other))//await(p => a1.step(p).fby(a2))  //await(p => a1.step(p).fby(a2))
    }

  def fby2(other: Matcher, cached: Pattern): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m2 ++ m1, n1.fby2(n2, m1))
      case (Match(m1, n1), a2: Await)     => n1.fby2(a2, m1)
      case (a1: Await, Match(m2, n2))     => Match(m2 ++ cached, a1.fby2(n2, cached))
      case (a1: Await, a2: Await)         => await(a1 + "//" + cached.facts.head + "~>" + a2, p => a1.fby2(a2.f(p withHistory cached), cached)) //Await(FBy(this, other))//await(p => a1.step(p).fby(a2))  //await(p => a1.step(p).fby(a2))
    }

}


trait HaltOrAwait extends Matcher

object Pattern {
  def apply(facts: List[Fact]): Pattern = Pattern(facts, Nil)
}

case class Pattern(facts: List[Fact], history: List[Fact]) {
  def ++(m: Pattern) = Pattern(facts ::: m.facts, history)

  def withHistory(history: Pattern) = Pattern(facts, history.facts)

  def factsAndHistory = facts ::: history

  def latest = facts.head.instant
}

trait MatchFunc {
  def apply(fact: Pattern): Matcher
}
case class MF(f: Pattern => Matcher, desc: String = "f(?)") extends MatchFunc {
  def apply(fact: Pattern) = f(fact)

  override def toString = desc
}
case class Halt() extends HaltOrAwait
case class Match(p: Pattern, next: Matcher) extends Matcher
case class Await(f: MatchFunc) extends HaltOrAwait {
  override def toString = f.toString
}


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
