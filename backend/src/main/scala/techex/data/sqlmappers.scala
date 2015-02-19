package techex.data

import java.util.UUID

import doobie.imports._
import org.joda.time.Instant
import techex._
import techex.domain._

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

object PlayerDAO {

  import sqlmappers._



  def insertPlayer(p: Player): ConnectionIO[Int] = {
    val drink = p.preference.drink
    val eat = p.preference.eat
    sql"""
          INSERT INTO players
          VALUES (${p.id.value}, ${p.nick.value}, $drink, $eat)
    """.update.run
  }

  def getPlayerById(id: PlayerId): ConnectionIO[Option[Player]] = {
    val value = id.value
    sql"""
        SELECT * FROM players WHERE id = $value
      """.query[(PlayerId, Nick, Drink, Eat, List[QuestId])].map(Player(_)).list.map(_.headOption)
  }

  def getPlayerByNick(id: Nick): ConnectionIO[Option[Player]] = {
    sql"""
        SELECT * FROM players WHERE nick = ${id.value}
      """.query[(PlayerId, Nick, Drink, Eat, List[QuestId])].map(Player(_)).list.map(_.headOption)
  }

  def getPlayers: ConnectionIO[List[Player]] = {
    sql"""
          SELECT * FROM players
       """.query[(PlayerId, Nick, Drink, Eat, List[QuestId])].map(Player(_)).list
  }

  def create: ConnectionIO[Int] =
    sql"""
         CREATE TABLE players (
           id VARCHAR(255) NOT NULL,
           nick VARCHAR(255) NOT NULL,
           drink VARCHAR(100) NOT NULL,
           eat VARCHAR(100) NOT NULL,
           PRIMARY KEY (id)
         );
    """.update.run
}


object ObservationDAO {

  import sqlmappers._

  def createObservationtable: ConnectionIO[Int] = {
    sql"""
          CREATE TABLE observations (
            id BIGINT AUTO_INCREMENT NOT NULL,
            beaconId VARCHAR(200) NOT NULL ,
            playerId VARCHAR(200) NOT NULL,
            instant BIGINT NOT NULL,
            PRIMARY KEY (id)
          );
          """.update.run
  }

  def storeObservation(observation: Observation): ConnectionIO[Int] = {
    sql"""
          INSERT INTO observations
          VALUES (${observation.beacon},${observation.playerId},${observation.instant}  )
    """.update.run
  }

  def loadObservationForPlayer(playerId: PlayerId, max: Int): ConnectionIO[List[Observation]] = {
    sql"""
          SELECT * FROM PLAYERS WHERE playerId = ${playerId} ORDER BY instant LIMIT $max
    """.query[Observation].list
  }
}

object LocationDao {

  import sqlmappers._

  def createLocationTable: ConnectionIO[Int] = {
    sql"""
          CREATE TABLE movements(
            id  BIGINT AUTO_INCREMENT NOT NULL,
            playerId VARCHAR(200) NOT NULL,
            direction VARCHAR(200) NOT NULL,
            locationId VARCHAR(200) NOT NULL,
            instant BIGINT NOT NULL,
            PRIMARY KEY (id)
          );
    """.update.run
  }

  def loadLocationsForPlayer(playerId: PlayerId, max: Int): ConnectionIO[List[LocationUpdate]] = {
    sql"""
          SELECT * FROM movement WHERE playerId = $playerId ORDER BY instant LIMIT $max
    """.query[LocationUpdate].list
  }

  def storeLocation(movement: LocationUpdate): ConnectionIO[Int] = {
    sql"""
          INSERT INTO movement
          VALUES (${movement.playerId},${movement.area},${movement.instant});
    """.update.run
  }
}
