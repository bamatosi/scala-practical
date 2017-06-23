package jobs

import akka.actor.Actor
import akka.event.Logging
import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import play.api.libs.json._
import repos.TweetsRepoImpl
import scala.collection.mutable

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

  var workLoad: mutable.Map[String, String] = mutable.Map[String, String]()

  /* Twitter auth and REST API config  */
  val consumerKey = "eY4xR9bzT9kc84URZMbAYSsjw"
  val consumerSecret = "uVqLYmQDdfGe2pbr5npzR3CzU7PCbHR5ZF79MftR7CvFpoFG5U"

  override def preStart(): Unit = {
    log.info("Starting Master and authenticating to twitter API")
    httpClient.singleRequest(authenticate(consumerKey, consumerSecret)).pipeTo(self)
  }

  override def postStop():Unit = {
    log.info(s"Master $uuid stopped")
  }

  def receive: PartialFunction[Any, Unit] = {
    case FetchActorMaster.StatusPropagate =>
      log.info("Propagating status")
      sender() ! workLoad

    case FetchActorMaster.StatusUpdate(tag, progress) => {
      log.info(s"Updating status for $tag: $progress")
      workLoad(tag) = progress match {
        case -1 => "error"
        case 0 => "processing"
        case 1 => "done"
      }
      // Close master and trigger indexer when all tags are processed
      val numberProcessed = workLoad.values.groupBy(identity).mapValues(_.size).getOrElse("done",0)
      val numberAll = workLoad.keys.size
      if (numberProcessed == numberAll) {
        println(tweetsRepo)
        tweetsRepo.indexTweets()
        context.stop(self)
      }
    }

    /*
    * Piped success response
    */
    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        val json = Json.parse(body.utf8String)
        val maybeAuth = (json \ "access_token").asOpt[String]
        maybeAuth match {
          case None =>
            log.error("Unexpected authentication response")
          case Some(accessToken) =>
            initializeWorkers(accessToken, tags)
            log.info(s"Authentication successful. Workers started.")
        }
      }

    /*
    * Piped non-success response
    */
    case resp@HttpResponse(code, _, _, _) =>
      log.info(s"Authentication request failed, response code: $code")
      resp.discardEntityBytes()
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

  def initializeWorkers(accessToken: String, tags: Option[Seq[String]]): Unit = {
    // create actors for each tag
    tags match {
      case Some(tags) =>
        tags.foreach(tag => {
          val worker = context.actorOf(FetchActorWorker.props(tag, accessToken, tweetsRepo), "worker" + tag.replaceAll("[^A-Z-a-z0-9]", "_"))
          worker ! FetchActorWorker.Initialize
        })
      case None =>
        log.info("No worker initialized since no tags provided.")
    }
  }
}