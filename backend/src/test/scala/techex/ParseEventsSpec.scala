package techex

import org.specs2.mutable._
import techex.data.windows
import techex.domain._
import techex.domain.preds._
import patternmatching._
import scalaz._, Scalaz._


case class DelimInner(n: Int) extends Fact
case class DelimOuter(n: Int) extends Fact
case class FactA(n: Int, value: String) extends Fact
case class FactB(n: Int) extends Fact
case class FactC(n: Int) extends Fact
class ParseEventsSpec extends Specification {


  try {

    val innerDelim =
      head({ case di: DelimInner => true})

    val outDelim =
      head({ case od: DelimOuter => true})

    val factA =
      head({ case entered: FactA => true})

    val factB =
      head({ case c: FactB => true})

    val factC =
      head({ case c: FactC => true})

    val factAWithSameValue =
      pattern({ case (FactA(entry, _) :: matches) if matches.exists({ case FactA(e, _) if entry === e => true}) => true})


    val events =
      List(
        FactA(1, "a"),
        FactB(2),
        FactA(3, "b"),
        FactA(4, "c"),
        FactB(5),
        FactA(6, "d"),
        FactC(7),
        FactA(8, "b"),
        FactB(9),
        FactA(10, "e"),
        FactA(11, "e"),
        FactC(12)
      )


    "The parser" should {

      "first" in {

        val pattern =
          factA.first

        val tokens =
          foldEvents(pattern, events)

        printTokens(tokens) !
          ((tokens.length must_== 12) and
            tokens.map(_.facts.length).forall(_ == 1)
            )
      }

      "last" in {

        val pattern =
          factB.last

        val tokens =
          foldEvents(pattern, events)

        printTokens(tokens) ! ((tokens.length must_== 11))
      }

      "a ~> a " in {

        val pattern =
          factA ~> factA

        val tokens =
          foldEvents(pattern, events, print = false)

        printTokens(tokens) ! ((tokens.length must_== 2))
      }

      "a ~> b " in {

        val pattern =
          factA ~> factB

        val evs =
          List(
            FactA(-0, "a")
            , FactA(0, "b")
            , FactA(1, "a")
            , FactB(2)
            , FactA(3, "b")
            , FactA(4, "c")
            , FactB(5)
            , FactA(6, "d")
            , FactA(7, "b")
            , FactA(8, "c")
            , FactB(9)
            , FactA(10, "c")
          )


        val tokens =
          foldEvents(pattern, evs, print = false)

        printTokens(tokens) ! ((tokens.length must_== 3))
      }

      "(factA ~> factB) ~> (factA ~> factB) " in {

        val pattern =
          (factA.first ~> factB.first) ~> (factA.first ~> factB.first)

        val evs =
          List(
            FactA(-0, "a")
            , FactA(0, "b")
            , FactA(1, "a")
            , FactB(2)
            , FactA(3, "b")
            , FactA(4, "c")
            , FactB(5)
            , FactA(6, "d")
            , FactA(7, "b")
            , FactA(8, "c")
            , FactB(9)
            , FactA(10, "c")
            , FactA(11, "a")
            , FactB(12)
            , FactA(13, "b")
            , FactA(14, "c")
            , FactB(15)
          )


        val tokens =
          foldEvents(pattern, evs, print = true)

        printTokens(tokens) ! ((tokens.length must_== 11))
      }

      "(a ~>< b).repeat2 " in {

        val pattern =
          (factA ~> factB).times(2)

        val evs =
          List(
            FactA(-0, "a")
            , FactA(0, "b")
            , FactA(1, "a")
            , FactB(2)
            , FactA(3, "b")
            , FactA(4, "c")
            , FactB(5)
            , FactA(6, "d")
            , FactA(7, "b")
            , FactA(8, "c")
            , FactB(9)
            , FactA(10, "c")
          )


        val tokens =
          foldEvents(pattern, evs, print = false)

        printTokens(tokens) ! ((tokens.length must_== 1) and tokens.map(_.facts.length).forall(_ == 4))
      }


      "factA.first ~> factB.accumN(10) ~>< factC" in {

        val evs =
          List(
            FactA(0, "a"),
            FactA(1, "a"),
            FactB(2),
            FactA(3, "b"),
            FactA(4, "c"),
            FactB(5),
            FactA(6, "d"),
            FactC(7),
            FactA(8, "b"),
            FactB(9),
            FactA(10, "e"),
            //FactA(11, "e"),
            FactC(12)
          )


        val pattern =
          factA.first ~> factB.accumN(10) ~>< factC


        val tokens =
          foldEvents(pattern, evs, print = false)

        printTokens(tokens) ! ((tokens.length must_== 1))
      }

      "(factA.first ~> factB.accum(windows.sized[Fact](10)) ~>< factC).repeat" in {

        val evs =
          List(
            FactA(0, "a"),
            FactA(1, "a"),
            FactB(2),
            FactA(3, "b"),
            FactA(4, "c"),
            FactB(5),
            FactA(6, "d"),
            FactC(7),
            FactA(8, "b"),
            FactB(9),
            FactA(10, "e"),
            //FactA(11, "e"),
            FactC(12)
          )


        val pattern =
          (factA.first ~> factB.accumN(10) ~>< factC).repeat


        val tokens =
          foldEvents(pattern, evs)

        printTokens(tokens) ! ((tokens.length must_== 2))
      }
      "(factA.first ~> factB.last ~>< factC).repeat" in {

        val evs =
          List(
            FactA(0, "a"),
            FactA(1, "a"),
            FactB(2),
            FactA(3, "b"),
            FactA(4, "c"),
            FactB(5),
            FactA(6, "d"),
            FactC(7),
            FactA(8, "b"),
            FactB(9),
            FactA(10, "e"),
            //FactA(11, "e"),
            FactC(12)
          )


        val pattern =
          (factA.first ~> factB.last ~>< factC).repeat


        val tokens =
          foldEvents(pattern, evs)

        printTokens(tokens) ! ((tokens.length must_== 2) and tokens.map(_.facts.length).forall(_ == 3))
      }



      "innerDelim.last ~> (factB.first ~> factB.first).repeatN(2) ~> innerDelim" in {

        val evs =
          List(
            FactA(0, "a"),
            FactA(1, "a"),
            DelimInner(1),
            FactB(2),
            FactA(3, "b"),
            FactA(4, "c"),
            FactB(5),
            FactA(6, "d"),
            FactC(7),
            FactA(8, "b"),
            FactB(9),
            FactA(10, "b"),
            FactB(11),
            DelimInner(12),
            FactA(13, "e"),
            //FactA(14, "e"),
            FactC(15)
          )

        val pattern =
          innerDelim.first ~> (factA.first ~> factB.first).times(2).first ~> innerDelim

        val tokens =
          foldEvents(pattern, evs)

        printTokens(tokens) ! ((tokens.length must_== 1))
      }


    }


    def foldEvents(pattern: Matcher, event: List[Fact], print: Boolean = false) =
      event
        .foldLeft(pattern, nil[Pattern]) { (pair: (Matcher, List[Pattern]), update: Fact) => {
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

    def printTokens(tokens: List[Pattern]): String = {
      "Final tokens: \n" + tokens.map(t => t.facts.reverse.mkString(" ~> ")).mkString("\n") + "\n"
    }
  } catch {
    case t: Throwable => t.printStackTrace()
  }
}
