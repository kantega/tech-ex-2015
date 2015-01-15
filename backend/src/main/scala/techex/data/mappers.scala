package techex.data

import doobie.imports._
import techex.domain.{Nick, Player, PlayerId}

import scalaz.stream.Process

object mappers {}

object PlayerDAO {

  implicit val playerIdAtom:Atom[PlayerId] =
    Atom.fromScalaType[String].xmap(PlayerId(_),_.value)


  def insertPlayer(p: Player): ConnectionIO[Int] = {
    sql"""
          INSERT INTO players
          VALUES (${p.id.value}, ${p.nick.value})
    """.update.run
  }

  def getPlayerById(id: PlayerId): ConnectionIO[List[Player]] = {
    val value = id.value
    sql"""
        SELECT * FROM players WHERE id = $value
      """.query[Player].list
  }

  def getPlayerByNick(id: Nick): ConnectionIO[List[Player]] = {
    val value = id.value
    sql"""
        SELECT * FROM players WHERE nick = $value
      """.query[Player].list
  }

  def getPlayers: Process[ConnectionIO, Player] = {
    sql"""
          SELECT * FROM players
       """.query[Player].process
  }

  def create: ConnectionIO[Int] =
    sql"""
         CREATE TABLE players (
           id VARCHAR(255) NOT NULL PRIMARY KEY,
           nick VARCHAR(255) NOT NULL
         );
    """.update.run
}
