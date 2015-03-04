package techex

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.websocket.{WebSocket, WebSocketTextListener, WebSocketUpgradeHandler}
import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.domain.areas._
import techex.domain.{Near, Nick}

class WebsocketStreamSpec  extends Specification {


  try {
    val runningserver =
      server.start.run

    "The webserwer" should {
      "yield a stream of events through the websocket api" in {

        val  c = new AsyncHttpClient()
        val websocket = c.prepareGet("ws://localhost:8080/ws/observations")
          .execute(
            new WebSocketUpgradeHandler.Builder().setProtocol("text").addWebSocketListener(
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
            _ <- putObservation(playerId, beaconAt(kantegaCoffeeDn), Near)
            _ <- putObservation(playerId, beaconAt(kantegaCoffeeUp), Near)
            _ <- putObservation(playerId, beaconAt(mrtTesla), Near)
            _ <- putObservation(playerId, beaconAt(mrtTuring), Near)
            _ <- putObservation(playerId, beaconAt(mrtEngelbart), Near)
            _ <- putObservation(playerId, beaconAt(mrtAda), Near)
            _ <- putObservation(playerId, beaconAt(kantegaCoffeeDn), Near)
            _ <- putObservation(playerId, beaconAt(kantegaCoffeeUp), Near)
            _ <- putObservation(playerId, beaconAt(kantegaCoffeeDn), Near)
            response <- putObservation(playerId, beaconAt(kantegaCoffeeUp), Near)
          } yield response

        Thread.sleep(5000)

        quests().getStatusCode must_== 200
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }



}
