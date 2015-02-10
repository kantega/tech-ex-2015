package techex

import java.util.UUID

import org.joda.time.{Hours, DateTime, Instant}
import org.specs2.mutable.Specification
import techex.TestServer._
import techex.domain._
import areas._
import schedule._
import techex.domain.predicates._
import tracking._
import matching._
import predicates._
import scalaz._, Scalaz._

class ParseEventSteamSpec extends Specification {
  "The parser" should {
    "create a single matches" in {
      val enteredArea =
        fact({ case entered: Entered => true})

      val joinedActivityAtSameArea =
        ctx({ case (FactUpdate(_, JoinedActivity(entry)), matches) if matches.exists(matched({ case Entered(e) if entry.space.area === e => true})) => true})

      val pattern =
        enteredArea ~> joinedActivityAtSameArea

      val time =
        Instant.now()

      val playerId =
        PlayerId("asbcd")

      val events =
        List(
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(foyer)),
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Connected(PlayerId("1235"))),
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(auditorium)),
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(coffeeStand)),
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Connected(PlayerId("1234"))),
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(kantegaStand)),
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Connected(PlayerId("1235"))),
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Connected(PlayerId("1236"))),
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time), Entered(technoportStand)),
          FactUpdate(UpdateMeta(UUID.randomUUID(), playerId, time.plus(Hours.hours(3).toStandardDuration)), JoinedActivity(keyNote))
        )



      val (parser, tokens) =
        events
          .foldLeft(pattern, nil[Token]) { (pair: (EventPattern, List[Token]), update: FactUpdate) => {
          val (nextParser, newTokens) =
            pair._1.parse(Token(update, nil))


          (nextParser, newTokens ::: pair._2)

        }
        }

      println("Final tokens: \n"+tokens.map(t =>   t.matches.reverse.map(_.fact).mkString(" ~> ") ).mkString("\n"))
      println("\n")
      tokens.length must_== 1
    }

  }
}
