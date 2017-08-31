package model

import model.Exceptions.IndexBusyException
import model.Types.Word

/**
  * Representation of corpus that can be searched
  */
class Corpus() {
  var docsCount:Double = 0.0
  var idf: Map[String, Double] = Map.empty
  var tfidf: Map[Word, List[TfIdfVector]] = Map.empty
  var indexing: Boolean = false
  var corpusSize = 0

  def tfidfLookup(tfIdfVector: TfIdfVector): List[TfIdfVector] =
    tfidf
      .filter(e => tfIdfVector.originalDoc.words.contains(e._1)) // Take only vectors for word thaat exist in phrase
      .values
      .foldLeft(List.empty[TfIdfVector])(_ ++ _) // Combine lists for words from phrase
      .distinct // We need only unique values
      .filterNot(vector => vector.originalDoc.docId == tfIdfVector.originalDoc.docId) // Filter out

  def sortResults(s1: SearchResult, s2: SearchResult): Boolean = s1.similarity >= s2.similarity

  def index(corpus: List[Document]): Unit = {
    indexing = true
    docsCount = corpus.size
    idf = corpus
      .flatMap(m => m.wordsCount.keys)
      .groupBy(identity)
      .mapValues(
        occurences => 1 + Math.log(docsCount/occurences.size.toDouble)
      )

    // Adding additional indexing by Word to tfidf to reduce the amount of calculation when searching
    // This will eliminate comparisons that will end up with 0.0 score when searching
    tfidf = corpus
      .flatMap(doc => doc.words.flatMap(w => Seq(w -> doc)))
      .groupBy(_._1)
      .mapValues(docs => docs.map(doc => TfIdfVector(doc._2, idf)))
    indexing = false
    corpusSize = corpus.size
    println(s"Indexing completed idf: ${idf.size} terms, tfidf index size ${tfidf.size} with total ${tfidf.mapValues(_.size).values.sum} entries for $corpusSize documents")
  }

  def search(phrase: Document, numberOfResults: Int): List[SearchResult] = {
    println(s"Searching for ${phrase.toString} on corpus of $corpusSize docs")
    if (indexing) throw new IndexBusyException()
    else {
      val phraseVector = TfIdfVector(phrase, idf)
      tfidfLookup(phraseVector)
        .map((docVector: TfIdfVector) =>
          SearchResult(docVector.originalDoc.toString, phraseVector.cosineWith(docVector), docVector.originalDoc.docId)
        )
        .take(numberOfResults)
        .sortWith(sortResults)
    }
  }
}
