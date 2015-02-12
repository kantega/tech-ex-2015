package techex.cases

import java.util.concurrent.Executors

import dispatch.{Http, host}

import scala.concurrent.ExecutionContext
import scalaz.\/-
import scalaz.concurrent.Task
import argonaut._, Argonaut._

object notifyAdmin {

  implicit val executor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  lazy val h =
    host("https://hooks.slack.com/services/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").POST


  def sendMessage(txt: String): Task[Unit] = {

    val json: Json =
      Json(
        "channel" -> jString("#technoport"),
        "username" -> jString("TechEx2015 Server"),
        "text" -> jString(txt),
        "icon_emoji" -> jString(":ghost:")
      )

    Task.async(register => {
      Http(h.setBody("payload=" + json.nospaces)).onComplete(resp => register(\/-(Unit)))

    })
  }

}
