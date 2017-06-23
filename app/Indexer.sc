//import model.{Corpus, Document}
//
//val documents = List(
//  "The game of life is a game of everlasting learning",
//  "The unexamined life is not worth living",
//  "Never stop learning"
//)
//
//val doc1 = new Document(documents.head)
//val doc2 = new Document(documents(1))
//val doc3 = new Document(documents(2))
//
//doc1.tf
//// Expected: res0: Map[String,Double] = Map(is -> 0.1, a -> 0.1, everlasting -> 0.1, life -> 0.1, game -> 0.2, learning -> 0.1, of -> 0.2, the -> 0.1)
//
//doc2.tf
//// Expected: res1: Map[String,Double] = Map(is -> 0.14285714285714285, unexamined -> 0.14285714285714285, worth -> 0.14285714285714285, not -> 0.14285714285714285, life -> 0.14285714285714285, living -> 0.14285714285714285, the -> 0.14285714285714285)
//
//doc3.tf
//// Expected: res2: Map[String,Double] = Map(never -> 0.3333333333333333, stop -> 0.3333333333333333, learning -> 0.3333333333333333)
//
//val docList: List[Document] = List(doc1, doc2, doc3)
//val corpus = new Corpus()
//corpus.index(docList)
//// corpus.idf
//// Expected res3: Map[String,Double] = Map(is -> 1.4054651081081644, unexamined -> 2.09861228866811, a -> 2.09861228866811, stop -> 2.09861228866811, everlasting -> 2.09861228866811, worth -> 2.09861228866811, not -> 2.09861228866811, life -> 1.4054651081081644, game -> 2.09861228866811, learning -> 1.4054651081081644, of -> 2.09861228866811, living -> 2.09861228866811, the -> 1.4054651081081644, never -> 2.09861228866811)
//
//corpus.search("life learning")
//// expected res3: List[SearchResult] = List(0.2757854081643117 -> the game of life is a game of everlasting learning, 0.2048221980047982 -> the unexamined life is not worth living, 0.30263669792912185 -> never stop learning)
//
//corpus.search("never stop learning")
//// expected res4: List[SearchResult] = List(0.08346278526388236 -> the game of life is a game of everlasting learning, 0.0 -> the unexamined life is not worth living, 1.0 -> never stop learning)
//

//////
val a = Map("a"->"processing", "b"->"processing", "c"->"notprocessing")
a.values.groupBy(identity).mapValues(_.size)