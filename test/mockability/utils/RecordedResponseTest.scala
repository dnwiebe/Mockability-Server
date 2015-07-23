package mockability.utils

import org.scalatest.path
import play.api.libs.json
import play.api.libs.json.Json

/**
 * Created by dnwiebe on 7/17/15.
 */
class RecordedResponseTest extends path.FunSpec {

  describe ("A RecordedResponse created with apply ()") {
    val subject = RecordedResponse (123, List (("one", "two"), ("three", "four")), "reegs".getBytes)

    it ("is as expected") {
      assert (subject.status === 123)
      assert (subject.headers === List (("one", "two"), ("three", "four")))
      assert (subject.body === Array ('r'.toByte, 'e'.toByte, 'e'.toByte, 'g'.toByte, 's'.toByte))
    }

    describe ("when converted into a string") {
      val result = subject.toString

      it ("produces the desired result") {
        assert (result ===
          """
            |------
            |HTTP/1.x 123 xxx
            |one: two
            |three: four
            |
            |reegs
            |------
            |""".stripMargin)
      }
    }
  }

  describe ("A RecordedResponse with no body created with apply ()") {
    val subject = RecordedResponse (123, List (("one", "two"), ("three", "four")), Array[Byte] ())

    it ("is as expected") {
      assert (subject.status === 123)
      assert (subject.headers === List (("one", "two"), ("three", "four")))
      assert (subject.body === Array ())
    }

    describe ("when converted into a string") {
      val result = subject.toString

      it ("produces the desired result") {
        assert (result ===
          """
            |------
            |HTTP/1.x 123 xxx
            |one: two
            |three: four
            |
            |------
            |""".stripMargin)
      }
    }
  }

  describe ("Creating several RecordedResponses with makeList ()") {
    val body = Json.parse ("""
          [
            {
              "status": 200,
              "headers": [
                {"name": "Content-Type", "value": "text/plain"},
                {"name": "Content-Length", "value": "29"}
              ],
              "body": "Rmlyc3QgcmVzcG9uc2UgdG8gL3d1cmJseS93b28="
            },
            {
              "status": 401,
              "headers": [
                {"name": "Content-Type", "value": "text/plain"},
                {"name": "Content-Length", "value": "21"}
              ],
              "body": "RG9uJ3QgcHVzaCB5b3VyIGx1Y2sh"
            }
          ]
      """)
    val subjects = RecordedResponse.makeList (body)

    it ("works for the first one") {
      val response = subjects.head
      assert (response.status === 200)
      assert (response.headers === List (("Content-Type", "text/plain"), ("Content-Length", "29")))
      assert (new String (response.body) === "First response to /wurbly/woo")
    }

    it ("works for the second one") {
      val response = subjects.last
      assert (response.status === 401)
      assert (response.headers === List (("Content-Type", "text/plain"), ("Content-Length", "21")))
      assert (new String (response.body) === "Don't push your luck!")
    }
  }
}
