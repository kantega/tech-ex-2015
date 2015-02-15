package techex.data

import java.net.InetAddress
import java.util.concurrent.Executors

import argonaut.Argonaut._
import argonaut.Json
import dispatch.{Http, url}
import doobie.util.process

import scala.concurrent.ExecutionContext
import scalaz.\/-
import scalaz.concurrent.Task
import scalaz.stream._
object slack {
  implicit val executor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  lazy val h =
    url("https://hooks.slack.com/services/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").POST

  lazy val hostname =
    InetAddress.getLocalHost().getHostName()

  lazy val slack: Sink[Task, String] =
    process.sink((msg: String) => sendMessage(msg))

  def sendMessage(txt: String, color: String = "#439FE0"): Task[Unit] = {


    val attachments: Json =
      Json.array(Json(
        "fallback" -> jString(txt),
        "color" -> jString(color),
        "text" -> jString(txt)
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
        println("result from call: " + resp.get.getStatusCode + " " + resp.get.getResponseBody)
        register(\/-(Unit))
      })

    })
  }
}
