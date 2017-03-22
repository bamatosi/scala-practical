package model

import play.api.libs.json._
import slick.driver.H2Driver.api._

// Case class providing application model for a tweet
case class Tweet (id: Option[Long] = None, author: String, message: String, link: String)

// Table class priding twwet model for Slick
class TweetsTable(tag: Tag) extends Table[Tweet](tag, "TWEET") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def author = column[String]("author")
  def message = column[String]("message")
  def link = column[String]("link")

  override def * = (id.?, author, message, link) <>(Tweet.tupled, Tweet.unapply)
}

// Tweet format needed for JSON serializer
object TweetJSON {
  implicit val tweetWrites = new Writes[Tweet] {
    def writes(tweet: Tweet) = Json.obj(
      "id" -> tweet.id,
      "author" -> tweet.author,
      "message" -> tweet.message,
      "link" -> tweet.link
    )
  }
}