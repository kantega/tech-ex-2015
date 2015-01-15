package techex.domain


import scala.util.Random


case class PlayerId(value:String)
object PlayerId{
  def randomId() = PlayerId(Random.alphanumeric.take(6).mkString)
}
case class Nick(value:String)
case class Email(value:String)
case class Player(id:PlayerId,nick:Nick)
