package actors

import akka.actor._
import play.api.Logger
import play.api.libs.json._

// {"id":12,"description":"Cuisine d'été","value":0}
// http://localhost:9000/rest/device/12?value=1

object Device {
  implicit val deviceFormat = Json.format[Device]
}

case class Device(id: Int, description: String, value: Int)

case class ConnectDevice(device: Device, connection: ActorRef)

object DeviceValue {
  implicit val deviceValue = Json.format[DeviceValue]
}
case class DeviceValue(id: Int, value: Int)

object DeviceWsActor {
  def props(out: ActorRef, supervisor: ActorRef) = Props(new DeviceWsActor(out, supervisor))
}

class DeviceWsActor(out: ActorRef, supervisor: ActorRef) extends Actor {
  def receive = {
    case msg: JsValue => {
      val msg2 = Json.fromJson[Device](msg)
      msg2 match {
        case s: JsSuccess[Device] => supervisor ! ConnectDevice(s.get, out)
        case e: JsError => Logger.error("Bad formatted message : " + msg)
      }
    }
  }
}

object DeviceSupervisor {
  def props = Props(new DeviceSupervisor())
}

class DeviceSupervisor extends Actor {
  val devices = collection.mutable.Map[Int, ActorRef]()

  def receive = {
    case m: ConnectDevice => {
      devices.get(m.device.id) match {
        case Some(a) => a ! m
        case None => {
          val a = context.actorOf(DeviceActor.props(m.device.id))
          devices += (m.device.id -> a)
          a ! m
        }
      }
    }
    case m: DeviceValue => {
      devices.get(m.id).foreach { a =>
        a ! m
      }
    }
    case _ => Logger.info("Unknown message")
  }
}

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
          b ! Json.toJson(DeviceValue(id, device.value))
        }

        device = a
        connection = b
    }
    case v: DeviceValue => {
      Logger.info(s"Sendind state ${v.value} to device $id")
      connection ! Json.toJson(v)
    }
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
