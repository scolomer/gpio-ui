package actors

import akka.actor._
import akka.pattern.ask
import play.api.Logger
import play.api.libs.json._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}


// {"id":12,"description":"Cuisine d'été","value":0}
// http://localhost:9000/rest/device/12?value=1

object Device {
  implicit val deviceFormat = Json.format[Device]
}
case class Device(id: Int, description: String, value: Int)

case class ConnectDevice(device: Device, connection: ActorRef)

case class RegisterUI()

case class DeviceDescr()

case class Ping()
case class Pong()

object DeviceValue {
  implicit val deviceValue = Json.format[DeviceValue]
}
case class DeviceValue(id: Int, value: Int)

case class Message[+A](id: String, payload: A)
object Message {
  implicit val write = new Writes[Message[Any]] {
    def writes(msg: Message[Any]) = {
      Logger.debug(s"Write : $msg")
      Json.obj(
        "id" -> msg.id,
        "payload" -> (msg.payload match {
          case a: Seq[Device] => Json.toJson(a)
          case b: Device => Json.toJson(b)
          case _ => throw new Exception("Unsupported type")
        }))
    }
  }

  implicit val read = new Reads[Message[Any]] {
    def reads(json: JsValue): JsResult[Message[Any]] = {
      Logger.debug(s"Read : $json")
      JsSuccess((json \ "id").as[String] match {
        case "value" => Message("value", DeviceValue((json \ "payload" \ "id").as[Int], (json \ "payload" \ "value").as[Int]))
        case "ping" => Message("ping", Ping())
      })
    }
  }
}

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

object DeviceSupervisor {
  def props = Props(new DeviceSupervisor())
}

class DeviceSupervisor extends Actor {

  implicit val timeout = Timeout(2.seconds)

  val devices = collection.mutable.Map[Int, ActorRef]()
  var ui = context.actorOf(DeadLetterActor.props)

  def receive = {
    case m: ConnectDevice => {
      devices.get(m.device.id) match {
        case Some(a) => {
          a ! m
          ui ! Message("update", m.device)
        }
        case None => {
          val a = context.actorOf(DeviceActor.props(m.device.id))
          devices += (m.device.id -> a)
          a ! m
          ui ! Message("add", m.device)
        }
      }
    }
    case m: DeviceValue => {
      Logger.debug(s"DeviceValue $m")
      devices.get(m.id) match {
        case Some(a) => {
          a ! m
        //  ui ! Message("update", m.device)
        }
        case None => {
          Logger.warn(s"Device $m.device.id not found")
        }
      }
    }
    case m: RegisterUI => {
      ui = sender
      val s = sender
      val f = Future.sequence(devices.values.map(a => a ask DeviceDescr()))

      f onComplete {
        case Success(a) => {
          Logger.debug(a.toString)
          s ! Message("init", a)
        }
        case Failure(t) => Logger.error(t.getMessage, t)
      }

    }
    case m: Message[Any] => {
      Logger.debug(s"Message : $m")
      self ! m.payload
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
