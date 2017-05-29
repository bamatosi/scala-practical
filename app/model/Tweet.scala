package model

import play.api.libs.json._
import slick.driver.H2Driver.api._
import slick.lifted.ProvenShape

// Case class providing application model for a tweet
case class Tweet (id: Option[Long] = None, author: String, message: String, link: String)

// Table class priding twwet model for Slick
class TweetsTable(tag: Tag) extends Table[Tweet](tag, "TWEET") {
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def author: Rep[String] = column[String]("author")
  def message: Rep[String] = column[String]("message")
  def link: Rep[String] = column[String]("link")

  override def * : ProvenShape[Tweet] = (id.?, author, message, link) <>(Tweet.tupled, Tweet.unapply)
}

// Tweet format needed for JSON serializer
object TweetJSON {
  implicit val tweetWrites = new Writes[Tweet] {
    def writes(tweet: Tweet): JsObject = Json.obj(
      "id" -> tweet.id,
      "author" -> tweet.author,
      "message" -> tweet.message,
      "link" -> tweet.link
    )
  }
}