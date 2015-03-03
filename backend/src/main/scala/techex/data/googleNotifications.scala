package techex.data

import argonaut.Argonaut._
import argonaut.Json
import com.ning.http.client.Response
import dispatch.{Http, url}
import techex.domain.DeviceToken

import scala.concurrent.ExecutionContext
import scala.util.Try
import scalaz.{\/, -\/, \/-}
import scalaz.concurrent.Task

object googleNotifications {

  val executor =
    techex.namedSingleThreadExecutor("GCM notifier")

  implicit val execContext =
    ExecutionContext.fromExecutor(executor)

  lazy val h =
    url("https://android.googleapis.com/gcm/send").POST

  val key =
    scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("googleapikey.txt")).mkString

  def sendMessage(token: DeviceToken, txt: String): Task[Unit] = {


    val json: Json =
      Json(
        "registration_ids" -> Json.array(jString(token.value)),
        "data" -> Json("message" -> jString(txt))
      )

    Task.async(register => {
      Http(
        h
          .setBody(json.nospaces)
          .setHeader("Authorization", "key=" + key)
          .setContentType("application/json", "UTF-8"))
        .onComplete(
          tr => {
            val v = tr.transform[Throwable \/ String]((resp: Response) => Try(\/-(resp.getResponseBody)), (fail: Throwable) => Try(-\/(fail)))
            println(v.get)
            register(v.get.map(x => x.toString))
          })

    })
  }
}
