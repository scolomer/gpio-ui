package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.streams.ActorFlow
import actors._
import play.api.libs.json._
import play.api.mvc.WebSocket.MessageFlowTransformer


@Singleton
class HomeController @Inject()(cc: ControllerComponents) (implicit system: ActorSystem, mat: Materializer) extends AbstractController(cc) {

  val deviceSupervisorActor = system.actorOf(DeviceSupervisor.props)

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  implicit val mftDevice = MessageFlowTransformer.jsonMessageFlowTransformer[JsValue, DeviceValue]
  def devices() = WebSocket.accept[JsValue, DeviceValue] { request =>
    ActorFlow.actorRef { out =>
      DeviceWsActor.props(out, deviceSupervisorActor)
    }
  }

  implicit val mftUi = MessageFlowTransformer.jsonMessageFlowTransformer[Message[Any], Message[Any]]
  def ui() = WebSocket.accept[Message[Any], Message[Any]] { request =>
    ActorFlow.actorRef { out =>
      UIWsActor.props(out, deviceSupervisorActor)
    }
  }

  def setValue(id: Int, value: Int) = Action {
    deviceSupervisorActor ! DeviceValue(id, value)
    Ok("")
  }

  /*def setValue(id: Int) = Action { request =>
    Logger.info(request.body.toString)
    Ok(Json.obj("status" ->"OK"))
  }*/
}
