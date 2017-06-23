package repos

import javax.inject.Singleton

import slick.driver.H2Driver.api._

import concurrent._
import model._
import slick.driver.H2Driver

import scala.util.{Failure, Success}
import concurrent.ExecutionContext.Implicits.global

trait TweetsRepo {
  def list: Future[Seq[Tweet]]
  def findById(id: Long): Future[Tweet]
  def insert(tweet: Tweet): Future[Int]
  def update(id: Long, tweet: Tweet): Future[Int]
  def delete(id: Long): Future[Int]
  def count: Future[Int]
  def indexTweets(): Future[Unit]
  def recommendFor(id: Long): Future[List[SearchResult]]
}

@Singleton
class TweetsRepoImpl extends TweetsRepo {

  /* initialize repository */
  val tweets: TableQuery[TweetsTable] = TableQuery[TweetsTable]
  val db: H2Driver.backend.Database = Database.forConfig("h2mem")
  val setupAction: DBIO[Unit] = DBIO.seq(
    tweets.schema.create
  )
  db.run(setupAction).onComplete {
    case Success(_) => println("TweetsRepoImpl initialized")
    case Failure(t) => println("Initialization error has occured: " + t.getMessage)
  }
  val corpus = new Corpus()

  private def filterById(id: Long) = tweets.filter(_.id === id)

  /* Public API */
  def findById(id: Long): Future[Tweet] = db.run(filterById(id).result.head)
  def list: Future[Seq[Tweet]] = db.run(tweets.result)
  def insert(tweet: Tweet): Future[Int] = db.run(tweets += tweet)
  def update(id: Long, tweet: Tweet): Future[Int] = db.run(filterById(id).update(tweet))
  def delete(id: Long): Future[Int] = db.run(filterById(id).delete)
  def count: Future[Int] = db.run(tweets.length.result)
  def indexTweets(): Future[Unit] = list.map(tweets => corpus.index(tweets.toList.map(tweet => new Document(tweet.message))))
  def recommendFor(id: Long): Future[List[SearchResult]] = findById(id).map(tweet => corpus.search(tweet.message))
}