package techex.domain

import argonaut.{Argonaut, CodecJson}
import org.joda.time.Minutes._
import org.joda.time._
import techex._
import techex.domain.patternmatching._
import techex.domain.preds._
import areas._
import scalaz.Scalaz._
import scalaz._


object quests {


  def last(p: Pred[Fact]) =
    p.last

  def at(area: Area): Pred[Area] =
    a => {
      val eq = a === area
      eq
    }

  def atOneOf(areas: Area*): Pred[Area] =
    area => areas.exists(a => a === area)

  def enter(p: Pred[Area]): Matcher[EnteredArea] =
    on[EnteredArea].filter(f => p(f.area))

  def enterOneOf(areas: Area*): Matcher[EnteredArea] =
    on[EnteredArea].filter(entered => areas.exists(a => a === entered.area))

  def exitOneOf(areas: Area*): Pred[Fact] =
    fact({ case LeftArea(_, area, _) if areas.exists(a => a === area) => true})


  def time(t: DateTime => Boolean): Pred[Fact] =
    fact => t(fact.instant.toDateTime.toDateTime(DateTimeZone.forOffsetHours(1)))

  def size(t: Int): Pred[List[Fact]] =
    list => (list.length >= t)

  def visitCoffee =
    at(kantegaCoffeeUp) or at(kantegaCoffeeDn)

  //Patterns
  val joinedAnActivityOnTime =
    fact { case j: JoinedOnStart => true}

  val enteredArea =
    fact({ case entered: EnteredArea => true})

  val leftArea =
    fact({ case entered: LeftArea => true})

  def stay(dur: ReadableDuration)(pred: Pred[Area]): Matcher[(EnteredArea, LeftArea)] =
    (on[EnteredArea].last and on[LeftArea].last).filter {
      pair => {
        val enter = pair._1
        val exit = pair._2
        val atSameArea = enter.area === exit.area
        val atCorrectArea = pred(enter.area)
        val stayLongEnough = durationBetween(enter.instant, exit.instant).isLongerThan(dur)
        atCorrectArea && atSameArea && stayLongEnough
      }
    }

  def stayForThirtySecs = stay(Seconds.seconds(30)) _

  def stayForFive = stay(Minutes.minutes(5)) _

  def stayForTen = stay(Minutes.minutes(10)) _

  def stayForThirty = stay(Minutes.minutes(30)) _

  def stayForHour = stay(Hours.ONE) _

  def stayforTwoHours = stay(Hours.TWO) _

  val metOtherPlayer =
    fact({ case met: MetPlayer => true})

  val startOfDay =
    fact { case mn: StartOfDay => true}

  val endOfDay =
    fact { case mn: EndOfDay => true}

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
    val earlyatworks           = Achievement(Bid("kearlyatworks"), "Came in early today", "Have coffee before 08:00 for a week")

    val lateprettylateatwork  = Achievement(Bid("kprettylateatwork"), "Had some stuff to do.", "Have coffee after 16:00")
    val lateprettylateatworks = Achievement(Bid("kprettylateatworks"), "Had a lot of stuff to do.", "Have coffee after 16:00 for a week")
    val lateatwork            = Achievement(Bid("klateatwork"), "Had some more stuff to do.", "Have coffee after 17:00")
    val lateatworks           = Achievement(Bid("klateatworks"), "I like staying at work.", "Have coffee after 17:00 for a week")

    val coffeeliker       = Achievement(Bid("kkoffehero"), "I like coffee", "Get coffee five times in a day")
    val coffeexperimenter = Achievement(Bid("kkoffeexperimenter"), "I like to experiment", "Have coffe from both areas in a day")
    val coffeeconnector   = Achievement(Bid("kcoffeeconnector"), "I like my coworkers", "Meet with three others at the coffeemachine in one day")
    val coffeenetworker   = Achievement(Bid("kcoffenetworker"), "I like my coworkers a lot", "Meet with ten others at the coffeemachine in a day")
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


    val atAnyMeetingRoom =
      atOneOf(areas.mrtTuring, areas.mrtTesla, areas.mrtHopper, areas.mrtEngelbart, areas.mrtAda, areas.mrtCurie)


    val visitAllDesksTracker =
      progresstracker.collect(
        enterOneOf(areas.desk1, areas.desk2, areas.desk3), (e: EnteredArea) => e.area.some) {
        case 1 => seeAllTheDesksBronze.some
        case 2 => seeAllTheDesksSilver.some
        case 3 => seeAllTheDesksGold.some
        case _ => none
      }


    //Early
    val prettyEarlyAtWorkTracker =
      progresstracker.value(
        enter(visitCoffee).filter(time(_.getHourOfDay <= 9)).haltAfter,
        earlyprettyearlyatwork
      )

