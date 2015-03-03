package techex.cases

import techex.WebHandler

import argonaut.Argonaut._
import argonaut.CodecJson
import org.http4s.argonaut._
import org.http4s.dsl._
import techex.domain.areas

object getBeaconRegions {

  val uuid = "DA5336AE-2042-453A-A57F-F80DD34DFCD9"

  case class BeaconRegion(identifier: String, uuid: String, major: Int)

  implicit val codecbeaconReagion: CodecJson[BeaconRegion] =
    casecodec3(BeaconRegion, BeaconRegion.unapply)("identifier", "uui", "major")


  def restApi: WebHandler = {
    case req@GET -> Root / "beaconregions" =>
      val regions =
        areas.regionList.map(region => BeaconRegion(region.name, uuid, region.id))
      Ok(regions.asJson)

  }

}


