package techex

import java.util.concurrent.atomic.AtomicLong

import org.joda.time.Instant
import org.specs2.mutable._
import techex.data.windows
import techex.domain._
import techex.domain.preds._
import patternmatching._
import scalaz._, Scalaz._

object time {
  val counter = new AtomicLong(0)
}

abstract class NowFact extends Fact {
  val instant = Instant.now().plus(time.counter.incrementAndGet())
}
case class DelimInner(n: Int) extends NowFact
case class DelimOuter(n: Int) extends NowFact
case class FactA(n: Int, value: String) extends NowFact
case class FactB(n: Int) extends NowFact
case class FactC(n: Int) extends NowFact
case class FactD(n: Int,value:String) extends NowFact
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

    val factAWithSameValue :Matcher = patternmatching.on("factCsame", pattern => {
      val res = pattern.facts.head match {
        case a:FactD => pattern.factsAndHistory.collect{case a2:FactA => a.value === a2.value}.nonEmpty
        case _ => false
      }
      println("Checking factCA " + pattern + ":" + res)
      res
    })


    val events =
      List(
        FactA(1, "a"),
        FactB(2),
        FactA(3, "b"),
        FactA(4, "b"),
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
          factA.first ~> factA.last

        val tokens =
          foldEvents(pattern, events, print = false)

        printTokens(tokens) ! ((tokens.length must_== 10))
      }

      "short a ~> b " in {
        val evs = List(
          FactA(0, "b")
          , FactB(1)
        )
        val pattern =
          factA.first ~> factB.last

        val tokens =
          foldEvents(pattern, evs, print = false)

        printTokens(tokens) ! ((tokens.length must_== 1))
      }

      "a ~> b " in {

        val pattern =
          factA.first ~> factB.last

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

        printTokens(tokens) ! ((tokens.length must_== 9))
      }

      "(factA ~> factB) ~> (factA ~> factB) " in {

        val pattern =
          ((factA.first ~> factB.first) ~> (factA.first ~> factB.first)).repeat

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
          foldEvents(pattern, evs, print = false)

        printTokens(tokens) ! ((tokens.length must_== 2))
      }

      "short (factA ~> factB) ~> (factA ~> factB) " in {

        val pattern =
          (factA.first ~> factB.first).first ~> (factA.first ~> factB.first).first

        val evs =
          List(
            FactA(1, "a")
            , FactB(2)
            , FactA(4, "c")
            , FactA(5, "c")
            , FactB(6)
          )


        val tokens =
          foldEvents(pattern, evs, print = false)

        printTokens(tokens) ! ((tokens.length must_== 1))
      }

      "(a ~>< b).times(2) " in {

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
      "(a ~>< b).repeat " in {

        val pattern =
          (factA ~> factB).repeat

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
            // , FactA(10, "c")
          )


        val tokens =
          foldEvents(pattern, evs, print = false)

        printTokens(tokens) ! ((tokens.length must_== 3) and tokens.map(_.facts.length).forall(_ == 2))
      }

      " (factC.first ~> factA.first ~> factB.accumN(10) ~>< factC).repeat" in {

        val evs =
          List(
            FactC(1),
            FactA(0, "a"),
            FactA(1, "a"),
            FactC(2),
            FactB(2),
            FactC(3),
            FactA(3, "b"),
            FactA(4, "c"),
            FactB(5),
            FactA(6, "d"),
            FactC(4),
            FactC(5),
            FactA(9, "b"),
            FactB(10),
            FactB(11),
            FactB(12),
            FactB(13),
            FactA(14, "b"),
            FactA(11, "e"),
            //FactA(11, "e"),
            FactC(6)
          )


        val pattern =
          (factC.last ~> factA.first ~> factB.accumN(10) ~> factC).repeat


        val tokens =
          foldEvents(pattern, evs, print = false)

        printTokens(tokens) ! ((tokens.length must_== 2))
      }

      "(factA.first ~> factB.accumN(10) ~>< factC).repeat" in {

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
            FactB(8),
            FactA(8, "b"),
            FactB(9),
            FactA(10, "e"),
            //FactA(11, "e"),
            FactC(12),
            FactB(13)
          )


        val pattern =
          (factA.first ~> factB.accumN(10) ~>< factC).repeat


        val tokens =
          foldEvents(pattern, evs, print = false)

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



      "innerDelim.last ~> (factA.first ~> factB.first).times(3) ~> innerDelim" in {

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
          innerDelim.first ~> (factA.first ~> factB.first).times(3).first ~> innerDelim

        val tokens =
          foldEvents(pattern, evs)

        printTokens(tokens) ! ((tokens.length must_== 1))
      }

      " (factA.first and factB.first).times(3).first" in {

        val evs =
          List(
            // FactA(0, "a"),
            //FactA(1, "a"),
            //DelimInner(1),
            FactB(2),
            FactA(3, "b"),
            FactB(2),
            FactA(3, "b"),
            FactA(4, "c"),
            FactB(5)
            //DelimInner(16)
            //FactA(6, "d"),
          )

        val pattern =
          (factA.first and factB.first).times(3).first

        val tokens =
          foldEvents(pattern, evs)

        printTokens(tokens) ! ((tokens.length must_== 1))
      }


      "(factA.last ~> factAWithSameValue)" in {

        val evs =
          List(
            FactA(0, "a"),
            FactA(1, "b"),
            DelimInner(1),
            FactD(13, "a"),
            FactA(14, "e"),
            FactC(15)
          )

        val pattern =
          (factA.accumN(10) ~> factAWithSameValue)

        val tokens =
          foldEvents(pattern, evs,print=true)

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
      "Final tokens: \n" + tokens.map(t => t.facts.reverse.mkString(", ")).mkString("\n") + "\n"
    }
  } catch {
    case t: Throwable => t.printStackTrace()
  }
}
