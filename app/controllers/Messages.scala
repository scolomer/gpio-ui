package actors

import play.api.Logging
import play.api.libs.json._
import akka.actor._


object Device {
  implicit val deviceFormat = Json.format[Device]
}
case class Device(id: Int, description: String, value: Int)

case class ConnectDevice(device: Device, connection: ActorRef)

case class RegisterUI()
case class UnRegisterUI()

case class DeviceDescr()

case class Ping()
case class Pong()

object DeviceValue {
  implicit val deviceValue = Json.format[DeviceValue]
}
case class DeviceValue(id: Int, value: Int)

case class Message[+A](id: String, payload: A)
object Message extends Logging  {
  implicit val write = new Writes[Message[Any]] {
    def writes(msg: Message[Any]) = {
      logger.debug(s"Write : $msg")
      Json.obj(
        "id" -> msg.id,
        "payload" -> (msg.payload match {
          case a: Seq[Device] => Json.toJson(a)
          case a: Device => Json.toJson(a)
          case a: DeviceValue => Json.toJson(a)
          case _ => throw new Exception(s"Unsupported type : $msg.payload")
        }))
    }
  }

  implicit val read = new Reads[Message[Any]] {
    def reads(json: JsValue): JsResult[Message[Any]] = {
      logger.debug(s"Read : $json")
      JsSuccess((json \ "id").as[String] match {
        case "value" => Message("value", DeviceValue((json \ "payload" \ "id").as[Int], (json \ "payload" \ "value").as[Int]))
        case "ping" => Message("ping", Ping())
      })
    }
  }
}
