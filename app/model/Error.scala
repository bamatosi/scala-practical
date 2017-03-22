package model

import play.api.libs.json._

// Case class providing application model for a tweet
case class Error (code: String, message: String)

object ErrorJSON {
  implicit val errorWrites = new Writes[Error] {
    def writes(error: Error) = Json.obj(
      "code" -> error.code,
      "message" -> error.message
    )
  }
}