package techex

import argonaut.Parse
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.websocket.{WebSocketTextListener, WebSocket, WebSocketByteListener, WebSocketUpgradeHandler}
import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.domain.{Near, Nick}

class WebsocketSteamSpec  extends Specification {


  try {
    val runningserver =
      server.start.run

    "The webserwer" should {
      "yield a stream of events through the websocket api" in {

        val  c = new AsyncHttpClient()
        val websocket = c.prepareGet("ws://localhost:8080/observations")
          .execute(
            new WebSocketUpgradeHandler.Builder().addWebSocketListener(
              new WebSocketTextListener() {

                def onOpen(ws:WebSocket): Unit = {
                  println("Opened")
                }

                def onClose(ws:WebSocket ) {
                }

                def  onError(t:Throwable): Unit = {
                  t.printStackTrace()
                }

                def onMessage(message:String): Unit = {
                  println(new String(message))
                }

               def  onFragment(fragment:String, last:Boolean) {
                }
              }).build()).get()

        val quests =
          for {
            playerId <- putPlayer(Nick("balle"))
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "51194:16395", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "54803:59488", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "k", Near)
            response <- putObservation(playerId, "58796:18570", Near)
          } yield response

        Thread.sleep(5000)

        quests() must contain("200")
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }



}
