package techex.domain.pmatching

import techex.data.Window
import techex.domain.Fact
import patternmatching._

object patternmatching {

  type Pred = Pattern => Boolean

  def await(f: Pattern => Matcher) = {
    Await(MF(f))
  }

  def await(desc: String)(f: Pattern => Matcher) = {
    Await(MF(f, desc))
  }

}


trait Matcher {

  def apply(fact: Pattern): Matcher = {
    def go(p: Matcher): Matcher = {
      p match {
        case Halt()               => Halt()
        case Match(matches, next) => next(fact)
        case Await(f)             => f(fact)
      }
    }
    go(this)
  }

  def ++(a: Matcher) =
    and(a)


  def ~>(m: Matcher) =
    FBy(this, m)


  def ||(m: Matcher) =
    or(m)


  def or(m: Matcher) =
    Await(Or(this, m))

  def and(m: Matcher) =
    Await(And(this, m))

  def last =
    Await(HaltAfter(this))

  def repeatForever =
    Await(RepeatForever(this))

  def repeat(times: Int) =
    Await(RepeatCount(this, times))
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
case class Match(p: Pattern, next: HaltOrAwait) extends Matcher
case class Await(f: MatchFunc) extends HaltOrAwait

case class Occurs(pred: Pred) extends MatchFunc {
  def apply(p: Pattern) = {
    if (pred(p)) {
      Match(p, Await(this))
    }
    else
      Await(this)
  }
}

case class Accum(m: Matcher, matches: Window[Fact] = Nil) extends MatchFunc {
  def apply(p: Pattern) = {
    m(p) match {
      case Halt()                      => Halt()
      case Match(p, n)                 => {
        val acs = matches ++ p.facts
        Match(Pattern(acs.contents.toList), Await(Accum(m, acs)))
      }
      case a: Await if matches.isEmpty => Await(Accum(a, matches))
      case a: Await                    => Match(Pattern(matches.contents.toList), Await(Accum(m, matches)))
    }
  }
}

case class FirstOf(m: Matcher, cache: Option[Pattern] = None) extends MatchFunc {
  def apply(p: Pattern) = {
    m(p) match {
      case Halt()                    => Halt()
      case Match(p, n)               => Match(p, Await(FirstOf(Await(Never()), Some(p))))
      case a: Await if cache.isEmpty => Await(FirstOf(m, cache))
      case a: Await                  => Match(cache.get, Await(FirstOf(Await(Never()), cache)))
    }
  }
}

case class LastOf(m: Matcher, cache: Option[Pattern] = None) extends MatchFunc {
  def apply(p: Pattern) = {
    (m(p), cache) match {
      case (Halt(), _)          => Halt()
      case (Match(p, n), _)     => Match(p, Await(LastOf(m, Some(p))))
      case (a: Await, None)     => Await(LastOf(m, None))
      case (a: Await, Some(mp)) => Match(mp, Await(FirstOf(m, Some(mp))))
    }
  }
}

case class Never() extends MatchFunc {
  def apply(p: Pattern) = Await(Never())
}

case class And(one: Matcher, other: Matcher) extends MatchFunc {
  def apply(p: Pattern) = {

    val oneNext = one(p)
    val otherNext = other(p)
    (oneNext, otherNext) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m1 ++ m2, Await(And(n1, n2)))
      case (a, b)                         => Await(And(a, b))
    }
  }
}

case class Or(one: Matcher, other: Matcher) extends MatchFunc {
  def apply(p: Pattern) = {
    val oneNext = one(p)
    val otherNext = other(p)
    (oneNext, otherNext) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Match(m1 ++ m2, Await(Or(n1, n2)))
      case (Match(m1, n1), b: Await)      => Match(m1, Await(Or(n1, b)))
      case (a: Await, Match(m2, n2))      => Match(m2, Await(Or(a, n2)))
      case (a: Await, b: Await)           => Await(Or(a, b))
    }
  }
}

case class Xor(one: Matcher, other: Matcher) extends MatchFunc {
  def apply(p: Pattern) = {
    val oneNext = one(p)
    val otherNext = other(p)
    (oneNext, otherNext) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m1, n1), Match(m2, n2)) => Await(Or(n1, n2))
      case (Match(m1, n1), b: Await)      => Match(m1, Await(Or(n1, b)))
      case (a: Await, Match(m2, n2))      => Match(m2, Await(Or(a, n2)))
      case (a: Await, b: Await)           => Await(Or(a, b))
    }
  }
}

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

case class RepeatForever(original: Matcher, running: Option[Matcher] = None) extends MatchFunc {
  def apply(p: Pattern) = {
    running.getOrElse(original)(p) match {
      case Halt()      => Await(RepeatForever(original))
      case Match(m, n) => n match {
        case Halt()   => Match(m, Await(RepeatForever(original)))
        case a: Await => Match(m, Await(RepeatForever(original, Some(a))))
      }
      case a: Await    => Await(RepeatForever(original, Some(a)))
    }
  }
}

case class RepeatCount(original: Matcher, max: Int, runNr: Int = 1, running: Option[Matcher] = None) extends MatchFunc {
  def apply(p: Pattern) = {
    running.getOrElse(original)(p) match {
      case Halt()      => repeat
      case Match(m, n) => n match {
        case Halt()   => Match(m, repeat)
        case a: Await => Match(m, next(a))
      }
      case a: Await    => next(a)
    }
  }

  def next(m: Matcher) =
    Await(RepeatCount(original, max, runNr, Some(m)))

  def repeat: HaltOrAwait =
    if (max == runNr) Halt() else Await(RepeatCount(original, max, runNr + 1))

}

case class Until(one: Matcher, other: Matcher) extends MatchFunc {
  def apply(p: Pattern) = {
    one(p) match {
      case Halt()      => Halt()
      case Match(m, n) => Await(MatchedUntil(n, other, p))
      case a: Await    => Await(Until(a, other))
    }
  }
}

case class MatchedUntil(one: Matcher, other: Matcher, consumed: Pattern) extends MatchFunc {
  def apply(p: Pattern) = {

    (other(p), one(p)) match {
      case (Halt(), _) | (_, Halt())      => Halt()
      case (Match(m2, n2), Match(m1, n1)) => Match(consumed ++ m2, Halt())
      case (a2: Await, Match(m1, n1))     => Await(MatchedFBy(n1, a2, m1))
      case (Match(m2, n2), a1: Await)     => Match(consumed ++ m2, Halt())
      case (a2: Await, a1: Await)         => Await(FBy(a1, a2))
    }
  }
}

case class HaltOn(m: Matcher) extends MatchFunc {
  def apply(p: Pattern) = {
    m(p) match {
      case Halt()      => Halt()
      case Match(p, n) => Halt()
      case a: Await    => Await(HaltOn(m))
    }
  }
}

case class HaltAfter(m: Matcher) extends MatchFunc {
  def apply(p: Pattern) = {
    m(p) match {
      case Halt()      => Halt()
      case Match(p, n) => Match(p, Halt())
      case a: Await    => Await(HaltAfter(m))
    }
  }
}