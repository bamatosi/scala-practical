package repos

import slick.driver.H2Driver.api._
import model.Tweet
import slick.lifted.ProvenShape

class Tweets(tag: Tag) extends Table[Tweet](tag, "TWEET") {
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def author: Rep[String] = column[String]("author")
  def message: Rep[String] = column[String]("message")
  def link: Rep[String] = column[String]("link")

  override def * : ProvenShape[Tweet] = (id.?, author, message, link) <>(Tweet.tupled, Tweet.unapply)
}