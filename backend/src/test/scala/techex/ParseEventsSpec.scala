package techex

import org.joda.time.Instant
import org.specs2.mutable._
import org.specs2.mutable.Specification
import techex.domain._
import techex.domain.predicates._
import _root_.scalaz.Scalaz
import Scalaz._

class ParseEventsSpec extends Specification {

  case class FactA(value: String) extends Fact
  case class FactB() extends Fact

  try {

    val factA =
      fact({ case entered: FactA => true})

    val factB =
      fact({ case c: FactB => true})

    val factAWithSameValue =
      ctx({ case (FactA(entry), matches) if matches.exists(matched({ case FactA(e) if entry === e => true})) => true})


    val events =
      List(
        FactA("a"),
        FactA("b"),
        FactB(),
        FactA("c"),
        FactB(),
        FactB(),
        FactA("b"),
        FactB(),
        FactB()
      )


    "The parser" should {

      "yield true" in {
        1 must_== 1
      }

      "create a single match matching facts with same value" in {

        val pattern =
          factA ~> factAWithSameValue

        val tokens =
          foldEvents(pattern, events)

        tokens.length must_== 1
      }


            "create a single match matching facts with same value" in {

              val pattern =
                factA ~> factAWithSameValue

              val tokens =
                foldEvents(pattern, events)

              tokens.length must_== 1
            }

            "create a multiple matches when finding a and then b" in {

              val pattern =
                factA ~> factB

              val tokens =
                foldEvents(pattern, events)

              tokens.length must_== 16
            }
/*
            "create a multiple matches when finding a until b" in {

              val pattern =
                factA ~>< factB

              val tokens =
                foldEvents(pattern, events)



              printTokens(tokens) ! (tokens.length must_== 1)
            }

            "create create matches when entering an area without connecting" in {

              val pattern =
                factA ~> factA

              val tokens =
                foldEvents(pattern, events)



              printTokens(tokens) ! (tokens.length must_== 6)
            }
      */
    }


    def foldEvents(pattern: EventPattern, event: List[Fact]) =
      events
        .foldLeft(pattern, nil[Token]) { (pair: (EventPattern, List[Token]), update: Fact) => {
        val (nextParser, newTokens) =
          pair._1.parse(Token(update, nil))

        println(">>> " + update.getClass.getName)
        println("=== " + nextParser)
        println("<<< " + newTokens)
        println("")

        (nextParser, newTokens ::: pair._2)
      }
      }._2

    def printTokens(tokens: List[Token]): String = {
      "Final tokens: \n" + tokens.map(t => t.matches.reverse.mkString(" ~> ")).mkString("\n") + "\n"
    }
  } catch {
    case t: Throwable => t.printStackTrace()
  }
}
