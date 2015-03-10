package techex

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}

import scala.collection.JavaConversions._

object CfgTest extends App{

  val cfgMap =
    Map("db.type" -> "mysql", "db.username" -> "jalla", "db.password" -> "balla")

  val cfg = ConfigValueFactory.fromMap(cfgMap).toConfig.withFallback(ConfigFactory.load())

  println(cfg.toString)
}
