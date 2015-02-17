package techex

import java.util.UUID

import org.joda.time.{Duration, Hours, Instant}
import org.specs2.mutable.Specification
import techex.domain._
import techex.domain.matching._
import techex.domain.areas._
import techex.domain.predicates._
import techex.domain.scheduling._

import scalaz.Scalaz._
import scalaz._

class ParseEventSteamSpec extends Specification {

  val enteredArea =
    fact({ case entered: Entered => true})

  val connected =
    fact({ case c: MetPlayer => true})

  val joinedActivityAtSameArea =
    ctx({ case (FactUpdate(_, JoinedActivity(entry)), matches) if matches.exists(matched({ case Entered(e) if entry.area === e => true})) => true})


  val time =
    Instant.now()

  val playerId =
    PlayerId("asbcd")

  val events =
    List(
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(foyer)),
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), MetPlayer(PlayerId("1235"),Nick("falle"))),
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(auditorium)),
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(coffeeStand)),
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), MetPlayer(PlayerId("1234"),Nick("jalle"))),
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(kantegaStand)),
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), MetPlayer(PlayerId("1235"),Nick("falle"))),
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), MetPlayer(PlayerId("1236"),Nick("palle"))),
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(technoportStand)),
      FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time.plus(Hours.hours(3).toStandardDuration)), JoinedActivity(keyNote))
    )


  "The parser" should {
    "create a single match when joining same area" in {

      val pattern =
        enteredArea ~> joinedActivityAtSameArea

      val tokens =
        foldEvents(pattern, events)

      printTokens(tokens)

      tokens.length must_== 1
    }

    "create a multiple matches when entering two areas" in {

      val pattern =
        enteredArea ~> enteredArea

      val tokens =
        foldEvents(pattern, events)

      printTokens(tokens)

      tokens.length must_== 10
    }
/*
    "create a single match when entering an area without connecting" in {

      val pattern =
        enteredArea ~> enteredArea

      val tokens =
        foldEvents(pattern, events)

      printTokens(tokens)

      tokens.length must_== 1
    }
*/
  }


  def foldEvents(pattern: EventPattern, event: List[FactUpdate]) =
    events
      .foldLeft(pattern, nil[Token]) { (pair: (EventPattern, List[Token]), update: FactUpdate) => {
      val (nextParser, newTokens) =
        pair._1.parse(Token(update, nil))

      (nextParser, newTokens ::: pair._2)
    }
    }._2

  def printTokens(tokens: List[Token]) = {
    println("Final tokens: \n" + tokens.map(t => t.matches.reverse.map(_.fact).mkString(" ~> ")).mkString("\n") + "\n")
  }
}
