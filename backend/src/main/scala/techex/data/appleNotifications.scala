package techex.data

import com.notnoop.apns.APNS
import techex.domain.DeviceToken

import scalaz.concurrent.Task

object appleNotifications {

  lazy val prodService =
    APNS.newService()
      .withCert(getClass.getClassLoader.getResourceAsStream("iphone_prod.p12"), "balle")
      .withProductionDestination()
      .build()

  lazy val devService =
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

      prodService.push(token.value, payload)
      devService.push(token.value, payload)

    }
}
