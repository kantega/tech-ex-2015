package techex.cases

import org.http4s._
import org.http4s.dsl._
import techex.WebHandler

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._

import techex.domain.{Area, areas}
import scalaz._, Scalaz._
import scalaz.concurrent.Task

object getAreas {

  case class AreaNode(name: String, children: List[AreaNode])

  implicit def codecAreas: CodecJson[AreaNode] =
    casecodec2(AreaNode, AreaNode.unapply)("name", "children")


  def getResponseFor(area: Tree[Area]): Task[Response] = {
    val tree =
      getAreaTree(area)

    Ok(tree.asJson)
  }

  def getAreaTree(node: Tree[Area]): Json = {
    Json("name" -> jString(node.rootLabel.name), "children" -> jArray(node.subForest.map(getAreaTree).toList))
  }


  def restApi: WebHandler = {
    case GET -> Root / "areas" / location =>
      (location match {
        case "kantega" => getResponseFor(areas.kantegaLocationTree)
        case _         => getResponseFor(areas.technoportLocationTree)
      }).withHeaders(Header("Access-Control-Allow-Origin","*"))

  }

}
