package model

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import play.api.libs.json.{JsObject, Json, Writes}

import scala.collection.mutable

/**
  * Created by bartosz on 01/09/17.
  */
case class FetchStatusReport(startTime: LocalDateTime, workload: mutable.Map[String, String]) {
  def getStartTime: String = startTime.toString
  def getWorkTime: String = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()).toString
}

// Tweet format needed for JSON serializer
object FetchStatusReportJSON {
  implicit val fetchStatusReportWrites = new Writes[FetchStatusReport] {
    def writes(fetchStatus: FetchStatusReport): JsObject = Json.obj(
      "startTime" -> fetchStatus.getStartTime,
      "processTime" -> fetchStatus.getWorkTime,
      "report" -> fetchStatus.workload
    )
  }
}


