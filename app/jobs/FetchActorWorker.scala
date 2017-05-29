package jobs

import akka.actor.Actor
import akka.event.{Logging, LoggingAdapter}
import akka.actor.Props
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import java.net.URLEncoder
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import model.Tweet
import repos.TweetsRepoImpl

object FetchActorWorker {

  case object Initialize

  def props(tag: String, authToken: String, tweetsRepo: TweetsRepoImpl): Props = Props(new FetchActorWorker(tag, authToken, tweetsRepo))
}

/* Response reads */
case class SearchResults(statuses: Seq[Tweet], metaData: MetaData)
case class Url(url: String, expanded_url: String)
case class MetaData(maxId: Long, sinceId: Long, nextResults: Option[String], refreshUrl: Option[String])

class FetchActorWorker(tag: String, authToken: String, tweetsRepo: TweetsRepoImpl) extends Actor {

  import akka.pattern.pipe
  import context.dispatcher
  import HttpProtocols._
  import HttpMethods._

  /* Params */
  val encodedTag: String = URLEncoder.encode(tag, "UTF-8")
  var defaultFetchParams: String = s"?q=$encodedTag&count=5&include_entities=1"
  var pages: Int = 20
  val basePath: String = "https://api.twitter.com/1.1/search/tweets.json"

  val log: LoggingAdapter = Logging(context.system, this)
  val httpClient: HttpExt = Http(context.system)
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  /* Reads */
  implicit val urlRead: Reads[Url] = (
    (JsPath \ "url").read[String] and
    (JsPath \ "expanded_url").read[String]
  ) (Url.apply _)

  implicit val statusReads: Reads[Tweet] = (
    (JsPath \ "id_str").readNullable[String].map[Option[Long]] {
      case Some(d: String) => Some(d.toLong)
      case _ => None
    } and
    (JsPath \ "user" \ "screen_name").read[String] and
    (JsPath \ "text").read[String] and
    (JsPath \ "entities" \ "urls").read[Seq[Url]].map[String]((v) => if (v.nonEmpty) v.head.url else "")
  ) (Tweet.apply _)

  implicit val searchMetadataReads: Reads[MetaData] = (
    (JsPath \ "max_id_str").read[String].map[Long]((v) => v.toLong) and
    (JsPath \ "since_id_str").read[String].map[Long]((v) => v.toLong) and
    (JsPath \ "next_results").readNullable[String] and
    (JsPath \ "refresh_url").readNullable[String]
  ) (MetaData.apply _)

  implicit val tweetsReads: Reads[SearchResults] = (
    (JsPath \ "statuses").read[Seq[Tweet]] and
    (JsPath \ "search_metadata").read[MetaData]
  ) (SearchResults.apply _)

  override def postStop(): Unit = {
    log.info(s"Worker processing $tag stopped")
  }

  def receive: PartialFunction[Any, Unit] = {
    case FetchActorWorker.Initialize =>
      log.info(s"Initialize fetch for '$tag'")
      sender() ! FetchActorMaster.StatusUpdate(tag, 0)
      httpClient.singleRequest(fetch(authToken, defaultFetchParams)).pipeTo(self)

    /*
     * Piped success response
     */
    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        val json = Json.parse(body.utf8String)
        json.validate[SearchResults] match {
          case s: JsSuccess[SearchResults] =>
            val response: SearchResults = s.get
            val tweets: Seq[Tweet] = response.statuses
            val metaData: MetaData = response.metaData

            for (tweet <- tweets) yield tweetsRepo.insert(tweet)

            metaData.nextResults match {
              case Some(nextResults: String) if pages > 0 =>
                pages -= 1
                httpClient.singleRequest(fetch(authToken, nextResults)).pipeTo(self)
              case Some(_) if pages == 0 =>
                context.parent ! FetchActorMaster.StatusUpdate(tag, 1)
              case None =>
                log.info("No more results")
                context.parent ! FetchActorMaster.StatusUpdate(tag, 1)
            }
          case e: JsError =>
            log.error(e.toString)
            context.parent ! FetchActorMaster.StatusUpdate(tag, -1)
        }
      }

    /*
     * Piped non-success response
     */
    case resp@HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      context.parent ! FetchActorMaster.StatusUpdate(tag, -1)
      resp.discardEntityBytes()
  }

  // Twitter data fetch for a specific tag
  def fetch(authToken: String, params: String): HttpRequest = {
    /*
    GET https://api.twitter.com/1.1/search/tweets.json?q={HTTP encoded string}
    + header
      Authorization: Bearer AAAAAAAAAAAAAAAAAAAAALIo0AAAAAAASoA3fY9Ycgxf5O2pYqVz%2FlHm5bs%3DD3VSfjGjgNISgFHukKq6k09tIHeJtaZasthEl8KhDFcxFY8Koi
    */
    val authorization = headers.Authorization(headers.OAuth2BearerToken(authToken))
    val uri = s"$basePath$params"
    log.info(s"Calling $uri")
    HttpRequest(GET, uri = uri, headers = List(authorization), protocol = `HTTP/1.0`)
  }
}