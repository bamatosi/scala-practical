import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "TweetsController" should {
    "save the tweet" in {
      val postJson = Json.obj("author" -> "tstauthor", "message" -> "loremipsum", "link" -> "http://someline.pl")
      val result = route(app,
        FakeRequest(POST, controllers.routes.TweetsController.save().url)
          .withJsonBody(postJson)
          .withHeaders(CONTENT_TYPE -> "application/json")
      ).get
      status(result) mustBe OK
    }
  }

  "FetchController" should {
    "execute fetch process" in {
      val qs = "tag=%23scala&tag=%23spark&tag=%23cassandra&tag=%23java&tag=%23redis&tag=%23bigdata&tag=%23iot"
      val fetchUrl = s"${controllers.routes.FetchController.fetch().url}?$qs"
      val result = route(app,
        FakeRequest(POST, fetchUrl)
      ).get
      status(result) mustBe OK
      val uuid = contentAsString(result)

      Thread.sleep(1000)

      val statusUrl = s"${controllers.routes.FetchController.status(uuid).url}"
      println(s"Calling $statusUrl")
      val statusResult = route(app,
        FakeRequest(GET, statusUrl)
      ).get
      status(statusResult) mustBe OK
      println(contentAsString(statusResult))
    }
  }

}
