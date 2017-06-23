package model
import model.Types.Word
/**
  * representing document that can be indexed
  */
class Document(doc: Word) {
  val words: List[Word] = doc.toLowerCase.split("\\W+").toList
  val totalWordCount: Int = words.size
  val wordsCount: Map[String, Double] = words
    .foldLeft(Map.empty[String, Double])((count, word) => count + (word -> (count.getOrElse(word, 0.0) + 1.0)))
  def tf: Map[String, Double] = wordsCount.mapValues(count => count/totalWordCount)
  override def toString: String = words.mkString(" ")
}
