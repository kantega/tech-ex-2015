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

  implicit val instantAtom: Atom[Instant] =
    Atom.fromScalaType[Long].xmap(new Instant(_), _.getMillis)

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
            recordedtime varchar(255) not null,
            PRIMARY KEY (id)
          );
          """.update.run
  }

  def storeObservation(input: InputMessage): ConnectionIO[Unit] = {
    sql"""
          INSERT INTO observations (instant,type,payload,recordedtime)
          VALUES (${input.instant.getMillis},${input.msgType},${input.asJson.nospaces},${input.instant.toString} )
    """.update.run.map(x=>Unit)
  }

}


