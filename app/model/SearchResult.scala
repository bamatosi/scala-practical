package model

import play.api.libs.json.{JsObject, Json, Writes}

/**
  * Representing single search result
  */
case class SearchResult(document: String, similarity: Double, documentRefId: Long) {
  override def toString: String = s"$similarity -> $document"
}

// Tweet format needed for JSON serializer
object SearchResultJSON {
  implicit val searchResultWrites = new Writes[SearchResult] {
    def writes(searchResult: SearchResult): JsObject = Json.obj(
      "similarity" -> searchResult.similarity,
      "documentId" -> searchResult.documentRefId,
      "documentSummary" -> searchResult.document
    )
  }
}
