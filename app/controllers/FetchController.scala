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
    val fetchMaster = twitter.actorOf(FetchActorMaster.props(uuid, tags), "FetchMaster-"+uuid)
    masterMap += (uuid -> fetchMaster)

    Ok("Started "+uuid)
  }

  def status(uuid: String) = Action.async { implicit request =>
    if (masterMap.contains(uuid)) {
      val fetchMaster = masterMap(uuid);
      implicit val timeout = Timeout(10, TimeUnit.SECONDS)
      val statusFuture = (fetchMaster ? FetchActorMaster.Status).mapTo[Map[String,Boolean]]
      statusFuture.map(result => {
        println(result)
        Ok("Status OK") // TODO Provide Writeable for result and output it in response
       }).recover {
        case ex: AskTimeoutException => Ok("Status timeout expired")
        case _ => Ok("Error")
      }
    } else {
      Future {
        Ok("No master uuid found")
      }
    }
  }
}
