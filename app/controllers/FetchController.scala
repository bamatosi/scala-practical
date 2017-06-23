package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.{AskTimeoutException, ask}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import jobs.FetchActorMaster
import repos.TweetsRepoImpl
import model.Error
import model.ErrorJSON._
import scala.collection.mutable

@Singleton
class FetchController @Inject()(
  tweetsRepo: TweetsRepoImpl
) extends Controller {

  val twitter: ActorSystem = ActorSystem("twitter")
  val masterMap: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()

  def fetch: Action[AnyContent] = Action { implicit request =>
    val tags = request.queryString.get("tag")
    val uuid = java.util.UUID.randomUUID.toString

    val fetchMaster = twitter.actorOf(FetchActorMaster.props(uuid, tags, tweetsRepo), "FetchMaster-"+uuid)
    masterMap += (uuid -> fetchMaster)

    Ok("Started "+uuid)
  }

  def status(uuid: String): Action[AnyContent] = Action.async { implicit request =>
    if (masterMap.contains(uuid)) {
      val fetchMaster = masterMap(uuid)
      implicit val timeout = Timeout(10, TimeUnit.SECONDS)
      val statusFuture = (fetchMaster ? FetchActorMaster.StatusPropagate).mapTo[mutable.Map[String,String]]
      statusFuture.map(result => {
        Ok(Json.toJson(result))
       }).recover {
        case _: AskTimeoutException =>
          val error = Error("Controller", "Status timeout expired")
          InternalServerError(Json.toJson(error))
        case ex: Exception =>
          val error = Error("Controller", "Error: " + ex.getMessage)
          InternalServerError(Json.toJson(error))
      }
    } else {
      Future {
        val error = Error("Controller", "No master uuid found")
        InternalServerError(Json.toJson(error))
      }
    }
  }
}
