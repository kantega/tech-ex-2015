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
    ctx({ case (LeftOnTime(_, entry), matches) if matches.exists(matched({ case LeftActivityEarly(_, e) => entry === e})) => true})


  val leftActivity =
    fact({ case LeftActivityEarly(_, entry) => true})

  val joinedActivity =
    fact({ case JoinedActivityLate(_, entry) => true})

  val joinedSameActivity =
    ctx({ case (JoinedActivityLate(_, entry), matches) if matches.exists(matched({ case LeftActivityEarly(_, e) => entry === e})) => true})

  val joinedActivityAtSameArea =
    ctx({ case (JoinedActivityLate(_, entry), matches) if matches.exists(matched({ case ArrivedAtArea(_, e) => entry.area === e})) => true})

  val leftSameActivity =
    ctx({ case (LeftActivityEarly(_, entry), matches) if matches.exists(matched({ case JoinedActivityLate(_, e) => entry === e})) => true})

  val coffee =
    visited(areas.coffeeStand)

  val toilet =
    visited(areas.toiletAtSamf) or visited(areas.toiletAtSeminar)

  val attendedWholeSession =
    joinedActivityOnTime ~> notExists(leftSameActivity) ~>< leftSameOnTime

  val enteredArea =
    fact({ case entered: ArrivedAtArea => true})

  val leftArea =
    fact({ case entered: LeftArea => true})

  val metOtherPlayer =
    fact({ case met: MetPlayer => true})


  //Badges
  val seetalksbronze        = Achievement(Bid("seetalksbronze"), "Two talks down", "Attending two talks")
  val seetalkssilver        = Achievement(Bid("seetalkssilver"), "Three is silver", "Attending three talks")
  val seetalksgold          = Achievement(Bid("seetalksgold"), "Seen all the talks", "Attending all talks")
  val firstsession          = Achievement(Bid("firstsession"), "I have seen the light", "Attending a session")
  val kreatorsession        = Achievement(Bid("kreatorsession"), "I have been at Kreator", "Attending Kreator")
  val fundingsession        = Achievement(Bid("fundingsession"), "I know you've got funds", "Attending live crowdfunding")
  val keepsringing          = Achievement(Bid("keepsringing"), "Damned thing keeps ringing", "Left session for a couple of minutes")
  val placestogo            = Achievement(Bid("placestogo"), "Got places to go, people to meet...", "Left a session early")
  val tinyjavabladder       = Achievement(Bid("tinyjavabladder"), "Tiny java bladder", "Left session for toilet multiple times")
  val smalljavabladder      = Achievement(Bid("smalljavabladder"), "Small java bladder", "Left session for toilet one time")
  val earlybird             = Achievement(Bid("earlybird"), "Early bird", "Arrived 20 mins early")
  val ontime                = Achievement(Bid("ontime"), "If you are there on time, you are late", "Do not be late")
  val intlnetworker         = Achievement(Bid("intlnetworker"), "International networker", "You have many international connections")
  val ambassador            = Achievement(Bid("ambassador"), "Ambassador", "You add a lot of connection just right after they join the game")
  val seeAllTheStandsBronze = Achievement(Bid("seestandsbronze"), "See at least a stand", "Visitng at least one stand")
  val seeAllTheStandsSilver = Achievement(Bid("seestandssilver"), "See many stands", "Visiting half the stands")
  val seeAllTheStandsGold   = Achievement(Bid("seestandsgold"), "Be at all the stands", "Visiting all the stands")
  val networkingHero        = Achievement(Bid("nethero"), "Networking hero", "Meet with half of the crowd")

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
    StatefulTracker[Set[Area], Achievement](exists(vistitedStandPred), Set()) { token => State { set =>
      val entered =
        token.fact.asInstanceOf[ArrivedAtArea]

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
    StatefulTracker[Set[Nick], Achievement](exists(metOtherPlayer), Set()) { token => State { set =>
      val metOther =
        token.fact.asInstanceOf[MetPlayer]

      val newSet =
        set + metOther.otherPlayer.player.nick

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

  val questPermutations =
    (for {
      q1 <- quests
      q2 <- quests
    } yield (q1, q2)).filterNot(t => t._1 == t._2)


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

  val trackerForQuest: Map[Qid, PatternOutput[Achievement]] =
    Map(
      visitAllStands.id -> visitAllStandsTracker,
      networking.id -> networkingTracker
    )

  val zeroTracker =
    ZeroMatcher[Achievement]()
}


case class Qid(value: String)
case class Bid(value: String)
case class Quest(id: Qid, name: String, desc: String, visibility: Visibility, badges: List[Achievement]) {
  def containsAchievement(achievemnt: Achievement) =
    badges.contains(achievemnt)
}

object Quest {
  implicit val questEqual: Equal[Quest] =
    Equal.equalA[String].contramap(_.id.value)

}

case class Achievement(id: Bid, name: String, desc: String)

object Achievement {
  implicit val badgeEq: Equal[Achievement] =
    Equal.equalA[String].contramap(_.id.value)

  def byId(id: Bid) =
    quests.badgesMap(id)
}

trait Visibility {
  def asString = {
    this match {
      case Personal => "personal"
      case Public   => "public"
      case Secret   => "secret"
    }
  }
}
case object Personal extends Visibility
case object Public extends Visibility
case object Secret extends Visibility
object Visibility {
  implicit val visibilityEqual: Equal[Visibility] =
    Equal.equalRef[Visibility]
}
case class PlayerQuestProgress(quest: Quest, achievements: List[PlayerBadgeProgress])
case class Badge(achievement: Achievement)
case class PlayerBadgeProgress(id: String, title: String, desc: String, achieved: Boolean)
case class TotalAchievementProgress(playerId: PlayerId, isAssigned: Boolean, progress: List[PlayerBadgeProgress])
case class ProgressSummary(progression: Int, max: Int, assigned: Int, unassigned: Int) {
  def increment(isAssigned: Boolean) =
    if (isAssigned) copy(assigned = assigned + 1) else copy(unassigned = unassigned + 1)
}
case class TotalQuestProgress(quest: Quest, achievemnt: List[TotalAchievementProgress]) {

  def progressSummary: List[ProgressSummary] = {

    val initMap: Map[Int, ProgressSummary] =
      (0 to quest.badges.length).toSeq.map(index => (index, ProgressSummary(index, quest.badges.length, 0, 0))).toMap

    val updatedMap =
      achievemnt.foldLeft(initMap) { (map, progress) =>
        val count = progress.progress.count(_.achieved)

        map + (count -> map(count).increment(progress.isAssigned))
      }

    updatedMap.toList.map(_._2).sortBy(_.progression)
  }

}



