package techex

import org.specs2.mutable._
import techex.data.windows
import techex.domain._
import techex.domain.preds._
import patternmatching._
import scalaz._, Scalaz._



class SimpleParseEventsSpec extends Specification {


  try {

    val innerDelim =
      head({ case di: DelimInner => true})

    val outDelim =
      head({ case od: DelimOuter => true})

    val factA: Matcher = patternmatching.on("factA", pattern => {
      val res = pattern.facts.head.isInstanceOf[FactA]
     // println("Checking factA " + pattern + ":" + res)
      res
    })
    //head({ case entered: FactA => true})

    val factB: Matcher = patternmatching.on("factB", pattern => {
      val res = pattern.facts.head.isInstanceOf[FactB]
     // println("Checking factB " + pattern + ":" + res)
      res
    })

    val factC: Matcher = patternmatching.on("factC", pattern => {
      val res = pattern.facts.head.isInstanceOf[FactC]
     // println("Checking factC " + pattern + ":" + res)
      res
    })

    val factD: Matcher = patternmatching.on("factD", pattern => {
      val res = pattern.facts.head.isInstanceOf[FactD]
      //println("Checking factD " + pattern + ":" + res)
      res
    })

    val factCAgain: Matcher = patternmatching.on("factC2", pattern => {
      val res = pattern.facts.head.isInstanceOf[FactC]
      //println("Checking factC2 " + pattern + ":" + res)
      res
    })

    val factAWithSameValue =
      history(
      {
        case (FactA(entry, _) :: history) if history.collect({ case FactA(e, _) if entry === e => true}).nonEmpty => true
      }
      )


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

      "a ~> a " in {

        val pattern =
          factA.first ~> factA.last

        val es =
          List(
            FactA(1, "a"),
            FactA(2, "B"))
        val tokens =
          foldEvents(pattern, es, print = false)

        printTokens(tokens) ! ((tokens.length must_== 1))
      }

      "short a ~> b " in {
        val evs = List(
          FactA(0, "b")
          , FactB(1)
        )
        val pattern =
          factA.first ~> factB.first

        val tokens =
          foldEvents(pattern, evs, print = true)

        printTokens(tokens) ! ((tokens.length must_== 1))
      }

      "short c ~> a ~> b ~> c" in {
        val evs =
          List(
            FactB(8),
            FactC(9),
            FactA(10, "b"),
            FactD(11,"a"),
            FactC(12),
            FactA(15, "b"),
            FactB(16),
            FactA(17, "e"),
            FactA(18, "e"),
            FactC(19),
            FactD(20,"b")
          )
        val pattern =
          ((factC.first ~> factB.first).last ~>< factD).repeat

        val tokens =
          foldEvents(pattern, evs, print = false)

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

