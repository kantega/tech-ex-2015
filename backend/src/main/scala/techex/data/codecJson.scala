package techex.data

import argonaut.Argonaut._
import argonaut.{CodecJson, DecodeJson, EncodeJson}
import techex.domain._

object codecJson {

  implicit val areaCodec : CodecJson[Area] =
    casecodec1(Area.apply,Area.unapply)("name")

  implicit val visibilityEncode: EncodeJson[Visibility] =
    jencode1((v: Visibility) => v.getClass.getSimpleName.toLowerCase)

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

  implicit val observationDecodeJson: DecodeJson[ObservationData] =
    jdecode2L(
      ( beaconId: String, proximity: String) =>
        ObservationData(Beacon(beaconId), Proximity(proximity)))("beaconId","proximity")

  implicit val summaryEncode:CodecJson[ProgressSummary] =
    casecodec4(ProgressSummary,ProgressSummary.unapply)("level","max","onQuest","notOnQuest")


  implicit val totalQuestPogessEncode:EncodeJson[TotalQuestProgress] =
    EncodeJson(
      (a: TotalQuestProgress) =>
        ("id" := a.quest.id) ->:
          ("title" := a.quest.name) ->:
          ("progress" := a.progressSummary) ->:
          jEmptyObject
    )
}
