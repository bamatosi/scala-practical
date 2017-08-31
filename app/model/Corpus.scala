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

  def tfidfLookup(tfIdfVector: TfIdfVector): List[TfIdfVector] = tfidf.filter(e => tfIdfVector.originalDoc.words.contains(e._1)).values.foldLeft(List.empty[TfIdfVector])(_ ++ _).distinct

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
    println(s"Indexing completed idf: ${idf.size} terms, tfidf index size ${tfidf.size} with total ${tfidf.mapValues(_.size).values.sum} entries for ${corpus.size} documents")
  }

  def search(phrase: Word, numberOfResults: Int): List[SearchResult] = {
    println(s"Searching for $phrase on corpus of ${tfidf.size} docs")
    if (indexing) throw new IndexBusyException()
    else {
      val doc = Document(phrase.toLowerCase(), 0L)
      val phraseVector = TfIdfVector(doc, idf)
      tfidfLookup(phraseVector)
        .map((docVector: TfIdfVector) =>
          SearchResult(docVector.originalDoc.toString, phraseVector.cosineWith(docVector), docVector.originalDoc.docId)
        )
        .take(numberOfResults)
        .sortWith(sortResults)
    }
  }
}
