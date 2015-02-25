package techex.cases

import org.http4s.MediaType._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import techex._
import techex.data.InputMessage

import scalaz.stream.async.mutable.Topic

object serveHelptext {

  val iosTxt =
   """
     |<dv><em>Help</em> text for iOS is <i>cool</i></div>
   """.stripMargin

  val androidTxt =
    """
      |<dv><em>Help</em> text for Android is <i>cool</i></div>
    """.stripMargin

  def restApi: WebHandler = {
    case req@POST -> Root / "text" / "help" / "ios" =>
      Ok(iosTxt).withHeaders(`Content-Type`(`text/html`))
    case req@POST -> Root / "text" / "help" / "android" =>
      Ok(iosTxt).withHeaders(`Content-Type`(`text/html`))
  }
}
