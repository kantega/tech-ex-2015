package techex.data

import java.util.UUID

import doobie.hi
import doobie.imports._
import org.joda.time.Instant
import techex.domain._
import argonaut._
import Argonaut._

import scalaz.stream.Process

object sqlmappers {

  implicit val uuidAtom: Atom[UUID] =
    Atom.fromScalaType[String].xmap(UUID.fromString, _.toString)

  implicit val beaconAtom: Atom[Beacon] =
    Atom.fromScalaType[String].xmap(Beacon, _.id)

  implicit val playerIdAtom: Atom[PlayerId] =
    Atom.fromScalaType[String].xmap(PlayerId(_), _.value)

  implicit val eatAtom: Atom[Eat] =
    Atom.fromScalaType[String].xmap[Eat](Eat(_), _.asString)

  implicit val drinkAtom: Atom[Drink] =
    Atom.fromScalaType[String].xmap[Drink](Drink(_), _.asString)

  implicit val questListAtom: Atom[List[QuestId]] =
    Atom.fromScalaType[String].xmap(str => str.split(",").toList.map(QuestId), ids => ids.map(_.value).mkString(","))

  implicit val instantAtom: Atom[Instant] =
    Atom.fromScalaType[Long].xmap(new Instant(_), _.getMillis)

  implicit val locationAtom: Atom[Area] =
    Atom.fromScalaType[String].xmap(Area.apply, _.id)

  implicit val proximityAtom: Atom[Proximity] =
    Atom.fromScalaType[String].xmap(str => str.toLowerCase match {
      case "near"      => Near
      case "far"       => Far
      case "immediate" => Immediate
    }, {
      case Near      => "near"
      case Far       => "far"
      case Immediate => "immediate"
    })

}


object InputMessageDAO {

  import sqlmappers._
  import codecJson._


  //val storeInputProcess =
  //  Process.re(InputMessageDAO.storeObservation))

  def createObservationtable: ConnectionIO[Int] = {
    sql"""
          CREATE TABLE IF NOT EXISTS observations (
            id BIGINT AUTO_INCREMENT NOT NULL,
            instant BIGINT NOT NULL,
            type VARCHAR(200) NOT NULL,
            payload TEXT NOT NULL,
            PRIMARY KEY (id)
          );
          """.update.run
  }

  def storeObservation(input: InputMessage): ConnectionIO[Int] = {
    sql"""
          INSERT INTO observations
          VALUES (${Instant.now().getMillis},${input.msgType},${input.asJson.nospaces}  )
    """.update.run
  }

  def loadObservationForPlayer(playerId: PlayerId): Process[ConnectionIO, (Long, Long, String, String)] = {
    sql"""
          SELECT * FROM PLAYERS WHERE playerId = ${playerId} ORDER BY instant
    """.query[(Long, Long, String, String)].process
  }
}


