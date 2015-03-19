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
     |<p>The Technoport Experiment 2015 attempts to investigate if gamification can influence people’s behavior.</p>
     |<p>You have been assigned two quests, each consisting of five badges. Badges are awarded based on your presence and movements during the conference. Attempt to collect as many badges as possible and check your standing at the highscore board on the big screen above the Kantega stand.</p>
     |<p>Your location is tracked using iBeacon technology. Location data from your device will only be tracked within the event venue. The data collected will not be used for commercial purposes, but an anonymized data set will be made available for future research and development.</p>
     |<p><strong>Troubleshooting</strong></p>
     |<p>Not receiving badges as you should?</p>
     |<ul>
     |    <li>Make sure Bluetooth is turned on.</li>
     |   <li>Open the application at the point of interest.</li>
     |</ul>
     |<p>The application will attempt to track your location while running in the background. Tracking in the background is not as accurate as when the app is running and can fail to position you precisely in certain circumstances.</p>
     |<p>Problems or questions? Don't hesitate to contact the Kantega stand.</p>
   """.stripMargin

  val androidTxt =
    """
      |<p>The Technoport Experiment 2015 attempts to investigate if gamification can influence people’s behavior.</p>
      |<p>You have been assigned two quests, each consisting of five badges. Badges are awarded based on your presence and movements during the conference. Attempt to collect as many badges as possible and check your standing at the highscore board on the big screen above the Kantega stand.</p>
      |<p>Your location is tracked using iBeacon technology. Location data from your device will only be tracked within the event venue. The data collected will not be used for commercial purposes, but an anonymized data set will be made available for future research and development.</p>
      |<p><strong>Troubleshooting</strong></p>
      |<p>Not receiving badges as you should?</p>
      |<ul>
      |    <li>Make sure Bluetooth is turned on.</li>
      |   <li>Open the application at the point of interest.</li>
      |</ul>
      |<p>The application will attempt to track your location while running in the background. Tracking in the background is not as accurate as when the app is running and can fail to position you precisely in certain circumstances.</p>
      |<p>Problems or questions? Don't hesitate to contact the Kantega stand.</p>
    """.stripMargin

  def restApi: WebHandler = {
    case req@GET -> Root / "text" / "help" / "ios" =>
      Ok(iosTxt).withHeaders(`Content-Type`(`text/html`))
    case req@GET -> Root / "text" / "help" / "android" =>
      Ok(androidTxt).withHeaders(`Content-Type`(`text/html`))
  }
}
