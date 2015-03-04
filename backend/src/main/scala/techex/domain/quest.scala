package techex.domain

import org.joda.time.Minutes._
import org.joda.time.{Hours, DateTime, ReadableDuration}
import techex._
import techex.domain.patternmatching._
import techex.domain.preds._
import areas._
import scalaz.Scalaz._
import scalaz._


object quests {

  def first(p: Pred) =
    p.first

  def last(p: Pred) =
    p.last

  def enter(area: Area) =
    head({ case EnteredArea(_, a, _) if a === area => true})

  def enterOneOf(areas: Area*) =
    head({ case EnteredArea(_, area, _) if areas.exists(a => a === area) => true})

  def exitOneOf(areas: Area*) =
    head({ case LeftArea(_, area, _) if areas.exists(a => a === area) => true})

  //def stayAt(area:Area,duration:ReadableDuration) =


  def time(t: DateTime => Boolean): Pred =
    pattern => t(pattern.facts.head.instant.toDateTime)

  def size(t: Int): Pred =
    pattern => (pattern.facts.length >= t)

  def visitCoffee =
    enter(kantegaCoffeeUp) or enter(kantegaCoffeeDn)

  //Patterns
  val joinedAnActivityOnTime =
    head { case j: JoinedOnStart => true}

  val enteredArea =
    head({ case entered: EnteredArea => true})

  val leftArea =
    head({ case entered: LeftArea => true})

  def leftSameAreaWithin(duration: ReadableDuration) =
    pattern {
      case (LeftArea(_, exitArea, leftTime), history)
        if history.collect {
          case EnteredArea(_, enterArea, enterTime) if
          enterArea === exitArea && durationBetween(enterTime, leftTime).isShorterThan(duration) => true
        }.nonEmpty => true
    }

  val metOtherPlayer =
    head({ case met: MetPlayer => true})

  val startOfDay =
    head { case mn: StartOfDay => true}

  val endOfDay =
    head { case mn: EndOfDay => true}

  //Badges
  val seetalksiron     = Achievement(Bid("seetalksiron"), "First talk attended", "Attending one talks")
  val seetalksbronze   = Achievement(Bid("seetalksbronze"), "Two talks down", "Attending two talks")
  val seetalkssilver   = Achievement(Bid("seetalkssilver"), "Three is silver", "Attending three talks")
  val seetalksgold     = Achievement(Bid("seetalksgold"), "Seen all the talks", "Attending all talks")
  val firstsession     = Achievement(Bid("firstsession"), "I have seen the light", "Attending a session")
  val kreatorsession   = Achievement(Bid("kreatorsession"), "I have been at Kreator", "Attending Kreator")
  val fundingsession   = Achievement(Bid("fundingsession"), "I know you've got funds", "Attending live crowdfunding")
  val keepsringing     = Achievement(Bid("keepsringing"), "Damned thing keeps ringing", "Left session for a couple of minutes")
  val placestogo       = Achievement(Bid("placestogo"), "Got places to go, people to meet...", "Left a session early")
  val tinyjavabladder  = Achievement(Bid("tinyjavabladder"), "Tiny java bladder", "Left session for toilet multiple times")
  val smalljavabladder = Achievement(Bid("smalljavabladder"), "Small java bladder", "Left session for toilet one time")
  val earlybird        = Achievement(Bid("earlybird"), "Early bird", "Arrived 20 mins early")
  val ontime           = Achievement(Bid("ontime"), "If you are there on time, you are late", "Do not be late")
  val intlnetworker    = Achievement(Bid("intlnetworker"), "International networker", "You have many international connections")
  val ambassador       = Achievement(Bid("ambassador"), "Ambassador", "You add a lot of connection just right after they join the game")

  val networkingHero = Achievement(Bid("nethero"), "Networking hero", "Meet with half of the crowd")


  //Kantega badges and quests
  object kq {



    val earlyatwork            = Achievement(Bid("kearlyatwork"), "Came in early today", "Have coffee before 08:00")
    val earlyprettyearlyatwork = Achievement(Bid("kprettyearlyatwork"), "Came on time today", "Have coffee before 09:00")
    val earlyenough            = Achievement(Bid("kearlyenough"), "Came on time this week", "Have coffee before 09:00 for a week")
    val earlyatworks           = Achievement(Bid("kearlyatwork"), "Came in early today", "Have coffee before 08:00 for a week")

    val lateprettylateatwork  = Achievement(Bid("kprettylateatwork"), "Had some stuff to do.", "Have coffee after 16:00")
    val lateprettylateatworks = Achievement(Bid("kprettylateatworks"), "Had a lot of stuff to do.", "Have coffee after 16:00 for a week")
    val lateatwork            = Achievement(Bid("klateatwork"), "Had some more stuff to do.", "Have coffee after 17:00")
    val lateatworks           = Achievement(Bid("klateatworks"), "I like staying at work.", "Have coffee after 17:00 for a week")

