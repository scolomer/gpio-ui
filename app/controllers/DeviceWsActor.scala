package actors

import play.api.Logger
import akka.actor._
import play.api.libs.json._ // TODO remove this dependency to JSON


object DeviceWsActor {
  def props(out: ActorRef, supervisor: ActorRef) = Props(new DeviceWsActor(out, supervisor))
}

class DeviceWsActor(out: ActorRef, supervisor: ActorRef) extends Actor {
  def receive = {
    case msg: JsObject => {
      if (!msg.keys.isEmpty) {
        Json.fromJson[Device](msg) match {
          case s: JsSuccess[Device] => supervisor ! ConnectDevice(s.get, out)
          case e: JsError => Logger.error("Bad formatted message : " + msg)
        }
      } else {
        Logger.debug("Receive heartbeat")
      }
    }
  }
}
