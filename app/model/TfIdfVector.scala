package model

/**
  * Representation of vector tfidf
  */
class TfIdfVector(doc: Document, idf: Map[String, Double]) {
  val originalDoc: Document = doc
  def tfidf(tf: Double, idf: Double): Double = tf * idf
  val length: Double = Math.sqrt(doc.tf.keys.toList
    .map(term => doc.tf(term)*idf.getOrElse(term, 0.0))
    .map(a => a * a)
    .sum
  )
  def cosineWith(other: TfIdfVector): Double = {
    val commonPart = doc.tf.keySet.intersect(other.originalDoc.tf.keySet).toList

    val dotProduct = commonPart
      .map(term => {
        val docTFIDF = doc.tf(term)*idf(term)
        val otherTFIDF = other.originalDoc.tf(term)*idf(term)
        docTFIDF * otherTFIDF
      })
      .sum

    val lengths = {
      length * other.length
    }
    println(s"Comparing\n\t${doc.words} with\n\t${other.originalDoc.words}")
    println(s"\t-----> $dotProduct/$lengths")
    dotProduct/lengths
  }
  override def toString: String = doc.toString()
}
