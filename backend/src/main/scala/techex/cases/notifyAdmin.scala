package techex.cases

import java.net.InetAddress
import java.util.concurrent.Executors

import dispatch.{url, Http, host}

import scala.concurrent.ExecutionContext
import scalaz.\/-
import scalaz.concurrent.Task
import argonaut._, Argonaut._

object notifyAdmin {

  implicit val executor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  lazy val h =
    url("https://hooks.slack.com/services/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").POST

  lazy val hostname =
    InetAddress.getLocalHost().getHostName()

  def sendMessage(txt: String): Task[Unit] = {

    val json: Json =
      Json(
        "channel" -> jString("#technoport"),
        "username" -> jString("TechEx2015 Server at " + hostname),
        "text" -> jString(txt),
        "icon_emoji" -> jString(":metal:")
      )

    Task.async(register => {
      println("Calling slack")
      Http(h.setBody(json.nospaces)).onComplete(resp => {
        println("result from call: " + resp.get.getStatusCode + " " + resp.get.getResponseBody)
        register(\/-(Unit))
      })

    })
  }

}
