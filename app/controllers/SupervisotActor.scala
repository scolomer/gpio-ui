package actors

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import play.api.Logger
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout


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
