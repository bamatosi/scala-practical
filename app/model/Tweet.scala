package model

import play.api.libs.json._

// Case class providing model for a tweet
case class Tweet (id: Option[Long] = None, author: String, message: String, link: String)

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