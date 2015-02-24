package techex.data

import java.net.InetAddress
import java.util.concurrent.Executors

import argonaut.Argonaut._
import argonaut.Json
import dispatch.{Http, url}
import doobie.util.process
import techex.domain.{Info, AttentionLevel}

import scala.concurrent.ExecutionContext
import scalaz.\/-
import scalaz.concurrent.Task
import scalaz.stream._

object slack {
  val executor =
    techex.namedSingleThreadExecutor("Slacknotifier")

  implicit val execContext =
    ExecutionContext.fromExecutor(executor)

  lazy val h =
    url("https://hooks.slack.com/services/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").POST

  lazy val hostname =
    InetAddress.getLocalHost().getHostName()

  lazy val slack: Sink[Task, String] =
    process.sink((msg: String) => sendMessage(msg))

  def sendMessage(txt: String, color: AttentionLevel = Info): Task[Unit] = {


    val attachments: Json =
      Json.array(Json(
        "fallback" -> jString(txt),
        "color" -> jString(color.asColor),
        "text" -> jString(txt),
        "mrkdwn_in" -> jArray(List(jString("text"), jString("pretext")))
      ))


    val json: Json =
      Json(
        "channel" -> jString("#technoport"),
        "username" -> jString("TechEx2015 Server at " + hostname),
        "icon_emoji" -> jString(":metal:"),
        "attachments" -> attachments
      )

    Task.async(register => {
      Http(h.setBody(json.nospaces)).onComplete(resp => {
        register(\/-(()))
      })
    })
  }
}
