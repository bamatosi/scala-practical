package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import play.api.libs.json.{JsObject, Json}
import play.api.data._
import play.api.data.Forms._
import akka.util.Timeout
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, ActorRef}
import akka.actor.Props
import akka.pattern.{ask, AskTimeoutException}

import scala.concurrent.{Await, Future}
import scala.collection.mutable.Map

import scala.concurrent.ExecutionContext.Implicits.global // Needed for Future execution

import jobs.FetchActorMaster
import repos.TweetsRepoImpl

import model.Error
import model.ErrorJSON._

@Singleton
class FetchController @Inject()(
  tweetsRepo: TweetsRepoImpl
) extends Controller {

  val twitter = ActorSystem("twitter")
  val masterMap = Map[String, ActorRef]()

  def fetch() = Action { implicit request =>
    val tags = request.queryString.get("tag")
    val uuid = java.util.UUID.randomUUID.toString

    // TODO Terminate master when its work is done
    val fetchMaster = twitter.actorOf(FetchActorMaster.props(uuid, tags, tweetsRepo), "FetchMaster-"+uuid)
    masterMap += (uuid -> fetchMaster)

    Ok("Started "+uuid)
  }

  def status(uuid: String) = Action.async { implicit request =>
    if (masterMap.contains(uuid)) {
      val fetchMaster = masterMap(uuid);
      implicit val timeout = Timeout(10, TimeUnit.SECONDS)
      val statusFuture = (fetchMaster ? FetchActorMaster.StatusPropagate).mapTo[Map[String,String]]
      statusFuture.map(result => {
        Ok(Json.toJson(result))
       }).recover {
        case ex: AskTimeoutException => {
          val error = new Error("Controller", "Status timeout expired")
          InternalServerError(Json.toJson(error))
        }
        case ex: Exception => {
          val error = new Error("Controller", "Error: "+ex.getMessage())
          InternalServerError(Json.toJson(error))
        }
      }
    } else {
      Future {
        val error = new Error("Controller", "No master uuid found")
        InternalServerError(Json.toJson(error))
      }
    }
  }
}