    val coffeeliker       = Achievement(Bid("kkoffehero"), "I like coffee", "Get coffee five times in a day")
    val coffeexperimenter = Achievement(Bid("kkoffeexperimenter"), "I like to experiment", "Have coffe from both areas in a day")
    val coffeeconnector   = Achievement(Bid("kcoffeeconnector"), "I like my coworkers", "Meet with three others at the coffeemachine in one day")
    val coffeenetworker   = Achievement(Bid("kcoffeeconnector"), "I like my coworkers a lot", "Meet with ten others at the coffeemachine in a day")
    val coffeaddict       = Achievement(Bid("Kcoffeaddicted"), "Coffeeaddicted", "Get coffee five times in one hour")

    val seeAllTheDesksBronze = Achievement(Bid("seeAllTheDesksBronze"), "One desk down", "Visitng at least one desk")
    val seeAllTheDesksSilver = Achievement(Bid("seeAllTheDesksSilver"), "Both were thrilling", "Visiting half the desks")
    val seeAllTheDesksGold   = Achievement(Bid("seeAllTheDesksGold"), "Be at all the desks", "Visiting all the stands")

    val meetingRoomRoamer = Achievement(Bid("kmeetingroomroamer"), "Never settle", "Visit four meetingrooms in a day")
    val meetingAttender   = Achievement(Bid("kmeetingAttender"), "A meeting might be fun", "Stay at one meetingroom for more than an hour")
    val meetingAttenders  = Achievement(Bid("kmeetingAttenders"), "A meeting a day", "Stay at one meetingroom for more than an hour for a week")
    val meetingRoomStayer = Achievement(Bid("kmeetingroomstayer"), "Excellent focus", "Stay at a meetingroom for more than three hours")

    val coffeeHero  = Quest(Qid("kcoffeehero"), "Coffee connaisseur", "Drink coffe, the more the better", Public, List(coffeeliker, coffeexperimenter, coffeeconnector, coffeaddict))
    val earlybird   = Quest(Qid("kearlybird"), "The early bird", "Come early to work, feel the rising sun!", Public, List(earlyprettyearlyatwork, earlyatwork, earlyenough, earlyatworks))
    val stayer      = Quest(Qid("kstayer"), "The stayer", "Put in your hours, its good for the company and you", Public, List(lateprettylateatwork, lateatwork, lateprettylateatworks, lateatworks))
    val meetinghero = Quest(Qid("kmeetinroomhero"), "Meeting maniac", "Meetings are great for productivity!", Public, List(meetingAttender, meetingRoomRoamer, meetingRoomStayer, meetingAttenders))
    val roamer      = Quest(Qid("kroamer"), "The roamer", "Look for the desks", Public, List(seeAllTheDesksBronze, seeAllTheDesksSilver, seeAllTheDesksGold))


    val meetingRoomRoamerTracker =
      progresstracker.value(
        ((first(enter(areas.mrtTuring)) ++
          first(enter(areas.mrtTesla)) ++
          first(enter(areas.mrtHopper)) ++
          first(enter(areas.mrtEngelbart)) ++
          first(enter(areas.mrtAda)) ++
          first(enter(areas.mrtCurie))) or
          halt(on(endOfDay))).repeat,
        meetingRoomRoamer)

    val visitAllDesksTracker =
      progresstracker.collect(
      last(enter(areas.desk1)) or last(enter(areas.desk2)) or last(enter(areas.desk3)), { case EnteredArea(_, area, _) :: tail => area}) {
        case 1 => seeAllTheDesksBronze.some
        case 2 => seeAllTheDesksSilver.some
        case 3 => seeAllTheDesksGold.some
        case _ => none
      }



    //Early
    val prettyEarlyAtWorkTracker =
      progresstracker.value(
        last(visitCoffee and time(_.getHourOfDay <= 9)).haltAfter,
        earlyatwork
      )

    val earlyAtWorkTracker =
      progresstracker.value(
        last(visitCoffee and time(_.getHourOfDay <= 8)).haltAfter,
        earlyatwork
      )

    val earlyEnoughTracker =
      progresstracker.value(
        (last(startOfDay) and on(visitCoffee and time(_.getHourOfDay <= 9))).times(5),
        earlyenough
      )

    val earlyAtWorksTracker =
      progresstracker.value(
        (last(startOfDay) and on(visitCoffee and time(_.getHourOfDay <= 8))).times(5),
        earlyatworks
      )

