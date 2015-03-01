package techex.domain

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

  def occurs(pred: Pred): Matcher =
    await(p =>
      if (pred(p)) {
        Match(p, occurs(pred))
      }
      else
        occurs(pred))

  implicit def toMatcher(pred: Pred): Matcher =
    occurs(pred)

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


  def visited(area: Area) =
    pattern({ case ArrivedAtArea(_, a, _) :: tail if a === area => true})


  def pattern(f: PartialFunction[(List[Fact]), Boolean]): Pred =
    ctx => {
      if (f.isDefinedAt(ctx.facts))
        f(ctx.facts)
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
          case Match(m2, n2) => Match(m2 ++ matches, next.step(fact))
          case a@_           => Match(matches, a)
        }
        case Await(f)             => f(fact)
      }
    }
    go(this)
  }

  def ++(a: Matcher) =
    and(a)


  def ~>(m: Matcher) =
    fby(m)

  def ~><(m: Matcher) =
    fby(m).haltAfter

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
      case Match(m, n) => Match(m, await(p => n.step(p).lastC(m)))
      case a: Await    => await(p => a.step(p).last)
    }


  private def lastC(cache: Pattern): Matcher = {
    this match {
      case Halt()      =>  Match(cache, await(p => forever(cache)))
      case Match(m, n) => Match(m, await(p => n.step(p).lastC(m)))
      case a: Await    => Match(cache, await(p => a.step(p).lastC(cache)))
    }
  }

  def onAwait(m: Matcher): Matcher = {
    this match {
      case Await(f) => m
      case _        => this
    }
  }

  def first: Matcher =
    this match {
      case Halt()      => Halt()
      case Match(m, n) => Match(m, await(p => n.forever(m)))
      case Await(f)    => await(p => f(p).first)
    }

  private def forever(matched: Pattern): Matcher =
     Match(matched, await(p => forever(matched)))



  def accumN(size: Int): Matcher = {
    this match {
      case Halt()      => Halt()
      case Match(p, n) => {
        val acs = p.facts ::: windows.sized(size)
        Match(Pattern(acs.contents.toList), await(p => n.step(p).accumNonEmpty(acs)))
      }
      case Await(f)    => await(p => f(p).accumN(size))
    }
  }

  def accumNonEmpty(w: Window[Fact]): Matcher = {
    this match {
      case Halt()      => Halt()
      case Match(p, n) => {
        val acs = p.facts ::: w
        Match(Pattern(acs.contents.toList), await(p => n.step(p).accumNonEmpty(acs)))
      }
      case Await(f)    => Match(Pattern(w.contents.toList), await(p => f(p).accumNonEmpty(w)))
    }
  }

  def repeat: Matcher =
    repeatForever(this)

  private def repeatForever(original: Matcher): Matcher = {
    this match {
      case Halt()      => await(p => original.repeat.step(p))
      case Match(m, n) => Match(m, n.repeatForever(original))
      case Await(f)    => await(p => f(p).repeatForever(original))
    }
  }


  def times(times: Int): Matcher =
    repeat(this, times, none)


  private def repeat(original: Matcher, times: Int, matched: Option[Pattern]): Matcher = {
    if (times == 0)
      matched.fold[Matcher](Halt())(m => Match(m, Halt()))
    else
      this match {
        case Halt()      => await(p => original.repeat(original, times-1,  matched))
        case Match(m, n) => await(p => original.repeat(original, times-1, matched.fold(Some(m))(p => Some(m ++ p))))
        case Await(f)    => await(p => f(p).repeat(original, times, matched))
      }
  }

  def xor(other: Matcher): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => await(p => n1.step(p) xor n2.step(p))
      case (Match(m1, n1), b: Await)      => Match(m1, await(p => (n1.step(p) xor b.step(p)).step(p)))
      case (a: Await, Match(m2, n2))      => Match(m2, await(p => (a.step(p) xor n2.step(p)).step(p)))
      case (a: Await, b: Await)           => await(p => a.step(p) xor b.step(p))
    }


  def or(other: Matcher): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m1 ++ m2, await(p => n1.step(p) or n2.step(p)))
      case (Match(m1, n1), b: Await)      => Match(m1, await(p => (n1.step(p) or b.step(p)).step(p)))
      case (a: Await, Match(m2, n2))      => Match(m2, await(p => (a.step(p) or n2.step(p)).step(p)))
      case (a: Await, b: Await)           => await(p => a.step(p) or b.step(p))
    }

  def and(other: Matcher): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m1 ++ m2, await(p => n1.step(p) or n2.step(p)))
      case (Match(m1, n1), b: Await)      => await(p => n1.step(p) or b.step(p))
      case (a: Await, Match(m2, n2))      => await(p => a.step(p) or n2.step(p))
      case (a: Await, b: Await)           => await(p => a.step(p) or b.step(p))
    }

  def haltAfter: Matcher =
    this match {
      case Halt()      => Halt()
      case Match(p, n) => Match(p, Halt())
      case Await(f)    => await(p => f(p).haltAfter)
    }

  def haltOn: Matcher =
    this match {
      case Halt()      => Halt()
      case Match(p, n) => Halt()
      case Await(f)    => await(p => f(p).haltOn)
    }

  def fby(other: Matcher): Matcher =
    this match {
      case Halt()      => Halt()
      case Match(m, n) => await(p => n.step(p).fby(other.step(p), m)) //Await(MatchedFBy(n, other, m))
      case Await(f)    => await(p => f(p).fby(other))
    }


  def fby(other: Matcher, consumed: Pattern): Matcher =
    (this, other) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m2 ++ consumed, await(p => n1.step(p).fby(n2.step(p), m1))) //Await(MatchedFBy(n1, n2, m1)))
      case (Match(m1, n1), a2: Await)     => await(p => n1.step(p).fby(a2.step(p), m1)) //Await(MatchedFBy(n1, a2, m1))
      case (a1: Await, Match(m2, n2))     => Match(m2 ++ consumed, await(p => a1.step(p).fby(n2)))
      case (a1: Await, a2: Await)         => await(p => a1.step(p).fby(a2))
    }
}


trait HaltOrAwait extends Matcher

case class Pattern(facts: List[Fact]) {
  def ++(m: Pattern) = Pattern(facts ::: m.facts)
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
case class Await(f: MatchFunc) extends HaltOrAwait



/*
case class FBy(one: Matcher, other: Matcher) extends MatchFunc {
  def apply(p: Pattern) = {
    one(p) match {
      case Halt()      => Halt()
      case Match(m, n) => Await(MatchedFBy(n, other, p))
      case a: Await    => Await(FBy(a, other))
    }
  }
}

case class MatchedFBy(one: Matcher, other: Matcher, consumed: Pattern) extends MatchFunc {
  def apply(p: Pattern) = {

    (other(p), one(p)) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m2, n2), Match(m1, n1)) => Match(consumed ++ m2, Await(MatchedFBy(n1, n2, m1)))
      case (a2: Await, Match(m1, n1))     => Await(MatchedFBy(n1, a2, m1))
      case (Match(m2, n2), a1: Await)     => Match(consumed ++ m2, Await(FBy(a1, n2)))
      case (a2: Await, a1: Await)         => Await(FBy(a1, a2))
    }
  }
}
 */