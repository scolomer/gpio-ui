package actors

import akka.actor._
import play.api.Logging


// {"id":12,"description":"Cuisine d'été","value":0}
// http://localhost:9000/rest/device/12?value=1



object DeviceActor {
  def props(id: Int) = Props(new DeviceActor(id))
}

class DeviceActor(val id: Integer) extends Actor with Logging {
  var device = Device(id, "Unknown", -1)
  var connection = context.actorOf(DeadLetterActor.props)
  logger.info(s"Actor created for device $id")

  def receive = {
    case ConnectDevice(a,b) => {
        logger.info(s"Device $id connected")

        if (a.value == -1 && device.value != -1) {
          logger.info(s"Sendind state ${device.value} to device $id")
          b ! DeviceValue(id, device.value)
        }

        device = a
        connection = b
    }
    case v: DeviceValue => {
      logger.info(s"Sendind state ${v.value} to device $id")
      connection ! v
      device = device.copy(value = v.value)
    }
    case DeviceDescr() => sender ! device
  }

  override def postStop { 
    logger.info(s"Device $id disconnected")
  }

}

object DeadLetterActor {
  def props = Props(new DeadLetterActor())
}

class DeadLetterActor extends Actor with Logging {
  def receive = {
    case msg: Any => logger.warn("Message sent to dead letter : " + msg)
  }
}
