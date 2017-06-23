package model

/**
  * Representing single search result
  */
class SearchResult(document: String, similarity: Double) {
  override def toString: String = s"$similarity -> $document"
}
