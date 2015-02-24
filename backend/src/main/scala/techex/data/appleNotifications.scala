package techex.data

import com.notnoop.apns.APNS
import techex.domain.DeviceToken

import scalaz.concurrent.Task

object appleNotifications {

  lazy val service =
    APNS.newService()
      .withCert(getClass.getClassLoader.getResourceAsStream("iphone_dev.p12"), "balle")
      .withSandboxDestination()
      .build()


  def sendNotification(token: DeviceToken, msg: String): Task[Unit] =
    Task {
      val payload =
        APNS
          .newPayload()
          .alertBody(msg)
          .build()

      service.push(token.value, payload)

    }
}
