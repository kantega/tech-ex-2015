package techex.data

import argonaut.Argonaut._
import argonaut.{CodecJson, DecodeJson, EncodeJson, JsonLong}
import org.joda.time.Instant
import techex.cases.playerSignup.{CreatePlayerData, PlatformData}
import techex.domain._

object codecJson {

  implicit val areaCodec: CodecJson[Region] =
    casecodec1(Region.apply, Region.unapply)("name")

  implicit val visibilityEncode: EncodeJson[Visibility] =
    jencode1((v: Visibility) => v.asString)

  implicit val questIdEncode: EncodeJson[Qid] =
    jencode1L((id: Qid) => id.value)("id")

  implicit val questEncode: EncodeJson[Quest] =
    jencode3L((q: Quest) => (q.id.value, q.name, q.desc))("id", "title", "desc")


  implicit val badgeEncodeJson: EncodeJson[Achievement] =
    jencode3L((b: Achievement) => (b.id.value, b.name, b.desc))("id", "title", "desc")

  implicit val achievemntEncodeJson: EncodeJson[PlayerBadgeProgress] =
    EncodeJson(
      (a: PlayerBadgeProgress) =>
        ("id" := a.id) ->:
          ("title" := a.title) ->:
          ("desc" := a.desc) ->:
          ("achieved" := a.achieved) ->:
          jEmptyObject
    )

  implicit val progressEncodeJson: EncodeJson[PlayerQuestProgress] =
    EncodeJson(
      (progress: PlayerQuestProgress) =>
        ("id" := progress.quest.id.value) ->:
          ("title" := progress.quest.name) ->:
          ("desc" := progress.quest.desc) ->:
          ("visibility" := progress.quest.visibility.asString) ->:
          ("achievements" := progress.achievements) ->:
          jEmptyObject
    )

  implicit val encodeProximity: EncodeJson[Proximity] =
    jencode1((p: Proximity) => jString(p.asString))

  implicit val decodeProximity: DecodeJson[Proximity] =
    jdecode1((value: String) => Proximity(value))

  implicit val observationDecodeJson: DecodeJson[ObservationData] =
    jdecode2L(
      (beaconId: String, proximity: String) =>
        ObservationData(Beacon(beaconId), Proximity(proximity)))("beaconId", "proximity")

  implicit val summaryEncode: CodecJson[ProgressSummary] =
    casecodec4(ProgressSummary, ProgressSummary.unapply)("level", "max", "onQuest", "notOnQuest")


  implicit val totalQuestPogessEncode: EncodeJson[TotalQuestProgress] =
    EncodeJson(
      (a: TotalQuestProgress) =>
        ("id" := a.quest.id) ->:
          ("title" := a.quest.name) ->:
          ("progress" := a.progressSummary) ->:
          jEmptyObject
    )

  implicit val nickDecode: DecodeJson[Nick] =
    jdecode1((str: String) => Nick(str))

  implicit val nickEncode: EncodeJson[Nick] =
    jencode1((nick: Nick) => nick.value)

  implicit def playerCodecJson: CodecJson[Player] =
    CodecJson(
      (p: Player) =>
        ("nick" := p.nick.value) ->:
          ("id" := p.id.value) ->:
          ("preferences" := p.preference) ->:
          ("quests" := p.privateQuests.map(_.id.value)) ->:
          jEmptyObject,
      c => for {
        id <- (c --\ "id").as[String]
        nick <- (c --\ "nick").as[String]
        preference <- (c --\ "preferences").as[Option[PlayerPreference]]
        privateQuests <- (c --\ "quests").as[List[String]]
      } yield
        Player(
          PlayerId(id),
          Nick(nick),
          preference.getOrElse(PlayerPreference(Coke(), Salad())),
          privateQuests.map(str => quests.questMap(Qid(str))))
    )

  implicit val platformDecode: CodecJson[PlatformData] =
    casecodec2(PlatformData, PlatformData.unapply)("type", "deviceToken")

  implicit def playerPreferenceCode: CodecJson[PlayerPreference] =
    casecodec2(
      (drinkS: String, eatS: String) => PlayerPreference(Drink(drinkS), Eat(eatS)),
      (preference: PlayerPreference) => Some(preference.drink.asString, preference.eat.asString)
    )("drink", "eat")

