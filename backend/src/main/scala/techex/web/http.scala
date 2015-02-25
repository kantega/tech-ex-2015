package techex.web

import org.http4s.MediaType._
import org.http4s._
import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.http4s.server._
import org.http4s.server.websocket._
import org.http4s.websocket.WebsocketBits.{Text, WebSocketFrame}
import techex.WebHandler
import techex.cases.playerSignup

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scalaz.concurrent.Strategy.DefaultTimeoutScheduler
import scalaz.concurrent.Task
import scalaz.stream.Process

object test {

  implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  implicit def defaultSecheduler =
    DefaultTimeoutScheduler

  val testApi: WebHandler = {
    // Wire your data into your service
    case req@GET -> Root / "streaming" =>
      Ok(dataStream(10))

    // You can use helpers to send any type of data with an available EntityEncoder[T]
    case GET -> Root / "synchronous" =>
      Ok("This is good to go right now.")


    case GET -> Root / "ping" =>
      // EntityEncoder allows for easy conversion of types to a response body
      Ok("pong")

    case GET -> Root / "future" =>
      // EntityEncoder allows rendering asynchronous results as well
      Ok(Future("Hello from the future!"))

    case req@GET -> Root / "ip" =>
      // Its possible to define an EntityEncoder anywhere so you're not limited to built in types
      val json = jSingleObject("origin", jString(req.remoteAddr.getOrElse("unknown")))
      Ok(json)

    case req@GET -> Root / "redirect" =>
      // Not every response must be Ok using a EntityEncoder: some have meaning only for specific types
      TemporaryRedirect(uri("/http4s"))

    case GET -> Root / "content-change" =>
      // EntityEncoder typically deals with appropriate headers, but they can be overridden
      Ok("<h2>This will have an html content type!</h2>")
      .withHeaders(`Content-Type`(`text/html`))
  }


  // This is a mock data source, but could be a Process representing results from a database
  def dataStream(n: Int): Process[Task, String] = {

    val interval = 100.millis
    val stream: Process[Task, String] = Process.awakeEvery(interval)
                                        .map(_ => s"Current system time: ${System.currentTimeMillis()} ms\n")
                                        .take(n)

    val start: Process[Task, String] = Process.emit(s"Starting $interval stream intervals, taking $n results\n\n")

    start ++ stream
  }

}





