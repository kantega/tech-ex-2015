package techex

import scalaz._, Scalaz._
import org.joda.time.Instant
import org.specs2.mutable.Specification
import techex.domain.{Matcher, Fact}
import org.specs2.mutable._
import techex.domain.patternmatching._

class MatchingSpec extends Specification {


  case class FactA(count: Int, instant: Instant) extends Fact
  case class FactB(count: Int, instant: Instant) extends Fact

  val now = Instant.now()

  "When checking matcher" should {

    "Emit bothe when and'ing lasts" in {

      val m =
        on[FactA].last and on[FactB].last

      val evs =
        List(FactA(1, now), FactA(2, now), FactB(3, now), FactA(4, now))

      val tokens =
        foldEvents(m, evs,print=true)

      printTokens(tokens) ! (tokens.length must_== 2)
    }
  }

  def foldEvents[A](pattern: Matcher[A], event: List[Fact], print: Boolean = false) =
    event
      .foldLeft(pattern, nil[A]) { (pair: (Matcher[A], List[A]), update: Fact) => {
      val (nextParser, newTokens) =
        pair._1.check(update)
      if (print) {

        println("PRE " + pair._1)
        println(">>> " + update.getClass.getName)
        println("POS " + nextParser)
        println("<<< " + newTokens)
        println("")
      }
      (nextParser, newTokens.toList ::: pair._2)
    }
    }._2

  def printTokens(tokens: List[_]): String = {
    "Final tokens: \n" + tokens.mkString("\n") + "\n"
  }

}
