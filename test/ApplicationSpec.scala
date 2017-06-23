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

  "HomeController" should {
    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/plain")
      contentAsString(home) must include ("Your new application is ready.")
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

}
