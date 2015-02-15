package techex.cases

import java.util.UUID

import argonaut.Argonaut._
import argonaut.{DecodeJson, EncodeJson}
import org.joda.time.Instant
import techex.domain._

object codecJson {

  implicit val visibilityEncode: EncodeJson[Visibility] =
    jencode1((v: Visibility) => v.getClass.getSimpleName.toLowerCase)

  implicit val questIdEncode: EncodeJson[Qid] =
    jencode1L((id: Qid) => id.value)("id")

  implicit val questEncode: EncodeJson[Quest] =
    jencode3L((q: Quest) => (q.id.value, q.name, q.desc))("id", "name", "desc")


  implicit val badgeEncodeJson: EncodeJson[Badge] =
    jencode3L((b: Badge) => (b.id.value, b.name, b.desc))("id", "name", "desc")

  implicit val achievemntEncodeJson: EncodeJson[Achievement] =
    EncodeJson(
      (a: Achievement) =>
        ("badge" := a.badge) ->:
          ("achieved" := a.achieved) ->:
          ("achievedBy" := a.achievedBy.map(_.value)) ->:
          jEmptyObject
    )

  implicit val progressEncodeJson: EncodeJson[QuestProgress] =
    EncodeJson(
      (progress: QuestProgress) =>
        ("id" := progress.quest.id) ->:
          ("title" := progress.quest.name) ->:
          ("desc" := progress.quest.desc) ->:
          ("visibility" := progress.quest.visibility) ->:
          ("achievements" := progress.achievements) ->:
          jEmptyObject
    )

  implicit val observationDecodeJson: DecodeJson[ObservationData] =
    jdecode2L(
      ( beaconId: String, proximity: String) =>
        ObservationData(Beacon(beaconId), Proximity(proximity)))("beaconId","proximity")
}
