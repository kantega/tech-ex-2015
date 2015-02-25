package techex.web

import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import javax.servlet.http.HttpServletRequest

import doobie.util.process
import org.eclipse.jetty.websocket.api.{Session, WebSocketListener}
import org.eclipse.jetty.websocket.servlet._
import org.http4s._
import org.http4s.websocket.WebsocketBits.{Binary, Text, WebSocketFrame}
import techex.WSHandler

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.Cause.{End, Error, Kill}
import scalaz.stream._
import scalaz.stream.async.mutable.Topic
import scalaz.stream.io._


class WebsocketServiceServlet(handler: WSHandler) extends WebSocketServlet {

  def configure(factory: WebSocketServletFactory) {
    factory.getPolicy().setIdleTimeout(10000)
    factory.setCreator(new StreamWebsocketCreator(handler))
  }
}

class StreamWebsocketCreator(f: WSHandler,
  asyncTimeout: Duration = Duration.Inf,
  threadPool: ExecutorService = Strategy.DefaultExecutorService) extends WebSocketCreator {


  def createWebSocket(req: ServletUpgradeRequest, resp: ServletUpgradeResponse): AnyRef = {
    resp.setAcceptedSubProtocol("text")
    val requestresult =
      toRequest(req.getHttpServletRequest)

    val wsocket = requestresult
      .fold(
        fail => {println("!!!" + fail.sanitized); null},
        succ => new Http4sWebsocket(f(succ).run))

    wsocket
  }

  def toRequest(req: HttpServletRequest): ParseResult[Request] =
    for {
      method <- Method.fromString(req.getMethod)
      uri <- Uri.requestTarget(req.getRequestURI)
      version <- HttpVersion.fromString(req.getProtocol)
    } yield Request(
      method = method,
      uri = uri,
      httpVersion = version,
      headers = toHeaders(req),
      body = chunkR(req.getInputStream).map(_(4096)).eval,
      attributes = AttributeMap(
        Request.Keys.PathInfoCaret(req.getContextPath.length + req.getServletPath.length),
        Request.Keys.Remote(InetAddress.getByName(req.getRemoteAddr))
      )
    )

  private def toHeaders(req: HttpServletRequest): Headers = {
    val headers = for {
      name <- req.getHeaderNames.asScala
      value <- req.getHeaders(name).asScala
    } yield Header(name, value)
    Headers(headers.toSeq: _*)
  }
}

class Http4sWebsocket(ws: WebSocket) extends WebSocketListener {

  val topic: Topic[WebSocketFrame] =
    async.topic()

  override def onWebSocketBinary(payload: Array[Byte], offset: Int, len: Int): Unit = {
  }

  override def onWebSocketConnect(session: Session): Unit = {
    (ws.source to process.sink(send(session))).onHalt(onHalt(session)).run.runAsync(_.toString)
    (topic.subscribe to ws.sink).run.runAsync(_.toString)
  }

  override def onWebSocketError(cause: Throwable): Unit = {
    topic.fail(cause).run
  }

  override def onWebSocketText(message: String): Unit = {
    topic.publishOne(Text(message)).run
  }

  override def onWebSocketClose(statusCode: Int, reason: String): Unit = {
    topic.close.run
  }


  def send(session: Session): WebSocketFrame => Task[Unit] = {
    case Binary(data, last) => Task {session.getRemote.sendBytes(ByteBuffer.wrap(data))}
    case Text(t, last)      => Task {session.getRemote.sendString(t)}
    case _                  => Task {}
  }

  def onHalt(session: Session): Cause => Process[Task, Unit] = {
    case End      => Process.eval(Task {session.close(1000, "Stream ended")})
    case Kill     => Process.eval(Task {session.close(1011, "Stream killed by upstream source")})
    case Error(e) => Process.eval(Task {session.close(1011, "Source experienced trouble: " + e.getMessage)})
  }
}

case class WebSocket(source: Process[Task, WebSocketFrame], sink: Sink[Task, WebSocketFrame])
