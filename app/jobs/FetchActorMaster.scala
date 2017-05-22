package jobs

import akka.actor.Actor
import akka.event.Logging
import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString

import java.util.Base64
import java.nio.charset.StandardCharsets
import java.net.{URLEncoder}
import play.api.libs.json._

import scala.collection.mutable.Map
import repos.TweetsRepoImpl;

object FetchActorMaster {

  case object StatusPropagate

  case class StatusUpdate(tag: String, status: Int)

  def props(uuid: String, tags: Option[Seq[String]], tweetsRepo: TweetsRepoImpl): Props = Props(new FetchActorMaster(uuid, tags, tweetsRepo))
}

class FetchActorMaster(uuid: String, tags: Option[Seq[String]], tweetsRepo: TweetsRepoImpl) extends Actor {

  import akka.pattern.pipe
  import context.dispatcher
  import HttpProtocols._
  import MediaTypes._
  import HttpCharsets._
  import HttpMethods._

  val log = Logging(context.system, this)
  val httpClient = Http(context.system)
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  var workLoad = Map[String, Boolean]()

  /* Twitter auth and REST API config - move to props */
  val consumerKey = "T4BJURJjkXZmUjPUcW8S6MtDJ"
  val consumerSecret = "oq1gDqknDkndPKuYdNAUad7hlCtrE2D4tGe5S55V9jmhPxJMIC"

  override def preStart(): Unit = {
    log.info("Starting Master and authenticating to twitter API")
    httpClient.singleRequest(authenticate(consumerKey, consumerSecret)).pipeTo(self)
  }

  override def postStop():Unit = {
    log.info(s"Master $uuid stopped")
  }

  def receive = {
    case FetchActorMaster.StatusPropagate => {
      log.info("Propagating status")
      sender() ! workLoad
    }

    case FetchActorMaster.StatusUpdate(tag, progress) => {
      log.info(s"Updating status for $tag: $progress")
      workLoad(tag) = (progress == 100)
    }

    /*
    * Piped success response
    */
    case HttpResponse(StatusCodes.OK, headers, entity, _) => {
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        val json = Json.parse(body.utf8String)
        val maybeAuth = (json \ "access_token").asOpt[String]
        maybeAuth match {
          case None => {
            log.error("Unexpected authentication response")
          }
          case Some(accessToken) => {
            workLoad = initializeWorkers(accessToken, tags)
            log.info(s"Authentication sucessfull. Workers started $workLoad")
          }
        }
      }
    }

    /*
    * Piped non-success response
    */
    case resp@HttpResponse(code, _, _, _) => {
      log.info(s"Authentication request failed, response code: $code")
      resp.discardEntityBytes()
    }
  }

  // Twitter authentication request
  def authenticate(consumerKey: String, consumerSecret: String): HttpRequest = {
    /*
    POST oauth2 / token
    Mandatory headers
      Authorization: Basic eHZ6MWV2RlM0d0VFUFRHRUZQSEJvZzpMOHFxOVBaeVJnNmllS0dFS2hab2xHQzB2SldMdzhpRUo4OERSZHlPZw==
      Content-Type: application/x-www-form-urlencoded;charset=UTF-8

    Mandatory body:
      grant_type=client_credentials
    */
    val authorization = headers.Authorization(headers.BasicHttpCredentials(consumerKey, consumerSecret))
    HttpRequest(
      POST,
      uri = "https://api.twitter.com/oauth2/token",
      entity = HttpEntity(`application/x-www-form-urlencoded` withCharset `UTF-8`, ByteString("grant_type=client_credentials")),
      headers = List(authorization),
      protocol = `HTTP/1.0`
    )
  }

  def initializeWorkers(accessToken: String, tags: Option[Seq[String]]): Map[String, Boolean] = {
    val workLoad = Map[String, Boolean]();

    // create actors for each tag
    tags match {
      case Some(tags) => {
        tags.foreach(tag => {
          // TODO Provide a cleanup for an actor (worker should be terminated when finishing work)
          val worker = context.actorOf(FetchActorWorker.props(tag, accessToken, tweetsRepo), "worker" + tag.replaceAll("[^A-Z-a-z0-9]", "_"))
          worker ! FetchActorWorker.Initialize
          workLoad += (tag -> false)
        })
      }
      case None => {
        log.info("No worker initialized since no tags provided.")
      }
    }

    workLoad
  }
}