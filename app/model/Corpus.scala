package model

import model.Exceptions.IndexBusyException
import model.Types.Word

/**
  * Representation of corpus that can be searched
  */
class Corpus() {
  var docsCount:Double = 0.0
  var idf: Map[String, Double] = Map.empty
  var tfidf: List[TfIdfVector] = List.empty[TfIdfVector]
  var indexing: Boolean = false

  def index(corpus: List[Document]): Unit = {
    indexing = true
    docsCount = corpus.size
    idf = corpus
      .flatMap(m => m.wordsCount.keys)
      .groupBy(identity)
      .mapValues(
        occurences => 1 + Math.log(docsCount/occurences.size.toDouble)
      )
    tfidf = corpus.map(doc => new TfIdfVector(doc, idf))
    indexing = false
    println(s"Indexing completed idf: ${idf.size} terms, tfidf'ed documents in corpus: ${tfidf.size}")
  }

  def search(phrase: Word): List[SearchResult] = {
    println(s"Searching for $phrase on corpus of ${tfidf.size} docs")
    if (indexing) throw new IndexBusyException()
    else {
      val doc = new Document(phrase.toLowerCase())
      val phraseVector = new TfIdfVector(doc, idf)
      tfidf.map((docVector: TfIdfVector) =>
        new SearchResult(docVector.toString(), phraseVector.cosineWith(docVector))
      )
    }
  }
}
