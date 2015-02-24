package techex.cases

import java.util.concurrent.{ScheduledExecutorService, Executors}

import doobie.util.process
import org.http4s.dsl._
import org.http4s.server.websocket
import org.http4s.websocket.WebsocketBits.{Text, WebSocketFrame}
import techex._

import scala.concurrent.duration._
import scalaz.concurrent.Task
import scalaz.stream._

object updateStream {

  implicit val executor: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor()

  def wsApi: WebHandler = {
    case req@GET -> Root / "observations" =>
      // Send a Text message with payload 'Ping!' every second
      val src = Process.awakeEvery(10.seconds).map { d => Text("{\"ping\":\"ping\"}")}

      // Print received Text frames, and, on completion, notify the console
      val sink: Sink[Task, WebSocketFrame] = process.sink[Task, WebSocketFrame] {
        case Text(t, last) => Task.delay(println(t))
        case f             => Task.delay(println(s"Unknown type: $f"))
      }.onComplete(Process.eval(Task {println("Terminated!")}).drain)

      // Use the WS helper to make the Task[Response] carrying the info
      // needed for the backend to upgrade to a WebSocket connection
      websocket.WS(src, sink)

  }
}
