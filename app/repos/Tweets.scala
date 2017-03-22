package repos

import slick.driver.H2Driver.api._
import model.Tweet

class Tweets(tag: Tag) extends Table[Tweet](tag, "TWEET") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def author = column[String]("author")
  def message = column[String]("message")
  def link = column[String]("link")

  override def * = (id.?, author, message, link) <>(Tweet.tupled, Tweet.unapply)
}