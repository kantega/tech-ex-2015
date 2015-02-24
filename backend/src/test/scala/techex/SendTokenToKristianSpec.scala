package techex

import argonaut.Parse
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.websocket.{WebSocketTextListener, WebSocket, WebSocketByteListener, WebSocketUpgradeHandler}
import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.data.appleNotifications
import techex.domain.{DeviceToken, Near, Nick}

class SendTokenToKristianSpec extends Specification {


    "The APNS" should {
      "send messages to the phone" in {
        val sendTask =
          appleNotifications.sendNotification(DeviceToken("cd388a5ce0f720a7cdffeb46af3efb3674f2934d27d25a152136f49fa7fa9b48"), "Test")


        val result =
          sendTask.attemptRunFor(5000)

        val msg =
          result.fold(t => t.getMessage,s => "Ok")

        msg ! (result.isRight should_== true)
      }

    }

}
