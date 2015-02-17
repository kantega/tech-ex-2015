package techex.domain

import java.util.UUID

import org.joda.time.Instant
import techex.domain.predicates._
import matching._
import scalaz._, Scalaz._

object quests {

  //Patterns
  val joinedActivityOnTime =
    fact { case j: JoinedOnTime => true}

  val leftSameOnTime =
    ctx({ case (FactUpdate(_, LeftOnTime(entry)), matches) if matches.exists(matched({ case LeftActivity(e) => entry === e})) => true})


  val leftActivity =
    fact({ case LeftActivity(entry) => true})

  val joinedActivity =
    fact({ case JoinedActivity(entry) => true})

  val joinedSameActivity =
    ctx({ case (FactUpdate(_, JoinedActivity(entry)), matches) if matches.exists(matched({ case LeftActivity(e) => entry === e})) => true})

  val joinedActivityAtSameArea =
    ctx({ case (FactUpdate(_, JoinedActivity(entry)), matches) if matches.exists(matched({ case Entered(e) => entry.area === e})) => true})

  val leftSameActivity =
    ctx({ case (FactUpdate(_, LeftActivity(entry)), matches) if matches.exists(matched({ case JoinedActivity(e) => entry === e})) => true})

  val coffee =
    visited(areas.coffeeStand)

  val toilet =
    visited(areas.toiletAtSamf) or visited(areas.toiletAtSeminar)

  val attendedWholeSession =
    joinedActivityOnTime ~> notExists(leftSameActivity) ~>< leftSameOnTime

  val enteredArea =
    fact({ case entered: Entered => true})

  val leftArea =
    fact({ case entered: LeftArea => true})

  val metOtherPlayer =
    fact({ case met: MetPlayer => true})


  //Badges
  val seetalksbronze        = Badge(Bid("seetalksbronze"), "Two talks down", "Attending two talks")
  val seetalkssilver        = Badge(Bid("seetalkssilver"), "Three is silver", "Attending three talks")
  val seetalksgold          = Badge(Bid("seetalksgold"), "Seen all the talks", "Attending all talks")
  val firstsession          = Badge(Bid("firstsession"), "I have seen the light", "Attending a session")
  val kreatorsession        = Badge(Bid("kreatorsession"), "I have been at Kreator", "Attending Kreator")
  val fundingsession        = Badge(Bid("fundingsession"), "I know you've got funds", "Attending live crowdfunding")
  val keepsringing          = Badge(Bid("keepsringing"), "Damned thing keeps ringing", "Left session for a couple of minutes")
  val placestogo            = Badge(Bid("placestogo"), "Got places to go, people to meet...", "Left a session early")
  val tinyjavabladder       = Badge(Bid("tinyjavabladder"), "Tiny java bladder", "Left session for toilet multiple times")
  val smalljavabladder      = Badge(Bid("smalljavabladder"), "Small java bladder", "Left session for toilet one time")
  val earlybird             = Badge(Bid("earlybird"), "Early bird", "Arrived 20 mins early")
  val ontime                = Badge(Bid("ontime"), "If you are there on time, you are late", "Do not be late")
  val intlnetworker         = Badge(Bid("intlnetworker"), "International networker", "You have many international connections")
  val ambassador            = Badge(Bid("ambassador"), "Ambassador", "You add a lot of connection just right after they join the game")
  val seeAllTheStandsBronze = Badge(Bid("seestandsbronze"), "See at least a stand", "Visitng at least one stand")
  val seeAllTheStandsSilver = Badge(Bid("seestandssilver"), "See many stands", "Visiting half the stands")
  val seeAllTheStandsGold   = Badge(Bid("seestandsgold"), "Be at all the stands", "Visiting all the stands")
  val networkingHero        = Badge(Bid("nethero"), "Networking hero", "Meet with half of the crowd")

  //Quests
  val seeAllTalks =
    Quest(
      Qid("seealltalks"),
      "See all the Talks",
      "Maximize your smart, see them all",
      Public,
      List(
        seetalksbronze,
        seetalkssilver,
        seetalksgold
      )
    )


  val attendAllSessions =
    Quest(
      Qid("visitallsessions"),
      "Attend all the Talks",
      "Maximize your smart, see them all",
      Public,
      List(
        firstsession,
        kreatorsession,
        fundingsession
      )
    )

