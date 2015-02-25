package techex.cases

import java.util.concurrent.{Executors, ScheduledExecutorService}

import argonaut.Argonaut._
import argonaut._
import doobie.util.process
import org.http4s.dsl._
import org.http4s.websocket.WebsocketBits.{Text, WebSocketFrame}
import techex._
import techex.data.codecJson
import techex.domain.Fact
import techex.web.WebSocket

import scalaz.concurrent.Task
import scalaz.stream._
import scalaz.stream.async.mutable.Topic
import codecJson._
object updateStream {

  implicit val executor: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor()

  def wsApi(factUdpates: Topic[Fact]): WSHandler = {
    case req@GET -> Root / "ws" / "observations" =>

      // Print received Text frames, and, on completion, notify the console
      val sink: Sink[Task, WebSocketFrame] = process.sink[Task, WebSocketFrame] {
        case Text(t, last) => Task.delay(println(t))
        case f             => Task.delay(println(s"Unknown type: $f"))
      }.onComplete(Process.eval(Task {println("Terminated!")}).drain)


      val src = factUdpates.subscribe.map(fact => Text(fact.asJson.spaces4))

      Task{WebSocket(src,sink)}

  }
}
