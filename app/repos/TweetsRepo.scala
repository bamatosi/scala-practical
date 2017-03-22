package repos

import slick.driver.H2Driver.api._
import play.api.libs.json.{JsObject, Json}
import concurrent._
import model.{Tweet, TweetsTable}
import scala.util.{Success, Failure}
import concurrent.ExecutionContext.Implicits.global

trait TweetsRepo {
  def list: Future[Seq[Tweet]]
  def findById(id: Long): Future[Tweet]
  def insert(tweet: Tweet): Future[Int]
  def update(id: Long, tweet: Tweet): Future[Int]
  def delete(id: Long): Future[Int]
  def count: Future[Int]
}

class TweetsRepoImpl extends TweetsRepo {

  /* initialize repository */
  val tweets = TableQuery[TweetsTable]
  val db = Database.forConfig("h2mem")
  val setupAction: DBIO[Unit] = DBIO.seq(
    tweets.schema.create
  )
  db.run(setupAction).onComplete {
    case Success(posts) => println("TweetsRepoImpl initialized")
    case Failure(t) => println("Initialization error has occured: " + t.getMessage)
  }

  private def filterById(id: Long) = tweets.filter(_.id === id)

  /* Public API */
  def findById(id: Long): Future[Tweet] = db.run(filterById(id).result.head)
  def list: Future[Seq[Tweet]] = {
    insert(new Tweet(Option(1.asInstanceOf[Long]), "a", "b", "c"))
    db.run(tweets.result)
  }
  def insert(tweet: Tweet): Future[Int] = db.run(tweets += tweet)
  def update(id: Long, tweet: Tweet): Future[Int] = db.run(filterById(id).update(tweet))
  def delete(id: Long): Future[Int] = db.run(filterById(id).delete)
  def count: Future[Int] = db.run(tweets.length.result)
}