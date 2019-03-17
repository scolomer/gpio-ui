package actors

import akka.actor._
import akka.util._
import scala.concurrent.duration._
import akka.pattern.ask
import play.api.Logging
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global

object UIWsActor {
  def props(out: ActorRef, supervisor: ActorRef) = Props(new UIWsActor(out, supervisor))
}

class UIWsActor(out: ActorRef, supervisor: ActorRef) extends Actor with Logging {
  implicit val timeout = Timeout(2.second)
  val f = supervisor ! RegisterUI()

  def receive = {
    case msg: Message[Any] => {
      logger.debug("UIWsActor: " + msg.toString)
      if (sender == supervisor) {
        logger.debug("Sending out")
        out ! msg
      } else {
        logger.debug("Sending to supervisor")
        supervisor ! msg
      }
    }
  }

  override def postStop = {
    supervisor ! UnRegisterUI()
  }
}
