package jobs

import akka.actor.Actor
import akka.event.Logging
import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString

object FetchActorWorker {

  case object Running

  case object Initialize

  def props(tag: String): Props = Props(new FetchActorWorker(tag))
}

class FetchActorWorker(tag: String) extends Actor {

  import akka.pattern.pipe
  import context.dispatcher

  val log = Logging(context.system, this)
  val http = Http(context.system)

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val httpClient = Http(context.system)

  override def postStop():Unit = {
    log.info(s"Worker processing $tag stopped")
  }

  def receive = {
    case FetchActorWorker.Initialize => {
      log.info("initialize with " + tag)
      sender() ! FetchActorWorker.Running
      httpClient.singleRequest(HttpRequest(uri = "http://akka.io")).pipeTo(self)
    }

    /*
     * Piped success response
     */
    case HttpResponse(StatusCodes.OK, headers, entity, _) => {
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        log.info("Got response")
        val max = 10
        for (i <- 1 to max) {
          context.parent ! new FetchActorMaster.StatusUpdate(tag, i / max * 100)
        }
      }
    }
    /*
     * Piped non-success response
     */
    case resp@HttpResponse(code, _, _, _) => {
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
    }
  }

  // Twitter authentication
  // def authenticate(consumerKey: String, consumerSecret: String): ???

  // Twitter data fetch for a specific tag
  //def fetch(tag: String): ???
}