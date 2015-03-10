package techex

import argonaut.Argonaut._
import argonaut.CodecJson
import techex.domain.Area

import scalaz.Tree

object TestArgonaut extends App {

  case class AreaNode(name: String, children: List[AreaNode])

  implicit def codecAreas: CodecJson[AreaNode] =
    casecodec2(AreaNode, AreaNode.unapply)("name", "children")


  val tree =
    AreaNode("a", List(AreaNode("a.1", List()), AreaNode("a.2", List())))
  //getAreaTree(area)

  println(tree.asJson.spaces4)

}