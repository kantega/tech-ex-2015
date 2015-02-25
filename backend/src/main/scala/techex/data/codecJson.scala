package techex.data

import argonaut.Argonaut._
import argonaut.{CodecJson, DecodeJson, EncodeJson, JsonLong}
import org.joda.time.Instant
import techex.domain._

object codecJson {

  implicit val areaCodec: CodecJson[Area] =
    casecodec1(Area.apply, Area.unapply)("name")

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

  implicit val nickCodec: CodecJson[Nick] =
    casecodec1(Nick.apply, Nick.unapply)("value")


  implicit val codecBeacon: CodecJson[Beacon] =
    casecodec1(Beacon.apply, Beacon.unapply)("value")

  implicit val codecPlayerId: CodecJson[PlayerId] =
    casecodec1(PlayerId.apply, PlayerId.unapply)("value")

  implicit val codecInstant: CodecJson[Instant] =
    CodecJson((instant: Instant) => JsonLong(instant.getMillis).asJsonOrNull, c => c.as[Long].map(l => new Instant(l)))

  implicit val codecCrateUser: CodecJson[CreatePlayer] =
    casecodec2(CreatePlayer.apply, CreatePlayer.unapply)("nick", "data")

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
      case JoinedActivityLate(player, event)          => Map("activity"->"joinedLate","event"->event.id.value,"area"->event.area.id).asJson
      case LeftActivityEarly(player, event)           => Map("activity"->"leftEarly","event"->event.id.value,"area"->event.area.id).asJson
      case JoinedOnTime(player, event)                => Map("activity"->"arrivedOnTime","event"->event.id.value,"area"->event.area.id).asJson
      case LeftOnTime(player, event)                  => Map("activity"->"leftOnTime","event"->event.id.value,"area"->event.area.id).asJson
      case ArrivedAtArea(player, area)                => Map("activity"->"arrivedAtArea","area"->area.id).asJson
      case LeftArea(player, area)                     => Map("activity"->"leftFromArea","area"->area.id).asJson
      case MetPlayer(player, otherPlayer)             => Map("activity"->"metOther","player"->player.player.nick.value,"other"->otherPlayer.player.nick.value).asJson
      case EarnedAchievemnt(player, achievemnt)       => Map("activity"->"earnedAchievement","player"->player.player.nick.value,"badge"->achievemnt.id.value).asJson
      case AwardedBadge(player, badge)                => Map("activity"->"earnedBadge","player"->player.player.nick.value,"badge"->badge.achievement.id.value).asJson
      case PlayerCreated(player)                      => Map("activity"->"playerCreated","player"->player.player.nick.value).asJson
      case Started(instant, entry)                    => Map("activity"->"eventStarted","event"->entry.id.value,"area"->entry.area.id).asJson
      case Ended(instant, entry)                      => Map("activity"->"eventEnded","event"->entry.id.value,"area"->entry.area.id).asJson
      case Added(entry)                               => Map("activity"->"eventAdded","event"->entry.id.value,"area"->entry.area.id).asJson
      case Removed(entry)                             => Map("activity"->"eventRemoved","event"->entry.id.value,"area"->entry.area.id).asJson
      case _                                          => jEmptyObject
    }


    )
}
