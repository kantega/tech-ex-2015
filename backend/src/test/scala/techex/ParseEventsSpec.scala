package techex

import org.specs2.mutable.Specification
import techex.domain._
import techex.domain.preds._

import scalaz._,Scalaz._

case class FactA(value: String, n: Int) extends Fact
case class FactB(n: Int) extends Fact
case class FactC(n: Int) extends Fact
class ParseEventsSpec extends Specification {



  try {

    val factA =
      fact({ case entered: FactA => true})

    val factB =
      fact({ case c: FactB => true})

    val factC =
      fact({ case c: FactC => true})

    val factAWithSameValue =
      ctx({ case (FactA(entry, _) :: matches) if matches.exists(matched({ case FactA(e, _) if entry === e => true})) => true})


    val events =
      List(
        FactA("a", 1),
        FactB(2),
        FactA("b", 3),
        FactA("c", 4),
        FactB(5),
        FactA("d", 6),
        FactC(7),
        FactA("b",8),
        FactB(9),
        FactA("e",10),
        FactC(11)
      )


    "The parser" should {

      "create several matches match when accumulating three facts" in {

        val pattern =
          factA ~> factB ~> factC

        val tokens =
          foldEvents(pattern, events)

        printTokens(tokens) ! ((tokens.length must_== 10) and (tokens(0).pattern.length must_== 3))
      }

      "create many matches when accumulating two and one facts" in {

        val pattern =
          factA ~> factA ~> factB

        val tokens =
          foldEvents(pattern, events)

        printTokens(tokens) ! ((tokens.length must_== 12) and (tokens(0).pattern.length must_== 3))
      }

      "create a one match when accumulating three facts repeatedly" in {

        val pattern =
          (factA ~> factA ~> factB).once

        val tokens =
          foldEvents(pattern, events, print = false)

        printTokens(tokens) ! ((tokens.length must_== 1) and (tokens(0).pattern.length must_== 3))
      }

      "create a several matches match when accumulating three facts where the last is repeated" in {

        val pattern =
          factA ~> factA ~> exists(factB)

        val tokens =
          foldEvents(pattern, events, print = false)

        printTokens(tokens) ! ((tokens.length must_== 5) and (tokens(0).pattern.length must_== 3))
      }


      /*

      "create a one matches match when accumulating three facts until the last one" in {

        val pattern =
          factA ~> factA ~>< factB

        val tokens =
          foldEvents(pattern, events,print=true)

        printTokens(tokens) ! ((tokens.length must_== 11) and (tokens(0).matches.length must_== 3))
      }
*/
      "create a single match when matching one fact" in {

        val pattern =
          exists(factB).once

        val tokens =
          foldEvents(pattern, events)

        printTokens(tokens) ! (tokens.length must_== 1)
      }

      "create a multiple match when matching one fact repeatedly" in {

        val pattern =
          exists(factB)

        val tokens =
          foldEvents(pattern, events)

        printTokens(tokens) ! (tokens.length must_== 5)
      }

      "create a single match matching facts with same value" in {

        val pattern =
          exists(factA) ~> factAWithSameValue

        val tokens =
          foldEvents(pattern, events, print = false)

        (tokens.length must_== 1)
      }

      "create a many matches when finding a and then b" in {

        val pattern =
          (factA ~> factB)

        val tokens =
          foldEvents(pattern, events)

        printTokens(tokens) ! (tokens.length must_== 9)
      }

      "create a many matches when finding a and then b in a hort list" in {

        val pattern =
          (factA ~> factB)

        val tokens =
          foldEvents(pattern, events.take(5), print = true)

        printTokens(tokens) ! (tokens.length must_== 5)
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


    def foldEvents(pattern: PatternMatcher, event: List[Fact], print: Boolean = false) =
      events
        .foldLeft(pattern, nil[Match]) { (pair: (PatternMatcher, List[Match]), update: Fact) => {
        val (nextParser, newTokens) =
          pair._1.parse(update)
        if (print) {

          println("PRE " + pair._1)
          println(">>> " + update.getClass.getName)
          println("POS " + nextParser)
          println("<<< " + newTokens)
          println("")
        }
        (nextParser, pair._2 ::: newTokens)
      }
      }._2

    def printTokens(tokens: List[Match]): String = {
      "Final tokens: \n" + tokens.map(t => t.pattern.reverse.mkString(" ~> ")).mkString("\n") + "\n"
    }
  } catch {
    case t: Throwable => t.printStackTrace()
  }
}
