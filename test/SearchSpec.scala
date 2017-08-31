import model.{Corpus, Document, SearchResult, TfIdfVector}
import org.scalatestplus.play._
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import Matchers._

class SearchSpec extends PlaySpec {
  val documents = List(
    "The game of life is a game of everlasting learning",
    "The unexamined life is not worth living",
    "Never stop learning"
  )

  val phrase = Document("never stop", 0L)

  val doc1 = Document(documents.head, 1L)
  val doc2 = Document(documents(1), 2L)
  val doc3 = Document(documents(2), 3L)

  "Documents have proper TF" in {
    doc1.tf should equal(Map("is" -> 0.1, "a" -> 0.1, "everlasting" -> 0.1, "life" -> 0.1, "game" -> 0.2, "learning" -> 0.1, "of" -> 0.2, "the" -> 0.1))
    doc2.tf should equal(Map("is" -> 0.14285714285714285, "unexamined" -> 0.14285714285714285, "worth" -> 0.14285714285714285, "not" -> 0.14285714285714285, "life" -> 0.14285714285714285, "living" -> 0.14285714285714285, "the" -> 0.14285714285714285))
    doc3.tf should equal(Map("never" -> 0.3333333333333333, "stop" -> 0.3333333333333333, "learning" -> 0.3333333333333333))
  }

  val docList: List[Document] = List(doc1, doc2, doc3)
  val corpus = new Corpus()
  corpus.index(docList)

  "Corpus should have proper IDF" in {
    corpus.idf should equal(Map("is" -> 1.4054651081081644, "unexamined" -> 2.09861228866811, "a" -> 2.09861228866811, "stop" -> 2.09861228866811, "everlasting" -> 2.09861228866811, "worth" -> 2.09861228866811, "not" -> 2.09861228866811, "life" -> 1.4054651081081644, "game" -> 2.09861228866811, "learning" -> 1.4054651081081644, "of" -> 2.09861228866811, "living" -> 2.09861228866811, "the" -> 1.4054651081081644, "never" -> 2.09861228866811))
  }

  "Corpus should have proper index on top of TFIDF" in {
    corpus.tfidf.toString() should equal(Map(
      "is" -> List("the game of life is a game of everlasting learning", "the unexamined life is not worth living"),
      "unexamined" -> List("the unexamined life is not worth living"),
      "a" -> List("the game of life is a game of everlasting learning"),
      "stop" -> List("never stop learning"),
      "everlasting" -> List("the game of life is a game of everlasting learning"),
      "worth" -> List("the unexamined life is not worth living"),
      "not" -> List("the unexamined life is not worth living"),
      "life" -> List("the game of life is a game of everlasting learning", "the unexamined life is not worth living"),
      "game" -> List("the game of life is a game of everlasting learning", "the game of life is a game of everlasting learning"),
      "learning" -> List("the game of life is a game of everlasting learning", "never stop learning"),
      "of" -> List("the game of life is a game of everlasting learning", "the game of life is a game of everlasting learning"),
      "living" -> List("the unexamined life is not worth living"),
      "the" -> List("the game of life is a game of everlasting learning", "the unexamined life is not worth living"),
      "never" -> List("never stop learning")
    ).toString())
  }

  "Index lookup should work properly" in {
    val phraseVector = TfIdfVector(phrase, corpus.idf)
    val lookup = corpus.tfidfLookup(phraseVector)
    lookup.size mustBe 1
    lookup.head.toString mustBe "never stop learning"
  }

  "Corpus returns proper results when searched" in {
    corpus.search("life learning", 3).toString() should equal(List(
      SearchResult(doc3.toString, 0.30263669792912185),
      SearchResult(doc1.toString, 0.2757854081643117),
      SearchResult(doc2.toString, 0.2048221980047982)
    ).toString)

    // "The unexamined life is not worth living" should not be here since it should be excluded by index
    corpus.search("never stop learning", 3).toString() should equal(List(
      SearchResult(doc3.toString, 1.0),
      SearchResult(doc1.toString, 0.08346278526388236)
    ).toString)

    corpus.search("never stop learning", 1).toString() should equal(List(
      SearchResult(doc3.toString, 1.0)
    ).toString())
  }
}