  implicit val createPlayerDataDecode: CodecJson[CreatePlayerData] =
    casecodec3(CreatePlayerData, CreatePlayerData.unapply)("nick", "platform", "preferences")

  implicit val codecBeacon: CodecJson[Beacon] =
    casecodec1(Beacon.apply, Beacon.unapply)("value")

  implicit val codecPlayerId: CodecJson[PlayerId] =
    casecodec1(PlayerId.apply, PlayerId.unapply)("value")

  implicit val codecInstant: CodecJson[Instant] =
    CodecJson((instant: Instant) => JsonLong(instant.getMillis).asJsonOrNull, c => c.as[Long].map(l => new Instant(l)))

  implicit val codecCrateUser: CodecJson[CreatePlayer] =
    casecodec1(CreatePlayer.apply, CreatePlayer.unapply)("data")

  implicit val codecObservation: CodecJson[Observation] =
    casecodec4(Observation.apply, Observation.unapply)("beacon", "playerId", "instant", "proximity")

  implicit val codecInputMessage: CodecJson[InputMessage] =
    CodecJson(
      (input: InputMessage) => input match {
        case cp: CreatePlayer => codecCrateUser.encode(cp)
        case _                => jEmptyObject
      },
      (json) => null
    )

  implicit val encodeFact: EncodeJson[Fact] =
    EncodeJson((fact: Fact) => fact match {
      case JoinedActivityLate(player, event, instant)    => ("activity" := "joinedLate") ->: ("event" := event.id.value) ->: ("area" := event.area.name) ->: ("instant" := instant) ->: jEmptyObject
      case LeftActivityEarly(player, event, instant)     => ("activity" := "leftEarly") ->: ("event" := event.id.value) ->:("area" := event.area.name) ->: ( "instant" := instant) ->: jEmptyObject
      case JoinedOnStart(player, event, instant)         => ("activity" := "arrivedOnTime") ->: ("event" := event.id.value) ->:("area" := event.area.name) ->: ( "instant" := instant) ->: jEmptyObject
      case LeftOnEnd(player, event, instant)             => ("activity" := "leftOnTime") ->: ("event" := event.id.value) ->:("area" := event.area.name) ->: ( "instant" := instant) ->: jEmptyObject
      case EnteredRegion(player, area, instant)          => ("activity" := "arrivedAtArea") ->: ("area" := area.name) ->: ("instant" := instant) ->: jEmptyObject
      case LeftRegion(player, area, instant)               => ("activity" := "leftFromArea") ->: ("area" := area.name) ->: ("instant" := instant) ->: jEmptyObject
      case MetPlayer(player, otherPlayer, instant)       => ("activity" := "metOther") ->: ("player" := player.player.nick.value) ->: ("other" := otherPlayer.player.nick.value) ->: ("instant" := instant) ->: jEmptyObject
      case EarnedAchievemnt(player, achievemnt, instant) => ("activity" := "earnedAchievement") ->: ("player" := player.player.nick.value) ->: ("badge" := achievemnt.id.value) ->: ("instant" := instant) ->: jEmptyObject
      case AwardedBadge(player, badge, instant)          => ("activity" := "earnedBadge") ->: ("player" := player.player.nick.value) ->: ("badge" := badge.achievement.id.value) ->: ("instant" := instant) ->: jEmptyObject
      case PlayerCreated(player, instant)                => ("activity" := "playerCreated") ->: ("player" := player.player.nick.value) ->: ("instant" := instant) ->: jEmptyObject
      case Started(entry, instant)                       => ("activity" := "eventStarted") ->: ("event" := entry.id.value) ->:("area" := entry.area.name) ->: ( "instant" := instant) ->: jEmptyObject
      case Ended(entry, instant)                         => ("activity" := "eventEnded") ->: ("event" := entry.id.value) ->:("area" := entry.area.name) ->: ( "instant" := instant) ->: jEmptyObject
      case Added(entry, instant)                         => ("activity" := "eventAdded") ->: ("event" := entry.id.value) ->:("area" := entry.area.name) ->: ( "instant" := instant) ->: jEmptyObject
      case Removed(entry, instant)                       => ("activity" := "eventRemoved") ->: ("event" := entry.id.value) ->:("area" := entry.area.name) ->: ( "instant" := instant) ->: jEmptyObject
      case _                                             => jEmptyObject
    }


    )
}
