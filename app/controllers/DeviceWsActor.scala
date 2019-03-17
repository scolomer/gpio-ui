package actors

import play.api.Logging
import akka.actor._
import play.api.libs.json._ // TODO remove this dependency to JSON


object DeviceWsActor {
  def props(out: ActorRef, supervisor: ActorRef) = Props(new DeviceWsActor(out, supervisor))
}

class DeviceWsActor(out: ActorRef, supervisor: ActorRef) extends Actor with Logging {
  def receive = {
    case msg: JsObject => {
      if (!msg.keys.isEmpty) {
        Json.fromJson[Device](msg) match {
          case s: JsSuccess[Device] => supervisor ! ConnectDevice(s.get, out)
          case e: JsError => logger.error("Bad formatted message : " + msg)
        }
      } else {
        logger.debug("Receive heartbeat")
      }
    }
  }
}
