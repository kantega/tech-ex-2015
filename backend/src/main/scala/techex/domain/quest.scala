package techex.domain

import scalaz._, Scalaz._

object quests {


  val seeAllTalks = Quest(Qid("seealltalks"), "See all the Talks", "Maximize your smart, see them all")
  val networking  = Quest(Qid("networkingchamp"), "Networking champion", "Connect, its good for you (and your stats)")

  val seetalksbronze   = Badge(Bid("seetalksbronze"), "Two talks down", "Attending two talks", Public, seeAllTalks.some)
  val seetalkssilver   = Badge(Bid("seetalkssilver"), "Three is silver", "Attending three talks", Public, seeAllTalks.some)
  val seetalksgold     = Badge(Bid("seetalksgold"), "Seen all the talks", "Attending all talks", Public, seeAllTalks.some)
  val firstsession     = Badge(Bid("firstsession"), "I have seen the light", "Attending a session", Public, none)
  val kreatorsession   = Badge(Bid("kreatorsession"), "I have been at Kreator", "Attending Kreator", Public, none)
  val fundingsession   = Badge(Bid("fundingsession"), "I know you've got funds", "Attending live crowdfunding", Public, none)
  val keepsringing     = Badge(Bid("keepsringing"), "Damned thing keeps ringing", "Left session for a couple of minutes", Public, none)
  val placestogo       = Badge(Bid("placestogo"), "Got places to go, people to meet...", "Left a session early", Public, none)
  val tinyjavabladder  = Badge(Bid("tinyjavabladder"), "Tiny java bladder", "Left session for toilet multiple times", Secret, none)
  val smalljavabladder = Badge(Bid("smalljavabladder"), "Small java bladder", "Left session for toilet one time", Secret, none)
  val earlybird        = Badge(Bid("earlybird"), "Early bird", "Arrived 20 mins early", Public, none)
  val ontime           = Badge(Bid("ontime"), "If you are there on time, you are late", "Do not be late", Personal, none)
  val intlnetworker    = Badge(Bid("intlnetworker"), "International networker", "You have many international connections", Public, networking.some)
  val ambassador       = Badge(Bid("ambassador"), "Ambassador", "You add a lot of connection just right after they join the game", Personal, networking.some)


  val quests =
    List(
      seeAllTalks,
      networking
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
}
case class Qid(value: String)
case class Bid(value: String)
case class Quest(id: Qid, name: String, desc: String)
object Quest{
  implicit val questEqual:Equal[Quest] =
  Equal.equalA[String].contramap(_.id.value)
}
case class Badge(id: Bid, name: String, desc: String, visibility: Visibility, quest: Option[Quest])

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
object Visibility{
  implicit val visibilityEqual:Equal[Visibility]=
    Equal.equalRef[Visibility]
}
case class QuestProgress(quest: Quest, achievements: List[Achievement])
case class Achievement(badge: Badge, achieved: Boolean, achievedBy: List[Nick])