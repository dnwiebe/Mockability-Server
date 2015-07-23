package mockability.utils

import org.scalatest.path
import play.api.libs.json.Json
import play.api.mvc._
import org.mockito.Mockito._

/**
 * Created by dnwiebe on 7/19/15.
 */
class RecordedRequestTest extends path.FunSpec {
  describe ("A RecordedRequest created from miscellaneous parameters") {
    val subject = RecordedRequest ("WUfF", "blIggo", List (("one", "two"), ("three", "four")), Array ('x'.toByte))

    it ("is as expected") {
      assert (subject.method === "WUFF")
      assert (subject.uri === "blIggo")
      assert (subject.headers === List (("one", "two"), ("three", "four")))
      assert (subject.body === Array ('x'.toByte))
    }
  }

  describe ("A RecordedRequest created from a Request") {
    val headers = mock (classOf [Headers])
    when (headers.keys).thenReturn (Set ("one", "three"))
    when (headers.getAll ("one")).thenReturn (Seq ("two", "four"))
    when (headers.getAll ("three")).thenReturn (Seq ("four"))
    val request = mock (classOf [Request[AnyContent]])
    when (request.method).thenReturn ("WUfF")
    when (request.uri).thenReturn ("blIggo")
    when (request.headers).thenReturn (headers)
    when (request.body).thenReturn (AnyContentAsRaw (RawBuffer (50, "pokey bits".getBytes)))
    val subject = RecordedRequest (request)

    it ("is as expected") {
      assert (subject.method === "WUFF")
      assert (subject.uri === "blIggo")
      assert (subject.headers === List (("one", "two"), ("one", "four"), ("three", "four")))
      assert (subject.body === "pokey bits".getBytes)
    }

    describe ("when converted to JSON") {
      val result = subject.toJson

      it ("is as expected") {
        assert (result ===
          Json.obj (
            "method" -> "WUFF",
            "uri" -> "blIggo",
            "headers" -> Json.arr (
              Json.obj ("name" -> "one", "value" -> "two"),
              Json.obj ("name" -> "one", "value" -> "four"),
              Json.obj ("name" -> "three", "value" -> "four")
            ),
            "body" -> "cG9rZXkgYml0cw=="
          )
        )
      }
    }
  }
}