    val earlyAtWorkTracker =
      progresstracker.value(
        enter(visitCoffee).filter(time(_.getHourOfDay <= 8)).haltAfter,
        earlyatwork
      )

    val earlyEnoughTracker =
      progresstracker.value(
        (last(startOfDay) and enter(visitCoffee).filter(time(_.getHourOfDay <= 9))).times(5),
        earlyenough
      )

    val earlyAtWorksTracker =
      progresstracker.value(
        (last(startOfDay) and enter(visitCoffee).filter(time(_.getHourOfDay <= 8))).times(5),
        earlyatworks
      )

    //LAte
    val prettyLateAtWorkTracker =
      progresstracker.value(
        enter(visitCoffee).filter(time(_.getHourOfDay >= 16)).haltAfter,
        lateprettylateatwork
      )

    val lateAtWorkTracker =
      progresstracker.value(
        enter(visitCoffee).filter(time(_.getHourOfDay >= 17)).haltAfter,
        lateatwork
      )

    val lateEnoughTracker =
      progresstracker.value(
        last(startOfDay) and enter(visitCoffee).filter(time(_.getHourOfDay >= 16)).times(5),
        lateprettylateatworks
      )

    val lateAtWorksTracker =
      progresstracker.value(
        last(startOfDay) and enter(visitCoffee).filter(time(_.getHourOfDay >= 17)).times(5),
        lateatworks
      )

    val coffeeLikerTracker =
      progresstracker.value(
        (stayForThirtySecs(visitCoffee).times(5) or halt(on[EndOfDay])).repeat,
        coffeeliker
      )

    val coffeexperimenterTracker =
      progresstracker.value(
        ((stayForThirtySecs(at(kantegaCoffeeUp)).last and stayForThirtySecs(at(kantegaCoffeeDn)).last) or halt(on[EndOfDay])).repeat,
        coffeexperimenter
      )

    val coffeeConnectorTracker =
      progresstracker.value(
        (on[MetPlayer].times(3) or halt(on[EndOfDay])).times(1),
        coffeeconnector
      )

    val coffeeNetworkerTracker =
      progresstracker.value(
        (on[MetPlayer].times(10) or halt(on[EndOfDay])).times(1),
        coffeenetworker
      )

    val coffeeAddictTracker =
      progresstracker.value(
        (stayForThirtySecs(visitCoffee).times(10) or halt(on[EndOfDay])).times(1),
        coffeenetworker
      )

    val meetingRoomRoamerTracker =
      progresstracker.collect(
        (stayForThirtySecs(atAnyMeetingRoom).map(_._1.area) xor halt(on[EndOfDay])).repeat, (pair: Area \/ EndOfDay) => pair.fold(_.some, ex => none)) {
        case 4 => meetingRoomRoamer.some
        case _ => none
      }

    val meetingAttenderTracker =
      progresstracker.value(
        (stayForHour(atAnyMeetingRoom) xor halt(on[EndOfDay])).times(1),
        meetingAttender
      )

    val meetingAttendersTracker =
      progresstracker.value(
        (stayForHour(atAnyMeetingRoom) xor halt(on[EndOfDay])).times(5),
        meetingAttender
      )

    val meetingRoomStayerTracker =
      progresstracker.value(
        (stayforTwoHours(atAnyMeetingRoom) xor halt(on[EndOfDay])).times(1),
        meetingRoomStayer
      )

    val quests =
      List(
        kq.coffeeHero,
        kq.earlybird,
        kq.meetinghero,
        kq.stayer,
        kq.roamer
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
      kq.meetinghero.id -> (kq.meetingRoomRoamerTracker ++ kq.meetingAttenderTracker ++ kq.meetingAttendersTracker ++ kq.meetingRoomStayerTracker),
      kq.roamer.id -> kq.visitAllDesksTracker,
      kq.stayer.id -> (kq.prettyLateAtWorkTracker ++ kq.lateAtWorkTracker ++ kq.lateEnoughTracker ++ kq.lateAtWorksTracker),
      kq.coffeeHero.id -> (kq.coffeeLikerTracker ++ kq.coffeexperimenterTracker ++ kq.coffeeConnectorTracker ++ kq.coffeeNetworkerTracker ++ kq.coffeeAddictTracker),
      kq.earlybird.id -> (kq.prettyEarlyAtWorkTracker ++ kq.earlyAtWorkTracker ++ kq.earlyEnoughTracker ++ kq.earlyAtWorksTracker)
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

case class RankEntry(player: String, noOfBadges: Int)
object RankEntry {
  implicit def rankEntryCodec: CodecJson[RankEntry] =
    Argonaut.casecodec2(RankEntry.apply, RankEntry.unapply)("player", "noOfBadges")
}



