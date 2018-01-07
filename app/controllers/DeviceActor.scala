package actors

import akka.actor._
import play.api.Logger


// {"id":12,"description":"Cuisine d'été","value":0}
// http://localhost:9000/rest/device/12?value=1



object DeviceActor {
  def props(id: Int) = Props(new DeviceActor(id))
}

class DeviceActor(val id: Integer) extends Actor {
  var device = Device(id, "Unknown", -1)
  var connection = context.actorOf(DeadLetterActor.props)
  Logger.info(s"Actor created for device $id")

  def receive = {
    case ConnectDevice(a,b) => {
        Logger.info(s"Actor connected with device $id")

        if (a.value == -1 && device.value != -1) {
          Logger.info(s"Sendind state ${device.value} to device $id")
          b ! DeviceValue(id, device.value)
        }

        device = a
        connection = b
    }
    case v: DeviceValue => {
      Logger.info(s"Sendind state ${v.value} to device $id")
      connection ! v
      device = device.copy(value = v.value)
    }
    case DeviceDescr() => sender ! device
  }
}

object DeadLetterActor {
  def props = Props(new DeadLetterActor())
}

class DeadLetterActor extends Actor {
  def receive = {
    case msg: Any => Logger.warn("Message sent to dead letter : " + msg)
  }
}
