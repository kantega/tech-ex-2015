package techex.domain

import argonaut.{Argonaut, CodecJson}
import org.joda.time.Minutes._
import org.joda.time._
import techex._
import techex.domain.patternmatching._
import techex.domain.preds._
import areas._
import techex.domain.scheduling._
import scalaz.Scalaz._
import scalaz._


object quests {

  implicit val isStayAfterLeft: Pred[(LeftOnEnd, (EnteredArea, LeftArea))] =
    pair => pair._1.instant.isBefore(pair._2._1.instant)


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

  def startOfDayTwo: Matcher[StartOfDay] =
    on[StartOfDay].filter(time(dt => dt.getDayOfMonth === day2.getStart.getDayOfMonth && dt.getMonthOfYear === day2.getStart.getMonthOfYear))

  def time(t: DateTime => Boolean): Pred[Fact] =
    fact => t(fact.instant.toDateTime.toDateTime(DateTimeZone.forOffsetHours(1)))

  def within(interval: Interval): Pred[Fact] =
    fact => interval.contains(fact.instant)

  def started(event: ScheduleEntry): Matcher[Started] =
    on[Started].filter(evt => evt.entry === event)

  def ended(event: ScheduleEntry): Matcher[Ended] =
    on[Ended].filter(evt => evt.entry === event)

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

  def stay(dur: ReadableDuration)(pred: Pred[Area]): Matcher[AtArea] =
    on[AtArea].filter(at => pred(at.area) && at.duration.isLongerThan(dur))

  def see(pred: ScheduleEntry => Boolean) = //Matcher[(JoinedOnStart, LeftOnEnd)] =
    (on[JoinedOnStart].filter(evt => pred(evt.event)).last and on[LeftOnEnd].last).filter(pair => pair._1.event === pair._2.event)

  def seeEntry(entry: ScheduleEntry) =
    see(evt => evt === entry)

  def stayForFifteenSecs = stay(Seconds.seconds(15)) _

  def stayForThirtySecs = stay(Seconds.seconds(30)) _

  def stayForFive = stay(minutes(5)) _

  def stayForTen = stay(minutes(10)) _

  def stayForThirty = stay(minutes(30)) _

  def stayForHour = stay(Hours.ONE) _

  def stayforTwoHours = stay(Hours.TWO) _

  def stayforThreeHours = stay(Hours.THREE) _

  val metOtherPlayer =
    fact({ case met: MetPlayer => true})

  val startOfDay =
    fact { case mn: StartOfDay => true}

  val endOfDay =
    fact { case mn: EndOfDay => true}

  def cameLate =
    (on[Started].last and on[JoinedActivityLate].last)
      .filter(both => both._1.entry === both._2.event && durationBetween(both._1.instant, both._2.instant).isShorterThan(minutes(10).toStandardDuration))


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