    //LAte
    val prettyLateAtWorkTracker =
      progresstracker.value(
        on(visitCoffee) and time(_.getHourOfDay >= 16),
        lateprettylateatwork
      )

    val lateAtWorkTracker =
      progresstracker.value(
        on(visitCoffee) and time(_.getHourOfDay >= 17),
        lateatwork
      )

    val lateEnoughTracker =
      progresstracker.value(
        last(startOfDay) and on(visitCoffee and time(_.getHourOfDay >= 16)).times(5),
        lateprettylateatworks
      )

    val lateAtWorksTracker =
      progresstracker.value(
        last(startOfDay) and on(visitCoffee and time(_.getHourOfDay >= 17)).times(5),
        lateatworks
      )

    val coffeeLikerTracker =
      progresstracker.value(
        on(visitCoffee).times(5) or halt(on(endOfDay)),
        coffeeliker
      )

    val coffeexperimenterTracker =
      progresstracker.value(
        (last(enter(kantegaCoffeeUp)) and last(enter(kantegaCoffeeDn))) or halt(on(endOfDay)),
        coffeexperimenter
      )

    val coffeeConnectorTracker =
      progresstracker.value(
        on(metOtherPlayer).times(3) or halt(on(endOfDay)),
        coffeeconnector
      )

    val coffeeNetworkerTracker =
      progresstracker.value(
        on(metOtherPlayer).times(10) or halt(on(endOfDay)),
        coffeenetworker
      )

    val coffeeAddictTracker =
      progresstracker.value(
        on(visitCoffee).accumD(Hours.ONE).filter(_.facts.length > 10) or halt(on(endOfDay)),
        coffeenetworker
      )

    val meetingAttenderTracker =
      progresstracker.value(
        ((first(enterOneOf(areas.mrtTuring, areas.mrtTesla, areas.mrtHopper, areas.mrtEngelbart, areas.mrtAda, areas.mrtCurie)) ++
          first(exitOneOf(areas.mrtTuring, areas.mrtTesla, areas.mrtHopper, areas.mrtEngelbart, areas.mrtAda, areas.mrtCurie))) or
          halt(on(endOfDay))).repeat,
        coffeaddict
      )

  }

  //Quests
  val seeAllTalks       = Quest(Qid("seealltalks"), "See all the Talks", "Maximize your smart, see them all", Public, List(seetalksiron, seetalksbronze, seetalkssilver, seetalksgold))
  val attendAllSessions = Quest(Qid("visitallsessions"), "Attend all the Talks", "Maximize your smart, see them all", Public, List(firstsession, kreatorsession, fundingsession))
  val visitAllStands    = Quest(Qid("visitthestands"), "Visit the stands", "Visit as many stands you can", Public, List())
  val eagerNess         = Quest(Qid("beEager"), "The early bird", "Be on time", Public, List(earlybird, ontime))
  val antihero          = Quest(Qid("antihero"), "The Antihero", "No, you dont want to be good here", Secret, List(keepsringing, placestogo, tinyjavabladder, smalljavabladder))


  val ontimePred =
    fact { case arr: EnteredArea if DateTime.now().getHourOfDay < 9 => true}

  //val onTimeDayTwo =


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
    StatefulTracker[Set[Nick], Achievement](on(metOtherPlayer), Set()) { token => State { set =>
      val metOther =
        token.facts.head.asInstanceOf[MetPlayer]

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


  val quests =
    List(
      kq.coffeeHero,
      kq.earlybird,
      kq.meetinghero,
      kq.stayer,
      kq.roamer
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
      seetalksiron,
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
      ambassador,
      kq.meetingRoomRoamer,
      kq.meetingAttender,
      kq.meetingAttenders,
      kq.meetingRoomStayer,
      kq.earlyatwork,
      kq.earlyprettyearlyatwork,
      kq.earlyenough,
      kq.earlyatworks,
      kq.lateprettylateatwork,
      kq.lateprettylateatworks,
      kq.lateatwork,
      kq.lateatworks,
      kq.coffeeliker,
      kq.coffeexperimenter,
      kq.coffeeconnector,
      kq.coffeaddict,
      kq.coffeenetworker,
      kq.seeAllTheDesksBronze,
      kq.seeAllTheDesksSilver,
      kq.seeAllTheDesksGold
    )

  val badgesMap =
    badges
      .map(badge => (badge.id, badge))
      .toMap

  val trackerForQuest: Map[Qid, PatternTracker[Achievement]] =
    Map(
      kq.meetinghero.id -> networkingTracker,
      kq.roamer.id -> kq.visitAllDesksTracker
    )

  val zeroTracker =
    ZeroTracker[Achievement]()
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



