package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import play.api.libs.json.{JsObject, Json}
import play.api.data._
import play.api.data.Forms._
import model.{Error, Tweet}
import model.TweetJSON._
import model.ErrorJSON._
import repos.TweetsRepoImpl

import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class TweetsController @Inject()(
  tweetsRepo: TweetsRepoImpl
) extends Controller {

  // Tweet form definition needed for POST actions
  val tweetForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "author" -> nonEmptyText,
      "message" -> nonEmptyText,
      "link" -> nonEmptyText
    )(Tweet.apply)(Tweet.unapply)
  )

  // get tweet by id
  def get(id: Int) = Action.async { implicit request =>
    tweetsRepo
      .findById(id)
      .map(tweet => Ok(Json.toJson(tweet)))
      .recover {
        case ex: Exception => {
          val error = new Error("Controller", "Getting #" + id + " failed: " + ex.getMessage)
          InternalServerError(Json.toJson(error))
        }
      }
  }

  // save new tweet
  def save: Action[AnyContent] = Action.async { implicit request =>
    tweetForm.bindFromRequest.fold(
      formWithErrors => Future {
        val error = new Error("Controller", "Tweet saving failed")
        InternalServerError(Json.toJson(error))
      },
      tweet => {
        tweetsRepo.insert(tweet)
          .map (result => Ok(Json.toJson(tweet)))
          .recover {
              case ex: Exception =>
                val error = new Error("Controller", "Saving tweet failed: "+ex.getMessage())
                InternalServerError(Json.toJson(error))
          }
      })
  }

  // delete tweet by id
  def delete(id: Int) = Action.async { implicit request =>
    tweetsRepo
      .delete(id)
      .map(tweet => Ok(Json.toJson(tweet)))
      .recover {
        case ex: Exception => {
          val error = new Error("Controller", "Deleting #" + id + " failed: " + ex.getMessage)
          InternalServerError(Json.toJson(error))
        }
      }
  }

  // update/replace existing tweet
  def update(id: Int) = Action.async { implicit request =>
    tweetForm.bindFromRequest.fold(
      formWithErrors => Future {
        val error = new Error("Controller", "Tweet updating failed")
        InternalServerError(Json.toJson(error))
      },
      tweet => {
        tweetsRepo.update(id, tweet.copy(id = Some(id)))
          .map(result => Ok(Json.toJson(tweet)))
          .recover {
            case ex: Exception =>
              val error = new Error("Controller", "Updating #" + id + " failed: "+ex.getMessage())
              InternalServerError(Json.toJson(error))
          }
      })
  }

  // list tweets by pages
  def list = Action.async { implicit request =>
    tweetsRepo.list.map(tweets => Ok(
      Json.toJson(tweets)
    ))
  }

}