    val visitAllDesksTracker     =
      progresstracker.collect(
        enterOneOf(areas.desk1, areas.desk2, areas.desk3), (e: EnteredArea) => e.area.some) {
        case 1 => seeAllTheDesksBronze.some
        case 2 => seeAllTheDesksSilver.some
        case 3 => seeAllTheDesksGold.some
        case _ => none
      }
    val prettyEarlyAtWorkTracker = progresstracker.value(enter(visitCoffee).filter(time(_.getHourOfDay <= 9)).haltAfter, earlyprettyearlyatwork)
    val earlyAtWorkTracker       = progresstracker.value(enter(visitCoffee).filter(time(_.getHourOfDay <= 8)).haltAfter, earlyatwork)
    val earlyEnoughTracker       = progresstracker.value((last(startOfDay) and enter(visitCoffee).filter(time(_.getHourOfDay <= 9))).times(5), earlyenough)
    val earlyAtWorksTracker      = progresstracker.value((last(startOfDay) and enter(visitCoffee).filter(time(_.getHourOfDay <= 8))).times(5), earlyatworks)
    val prettyLateAtWorkTracker  = progresstracker.value(enter(visitCoffee).filter(time(_.getHourOfDay >= 16)).haltAfter, lateprettylateatwork)
    val lateAtWorkTracker        = progresstracker.value(enter(visitCoffee).filter(time(_.getHourOfDay >= 17)).haltAfter, lateatwork)
    val lateEnoughTracker        = progresstracker.value(last(startOfDay) and enter(visitCoffee).filter(time(_.getHourOfDay >= 16)).times(5), lateprettylateatworks)
    val lateAtWorksTracker       = progresstracker.value(last(startOfDay) and enter(visitCoffee).filter(time(_.getHourOfDay >= 17)).times(5), lateatworks)
    val coffeeLikerTracker       = progresstracker.value((stayForFifteenSecs(visitCoffee).times(5) or halt(on[EndOfDay])).repeat, coffeeliker)
    val coffeexperimenterTracker = progresstracker.value(((stayForFifteenSecs(at(kantegaCoffeeUp)).last and stayForFifteenSecs(at(kantegaCoffeeDn)).last) or halt(on[EndOfDay])).repeat, coffeexperimenter)
    val coffeeConnectorTracker   = progresstracker.value((on[MetPlayer].times(3) or halt(on[EndOfDay])).times(1), coffeeconnector)
    val coffeeNetworkerTracker   = progresstracker.value((on[MetPlayer].times(10) or halt(on[EndOfDay])).times(1), coffeenetworker)
    val coffeeAddictTracker      = progresstracker.value((stayForFifteenSecs(visitCoffee).times(10) or halt(on[EndOfDay])).times(1), coffeenetworker)
    val meetingRoomRoamerTracker =
      progresstracker.collect(
        (stayForThirtySecs(atAnyMeetingRoom).map(_.area) xor halt(on[EndOfDay])).repeat, (pair: Area \/ EndOfDay) => pair.fold(_.some, ex => none)) {
        case 4 => meetingRoomRoamer.some
        case _ => none
      }
    val meetingAttenderTracker   = progresstracker.value((stayForHour(atAnyMeetingRoom) xor halt(on[EndOfDay])).times(1), meetingAttender)
    val meetingAttendersTracker  = progresstracker.value((stayForHour(atAnyMeetingRoom) xor halt(on[EndOfDay])).times(5), meetingAttender)
    val meetingRoomStayerTracker = progresstracker.value((stayforTwoHours(atAnyMeetingRoom) xor halt(on[EndOfDay])).times(1), meetingRoomStayer)

  }

  val kantegaQuests =
    List(
      kq.coffeeHero,
      kq.earlybird,
      kq.meetinghero,
      kq.stayer,
      kq.roamer
    )


  val badgeSeeAppetiteForC            = Achievement(Bid("badgeSeeAppetiteForC"), "Seen Appetitite for Construction", "Be at the session Appetite for Construction")
  val badgeSeeEntre                   = Achievement(Bid("badgeSeeEntre"), "Seen Entrepreneurial State of Mind", "Be at the session Entrepreneurial State of Mind")
  val badgeSeeBlodSwotAndTears        = Achievement(Bid("abdgeSeeBlodSwotAndTears"), "Seen Blood Swot and tears", "Be at the session See Blood Swot and tears")
  val badgeSeeAllSessions             = Achievement(Bid("badgeSeeAllSessions"), "Seen all the sessions", "Be at all the sessions")
  val badgeEarlyBird                  = Achievement(Bid("badgeEearlyBird"), "The early bird", "Arrived more than 20 minutes prior to program start on day 1 or day 2")
  val badgeStarStruck                 = Achievement(Bid("badgeStarStruck"), "Starstruck", "Hang around the stage area for more then 6 minutes after the session ended")
  val badgeComfyChairs                = Achievement(Bid("comfyChairs"), "Damn! These chairs are comfortable", "Spend an entire brake in the auditorium")
  val badgeOnTimeLate                 = Achievement(Bid("badgeOnTimeLate"), "If you're there right on time you're 5 minutes late", "Be present in the room 2 minute before session starts")
  val badgeVisitAllStands             = Achievement(Bid("badgeVisitAllStands"), "Visit all the stands", "Be in close proximity to all stands once througout the conference")
  val badgeVisitAllStandsBoth         = Achievement(Bid("badgeVisitAllStandsBoth"), "Visit all the stands on both days", "Be in close proximity to all stands once both days")
  val badgeHaveACupper                = Achievement(Bid("badgeHaveACupper"), "Have a cupper", "Be in the proximity of coffe stand three times")
  val badgeMoreCoffee                 = Achievement(Bid("badgeMoreCoffee"), "You should probably blink by now #MoreCoffee?", "Be in close proximity to the coffee stand more then 5 times during one day.")
  val badgeILoveThisStand             = Achievement(Bid("badgeILoveThisStand"), "Wow! I love this Stand", "Hang around one stand for more then 10 minutes")
  val badgeVisitAllStandsInLunchBreak = Achievement(Bid("badgeVisitAllStandsInLunchBreak"), "Visit all the stands in the lunch break", "Visit all stands during the lunchbreak on thursday")
  val badgeNetworker                  = Achievement(Bid("badgeNetworker"), "Super networker", "Meet more than 4 people in the networkingroom")
  val badgePartyAnimal                = Achievement(Bid("badgePartyAnimal"), "Party Animal", "Be present at Samfundet after 01:00")
  val badgeTheResponsible             = Achievement(Bid("badgeTheResponsible"), "The responsible one", "Leave Samfundet prior to 22:15")
  val badgeHeroAtNightAndDay          = Achievement(Bid("badgeHeroAtNightAndDay"), "Hero at night, hero at day", "Present at Samfundet later then 01:00 AND present at the conference prior to 08:30 on day 2")
  val badgeDanceMoves                 = Achievement(Bid("badgeDanceMoves"), "You've got the moves", "Stay in the bar for more than 30 min")
  val badgeBeThereInASec              = Achievement(Bid("badgeBeThereInASec"), "I'll be there in a sec", "Stay at a stand uptil ten minutes after a session has started")

  val trackSeeApettiteForConstruction = progresstracker.value(seeEntry(sessionAppetiteForC), badgeSeeAppetiteForC)
  val trackSeeEntre                   = progresstracker.value(seeEntry(sessionEntrStateOfMind), badgeSeeEntre)
  val trackSeeBlodSwotAndT            = progresstracker.value(seeEntry(sessionBloodSwotTears), badgeSeeBlodSwotAndTears)
  val trackSeeAllSessions             = progresstracker.value(see(x => true).times(6), badgeSeeAllSessions)
  val trackEarlyBird                  = progresstracker.value(on[EnteredArea].filter(time(_.isBefore(day1.getStart.plus(minutes(40))))) or on[EnteredArea].filter(within(day2.getStart.minus(minutes(120)) until day2.getStart.minus(minutes(20)))), badgeEarlyBird)
  val trackStarStruck                 = progresstracker.value(on[LeftOnEnd].last fby stayForFive(at(stage)).last, badgeStarStruck)
  val trackComfyChairs                = progresstracker.value(on[LeftOnEnd].last fby stayForTen(at(auditorium)).last, badgeComfyChairs)
  val trackOnTimeIsLate               = progresstracker.value((on[EnteredArea].last and on[JoinedOnStart].end).repeat.filter(pair => pair._1.instant.isBefore(pair._2.instant.minus(minutes(2)))), badgeOnTimeLate)
  val visitAllStandsMatcher           =
    stayForThirtySecs(at(areas.standContext)).last and
      stayForThirtySecs(at(areas.standEntrepenerurShip)).last and
      stayForThirtySecs(at(areas.standUserInsight)).last and
      stayForThirtySecs(at(areas.standIntention)).last and
      stayForThirtySecs(at(areas.standProduction)).last and
      stayForThirtySecs(at(areas.standUse)).last and
      stayForThirtySecs(at(areas.standKantega)).last
  val trackVisitAllStands             = progresstracker.value((visitAllStandsMatcher before ended(sessionCrowdFund)) or (startOfDayTwo fby visitAllStandsMatcher), badgeVisitAllStands)
  val trackVisitAllStandsBoth         = progresstracker.value((visitAllStandsMatcher before ended(sessionCrowdFund)) and (startOfDayTwo fby visitAllStandsMatcher), badgeVisitAllStandsBoth)
  val trackYouShouldBlink             = progresstracker.value(((enter(at(coffeeStand)).last and stayForFifteenSecs(at(coffeeStand))).times(5) before ended(sessionCrowdFund)) or (startOfDayTwo fby stayForFifteenSecs(at(coffeeStand)).times(5)), badgeMoreCoffee)
  val trackILoveThisStand             = progresstracker.value(stayForTen(atOneOf(areas.standContext, areas.standEntrepenerurShip, areas.standIntention, areas.standKantega, areas.standProduction, areas.standUse, areas.standUserInsight)), badgeILoveThisStand)
  val trackVisitAllStandsInLunchBreak = progresstracker.value(started(lunchDayTwo) fby visitAllStandsMatcher until ended(lunchDayTwo), badgeVisitAllStandsInLunchBreak)
  val trackNetworker                  = progresstracker.value(on[MetPlayer].times(4), badgeNetworker)
  val trackPartyAnimal                = progresstracker.value(exitOneOf(samfKlubben, samfStorsal).filter(time(t => t.isAfter(samfundetEnds))), badgePartyAnimal)
  val trackTheResponsible             = progresstracker.value(exitOneOf(samfKlubben, samfStorsal).last.filter(time(t => t.isBefore(leaveEarlyFromSamfundet))) and stayforThreeHours(at(somewhere)), badgeTheResponsible)
  val trackHeroNightAndDay            = progresstracker.value(exitOneOf(samfKlubben, samfStorsal).filter(time(t => t.isAfter(samfundetEnds))) and enterOneOf(areas.standContext, areas.standEntrepenerurShip, areas.standIntention, areas.standKantega, areas.standProduction, areas.standUse, areas.standUserInsight, areas.auditorium, areas.coffeeStand).filter(within(samfundetEnds until arriveearlyOnDay2)), badgeHeroAtNightAndDay)
  val trackYouveGotTheMoves           = progresstracker.value(stayForHour(at(samfKlubben)), badgeDanceMoves)
  val trackBeThereInASec              = progresstracker.value(cameLate, badgeBeThereInASec)
  val trackHaveACupper                = progresstracker.value((enter(at(coffeeStand)).last and stayForFifteenSecs(at(coffeeStand))).times(3), badgeHaveACupper)

  //Quests
  val questInSearchForInspiration = Quest(Qid("inSearchForInsp"), "In search for inspiration", "", Public, List(badgeSeeAllSessions, badgeComfyChairs, badgeOnTimeLate, badgeVisitAllStands, badgeNetworker))
  val questStayer                 = Quest(Qid("stayer"), "Be the stayer", "", Public, List(badgeStarStruck, badgeEarlyBird, badgeILoveThisStand, badgePartyAnimal, badgeDanceMoves))
  val questForScience             = Quest(Qid("forscience"), "Explore for science!", "", Secret, List(badgeSeeEntre, badgeHaveACupper, badgeTheResponsible, badgeBeThereInASec, badgeVisitAllStandsInLunchBreak))
  val questForiNnovationAndBeyond = Quest(Qid("forinnovationAndBeyond"), "For innovation and beyond!", "", Public, List(badgeSeeBlodSwotAndTears, badgeMoreCoffee, badgePartyAnimal, badgeHeroAtNightAndDay, badgeVisitAllStandsBoth))


  val quests =
    List(
      questInSearchForInspiration,
      questStayer,
      questForScience,
      questForiNnovationAndBeyond
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
      badgeSeeAppetiteForC,
      badgeSeeEntre,
      badgeSeeBlodSwotAndTears,
      badgeSeeAllSessions,
      badgeEarlyBird,
      badgeStarStruck,
      badgeComfyChairs,
      badgeOnTimeLate,
      badgeVisitAllStands,
      badgeVisitAllStandsBoth,
      badgeHaveACupper,
      badgeILoveThisStand,
      badgeVisitAllStandsInLunchBreak,
      badgeNetworker,
      badgePartyAnimal,
      badgeTheResponsible,
      badgeHeroAtNightAndDay,
      badgeDanceMoves,
      badgeBeThereInASec,

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
      questInSearchForInspiration.id -> (trackSeeAllSessions ++ trackComfyChairs ++ trackOnTimeIsLate ++ trackVisitAllStands ++ trackNetworker),
      questStayer.id -> (trackStarStruck ++ trackEarlyBird ++ trackILoveThisStand ++ trackPartyAnimal ++ trackYouveGotTheMoves),
      questForScience.id -> (trackSeeEntre ++ trackHaveACupper ++ trackTheResponsible ++ trackBeThereInASec ++ trackVisitAllStandsInLunchBreak),
      questForiNnovationAndBeyond.id -> (trackSeeBlodSwotAndT ++ trackYouShouldBlink ++ trackPartyAnimal ++ trackHeroNightAndDay ++ trackVisitAllStandsBoth),
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



