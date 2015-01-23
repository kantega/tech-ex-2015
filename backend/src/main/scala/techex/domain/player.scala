package techex.domain

import scala.util.Random
import scalaz._

case class PlayerId(value: String)
object PlayerId {
  def randomId() = PlayerId(Random.alphanumeric.take(6).mkString)
}
case class Nick(value: String)
case class Email(value: String)
case class Player(id: PlayerId, nick: Nick, preference: PlayerPreference, privateQuests: List[QuestId])
case class QuestId(value: String)

object Player {
  def apply(data: (PlayerId, Nick, Drink, Eat, List[QuestId])): Player =
    Player(data._1, data._2, data._3, data._4, data._5)

  def apply(id: PlayerId, nick: Nick, drink: Drink, eat: Eat, quests: List[QuestId]): Player =
    Player(id, nick, PlayerPreference(drink, eat), quests)
}

trait Preference {
  def asString = getClass.getSimpleName
}

trait Drink extends Preference
case class Wine() extends Drink
case class Beer() extends Drink
case class Coke() extends Drink
object Drink {
  def apply(drinkS: String): Drink = drinkS.toLowerCase match {
    case "wine" => Wine()
    case "beer" => Beer()
    case _ => Coke()
  }
}

trait Eat extends Preference
case class Meat() extends Eat
case class Fish() extends Eat
case class Salad() extends Eat
object Eat {
  def apply(eatS: String): Eat = eatS.toLowerCase match {
    case "meat" => Meat()
    case "fish" => Fish()
    case _ => Salad()
  }
}
case class PlayerPreference(drink: Drink, eat: Eat)