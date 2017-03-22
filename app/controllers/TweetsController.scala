package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json.{JsObject, Json}
import model.Tweet
import model.TweetJSON._
import repos.TweetsRepoImpl
import concurrent.ExecutionContext.Implicits.global

@Singleton
class TweetsController @Inject()(
  tweetsRepo: TweetsRepoImpl
) extends Controller {

  // get tweet by id
  def get(id: Int) = TODO
  // save new tweet
  def save(tweet: Tweet) = TODO
  // delete tweet by id
  def delete(id: Int) = TODO
  // update/replace existing tweet
  def update(id: Int) = TODO

  // list tweets by pages
  def list = Action.async { implicit request =>
    tweetsRepo.list.map(tweets => Ok(
      Json.toJson(tweets)
    ))
  }

}
