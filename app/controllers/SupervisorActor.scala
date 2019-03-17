package actors

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import play.api.Logging
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import collection.mutable.{ HashMap, MultiMap, Set, Map, ListBuffer }

object DeviceSupervisor {
  def props = Props(new DeviceSupervisor())
}

class DeviceSupervisor extends Actor with Logging {

  implicit val timeout = Timeout(2.seconds)

  val devices = Map[Int, ActorRef]()
  val devicesWs = new HashMap[ActorRef, Set[ActorRef]] with MultiMap[ActorRef, ActorRef]
  val uis = ListBuffer.empty[ActorRef]

  def receive = {
    case m: ConnectDevice => {


      devices.get(m.device.id) match {
        case Some(a) => {
          a ! m
          uis.foreach { _ ! Message("update", m.device) }
        }
        case None => {
          context.watch(sender)
          val a = context.actorOf(DeviceActor.props(m.device.id))
          devicesWs.addBinding(sender, a)
          devices += (m.device.id -> a)
          a ! m
          uis.foreach {_ ! Message("add", m.device) }
        }
      }
    }
    case m: DeviceValue => {
      logger.debug(s"DeviceValue $m")
      devices.get(m.id) match {
        case Some(a) => {
          a ! m
          uis.filter(_ != sender).foreach {_ ! Message("update", m)}
        }
        case None => {
          logger.warn(s"Device $m.device.id not found")
        }
      }
    }
    case RegisterUI() => {
      val s = sender
      val f = Future.sequence(devices.values.map(a => a ask DeviceDescr()))

      f onComplete {
        case Success(a) => {
          logger.debug(a.toString)
          s ! Message("init", a)
          uis += s
          logger.debug(s"uis : $uis")
        }
        case Failure(t) => logger.error(t.getMessage, t)
      }

    }

    case UnRegisterUI() => {
      uis -= sender
      logger.debug(s"uis : $uis")
    }

    case m: Message[Any] => {
      logger.debug(s"Message : $m")
      self ! m.payload
    }

    case Ping() => {}

    case t: Terminated => {
      devicesWs.get(t.actor).getOrElse(Set()).foreach(a => a ! PoisonPill)
    }

    case a: Any => logger.warn(s"Unknown message : $a")
  }
}