  val attendAllSessionsProgressTracker =
    CountingTracker(0, 6, attendedWholeSession) {
      case 2 => seetalksbronze.some
      case 4 => seetalkssilver.some
      case 6 => seetalksgold.some
      case _ => none
    }

  val visitAllStands =
    Quest(
      Qid("visitthestands"),
      "Visit the stands",
      "Visit as many stands you can",
      Public,
      List(
        seeAllTheStandsBronze,
        seeAllTheStandsSilver,
        seeAllTheStandsGold
      )
    )


  val vistitedStandPred =
    visited(areas.testArea1) or visited(areas.testArea2) or visited(areas.testArea3)

  val visitAllStandsTracker =
    StatefulTracker[Set[Area], Badge](exists(vistitedStandPred), Set()) { token => State { set =>
      val entered =
        token.fact.fact.asInstanceOf[Entered]

      val newSet =
        set + entered.area

      val isIncrease =
        set.size != newSet.size

      val badge =
        if (isIncrease)
          newSet.size match {
            case 1 => seeAllTheStandsBronze.some
            case 2 => seeAllTheStandsSilver.some
            case 3 => seeAllTheStandsGold.some
            case _ => none
          }
        else none

      (newSet, badge)
    }
    }

  val eagerNess =
    Quest(
      Qid("beEager"),
      "The early bird",
      "Be on time",
      Public,
      List(
        earlybird,
        ontime)
    )


  val networking =
    Quest(
      Qid("networkingchamp"),
      "Networking champion",
      "Connect, its good for you (and your stats)",
      Public,
      List(
        networkingHero,
        intlnetworker,
        ambassador)
    )

  val networkingTracker =
    StatefulTracker[Set[Nick], Badge](exists(metOtherPlayer), Set()) { token => State { set =>
      val metOther =
        token.fact.fact.asInstanceOf[MetPlayer]

      val newSet =
        set + metOther.nick

      val isIncrease =
        set.size != newSet.size

      val badge =
        if (isIncrease)
          newSet.size match {
            case 2 => networkingHero.some
            case _ => none
          }
        else none

      (newSet, badge)
    }
    }

  val antihero =
    Quest(
      Qid("antihero"),
      "The Antihero",
      "No, you dont want to be good here",
      Secret,
      List(
        keepsringing,
        placestogo,
        tinyjavabladder,
        smalljavabladder)
    )

  val quests =
    List(
      visitAllStands,
      seeAllTalks,
      networking,
      antihero
    )

  val questMap =
    quests
      .map(q => (q.id, q))
      .toMap

  val badges =
    List(
      seetalksbronze,
      seetalksgold,
      seetalkssilver,
      firstsession,
      kreatorsession,
      fundingsession,
      keepsringing,
      placestogo,
      tinyjavabladder,
      smalljavabladder,
      earlybird,
      ontime,
      intlnetworker,
      ambassador)

  val badgesMap =
    badges
      .map(badge => (badge.id, badge))
      .toMap

  val trackerForQuest: Map[Qid, PatternOutput[Badge]] =
    Map(
      visitAllStands.id -> visitAllStandsTracker,
      networking.id -> networkingTracker
    )

  val zeroTracker =
    ZeroMatcher[Badge]()
}


case class Qid(value: String)
case class Bid(value: String)
case class Quest(id: Qid, name: String, desc: String, visibility: Visibility, badges: List[Badge])

object Quest {
  implicit val questEqual: Equal[Quest] =
    Equal.equalA[String].contramap(_.id.value)

}

case class Badge(id: Bid, name: String, desc: String)

object Badge {
  implicit val badgeEq: Equal[Badge] =
    Equal.equalA[String].contramap(_.id.value)

  def byId(id: Bid) =
    quests.badgesMap(id)
}

trait Visibility
case object Personal extends Visibility
case object Public extends Visibility
case object Secret extends Visibility
object Visibility {
  implicit val visibilityEqual: Equal[Visibility] =
    Equal.equalRef[Visibility]
}
case class QuestProgress(quest: Quest, achievements: List[Achievement])
case class Achievement(badge: Badge, achieved: Boolean, achievedBy: List[Nick])



