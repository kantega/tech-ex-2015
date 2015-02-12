package techex.cases

import argonaut.Argonaut._
import argonaut.EncodeJson
import techex.domain._

object codecJson {

  implicit val questIdEncode: EncodeJson[Qid] =
    jencode1L((id:Qid)=>id.value)("id")

  implicit val questEncode: EncodeJson[Quest] =
    jencode3L((q: Quest) => (q.id, q.name, q.desc))("id", "name", "desc")


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
        ("quest" := progress.quest) ->:
          ("achievements" := progress.achievements) ->:
          jEmptyObject
    )
}
