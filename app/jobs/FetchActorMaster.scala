package jobs

import akka.actor.Actor
import akka.event.Logging
import akka.actor.Props
import scala.collection.mutable.Map

object FetchActorMaster {

  case object Status

  case class StatusUpdate(tag: String, progress: Int)

  def props(uuid: String, tags: Option[Seq[String]]): Props = Props(new FetchActorMaster(uuid, tags))
}

class FetchActorMaster(uuid: String, tags: Option[Seq[String]]) extends Actor {
  val log = Logging(context.system, this)

  val workLoad = Map[String, Boolean]()

  override def preStart(): Unit = {
    // create actors for each tag
    tags match {
      case Some(tags) => {
        tags.foreach(tag => {
          // TODO Provide a cleanup for an actor (worker should be terminated when finishing work)
          val worker = context.actorOf(FetchActorWorker.props(tag), "worker" + tag)
          worker ! FetchActorWorker.Initialize
          workLoad += (tag -> false)
        })
        log.info("Started master. " + workLoad)
      }
      case None => {
        log.info("No tags provided.")
        // TODO Close Master then
      }
    }
  }

  override def postStop():Unit = {
    log.info(s"Master $uuid stopped")
  }

  def receive = {
    case FetchActorMaster.Status => {
      log.info("Propagating status")
      sender() ! workLoad
    }
    case FetchActorMaster.StatusUpdate(tag, progress) => {
      log.info(s"Updating status for $tag: $progress")
      workLoad(tag) = (progress == 100)
    }
  }
}