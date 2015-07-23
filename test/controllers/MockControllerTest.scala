package controllers

import org.scalatest.path
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.{Request, AnyContentAsEmpty, Controller}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}

/**
 * Created by dnwiebe on 7/16/15.
 */
class MockControllerTest extends path.FunSpec {

  class TestMockController extends Controller with MockController {}

  describe ("A MockController") {
    val subject = new TestMockController ()

    describe ("sent a 'prepare' order with bad JSON") {
      val prepareRequest = FakeRequest ("POST", "/mockability/get/wurbly/woo?a=b&c=d")
        .withHeaders ((CONTENT_TYPE, "application/json"))
        .withTextBody ("biddly [")
      val prepareResponse = subject.prepare (null) (prepareRequest)

      it ("complains") {
        assert (status (prepareResponse) === 400)
        assert (contentAsString (prepareResponse) ===
          """
            |Error: Body format was unexpected:
            |biddly [
            |---
            |Usage:
            |POST /mockability/<method>/<uri>
            |Content-Type: application/json
            |
            |<responses to prepare>
            |---
            |method:
            |one of: GET, POST, PUT, DELETE, HEAD (case doesn't matter)
            |
            |uri:
            |absolute URI to prepare for.  For example, to prepare for https://www.youtube.com/watch?v=rGCTLJDoMGw,
            |this should be "watch?v=rGCTLJDoMGw".
            |
            |responses to prepare (must be an array, even if of only one response):
            |[<response to prepare>, <response to prepare> ... ]
            |---
            |response to prepare:
            |{
            |  "status": <status>
            |  "headers": [
            |    {"name": <header-name>, "value": <header-value>},
            |    {"name": <header-name>, "value": <header-value>}
            |  ],
            |  "body": <Base64-encoded body>
            |}
            |""".stripMargin)
      }
    }

    describe ("sent a good 'prepare' order") {
      val prepareRequest = FakeRequest ("POST", "/mockability/get/wurbly/woo?a=b&c=d")
        .withHeaders ((CONTENT_TYPE, "application/json"))
        .withJsonBody (Json.parse ("""
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
        """))
      val prepareResponse = subject.prepare (null) (prepareRequest)

      it ("responds with a 200 and a list of prepared responses") {
        val content = contentAsString (prepareResponse)
        assert (content ===
          """======
            |GET '/wurbly/woo?a=b&c=d'
            |======
            |------
            |HTTP/1.x 200 xxx
            |Content-Type: text/plain
            |Content-Length: 29
            |
            |First response to /wurbly/woo
            |------
            |
            |------
            |HTTP/1.x 401 xxx
            |Content-Type: text/plain
            |Content-Length: 21
            |
            |Don't push your luck!
            |------
            |""".stripMargin)
        assert (status (prepareResponse) === 200)
      }

      describe ("and asked for the target of a GET for a different URI") {
        val request = FakeRequest ("get", "/wurbly/waffle?a=b&c=d")
        val response = subject.respond (null) (request)

        it ("complains") {
          assert (status (response) === 499)
          assert (contentAsString (response) ===
          """
            |Response was demanded for:
            |127.0.0.1: GET '/wurbly/waffle?a=b&c=d'
            |
            |Responses are prepared only for:
            |127.0.0.1: GET '/wurbly/woo?a=b&c=d' (2)
            |""".stripMargin)
        }
      }

      describe ("and asked for the target of a POST for the same URI") {
        val request = FakeRequest ("POST", "/wurbly/woo?a=b&c=d")
        val response = subject.respond (null) (request)

        it ("complains") {
          assert (status (response) === 499)
          assert (contentAsString (response) ===
            """
              |Response was demanded for:
              |127.0.0.1: POST '/wurbly/woo?a=b&c=d'
              |
              |Responses are prepared only for:
              |127.0.0.1: GET '/wurbly/woo?a=b&c=d' (2)
              |""".stripMargin)
        }
      }

      describe ("and asked for the target of a GET for the same URI, but from a different IP address") {
        val request = FakeRequest ("get", "/wurbly/woo?a=b&c=d", FakeHeaders(), AnyContentAsEmpty, remoteAddress = "different")
        val response = subject.respond (null) (request)

        it ("complains") {
          assert (status (response) === 499)
          assert (contentAsString (response) ===
            """
              |Response was demanded for:
              |different: GET '/wurbly/woo?a=b&c=d'
              |
              |Responses are prepared only for:
              |127.0.0.1: GET '/wurbly/woo?a=b&c=d' (2)
              |""".stripMargin)
        }
      }

      describe ("and then sent a 'clear' order for that URI") {
        val request = FakeRequest ("DELETE", "/mockability/get/wurbly/woo?a=b&c=d")
        val response = subject.clear (null) (request)

        it ("responds with a 200 and a newly cleared list") {
          val content = contentAsString (response)
          assert (content === "")
          assert (status (response) === 200)
        }

        describe ("and asked for the target of a GET for the that URI") {
          val request = FakeRequest ("get", "/wurbly/woo?a=b&c=d")
          val response = subject.respond (null) (request)

          it ("complains") {
            assert (status (response) === 499)
            assert (contentAsString (response) ===
              """
                |Response was demanded for:
                |127.0.0.1: GET '/wurbly/woo?a=b&c=d'
                |
                |Responses are prepared only for:
                |No responses are prepared.
                |""".stripMargin)
          }
        }
      }

      describe ("and then sent a 'clear' order for a different URI") {
        val request = FakeRequest ("DELETE", "/mockability/get/wurbly/waffle?a=b&c=d")
        val response = subject.clear (null) (request)

        it ("responds with a 200 and an unmolested list") {
          val content = contentAsString (response)
          assert (content ===
            """======
              |GET '/wurbly/woo?a=b&c=d'
              |======
              |------
              |HTTP/1.x 200 xxx
              |Content-Type: text/plain
              |Content-Length: 29
              |
              |First response to /wurbly/woo
              |------
              |
              |------
              |HTTP/1.x 401 xxx
              |Content-Type: text/plain
              |Content-Length: 21
              |
              |Don't push your luck!
              |------
              |""".stripMargin)
          assert (status (response) === 200)
        }
      }

      describe ("and asked for the target of that order") {
        val firstRequest = FakeRequest ("get", "/wurbly/woo?a=b&c=d")
        val firstResponse = subject.respond (null) (firstRequest)

        it ("responds as directed") {
          assert (status (firstResponse) === 200)
          assert (header (CONTENT_TYPE, firstResponse) === Some ("text/plain"))
          assert (header (CONTENT_LENGTH, firstResponse) === Some ("29"))
          assert (contentAsString (firstResponse) === "First response to /wurbly/woo")
        }

        describe ("and programmed with another response") {
          val anotherPrepareRequest = FakeRequest ("POST", "/mockability/get/wurbly/woo?a=b&c=d")
            .withHeaders ((CONTENT_TYPE, "application/json"))
            .withJsonBody (Json.parse (
            """
              [
                {
                  "status": 302,
                  "headers": [
                    {"name": "Location", "value": "https://gorfblatt.com"}
                  ]
                }
              ]
            """))
          val anotherPrepareResponse = subject.prepare (null) (anotherPrepareRequest)

          it ("responds with a 200") {
            assert (contentAsString (anotherPrepareResponse) ===
              """======
                |GET '/wurbly/woo?a=b&c=d'
                |======
                |------
                |HTTP/1.x 401 xxx
                |Content-Type: text/plain
                |Content-Length: 21
                |
                |Don't push your luck!
                |------
                |
                |------
                |HTTP/1.x 302 xxx
                |Location: https://gorfblatt.com
                |
                |------
                |""".stripMargin)
            assert (status (anotherPrepareResponse) === 200)
          }

          describe ("and asked again") {
            val secondRequest = FakeRequest ("get", "/wurbly/woo?a=b&c=d")
            val secondResponse =subject.respond (null) (secondRequest)

            it ("responds as directed again") {
              assert (status (secondResponse) === 401)
              assert (header (CONTENT_TYPE, secondResponse) === Some ("text/plain"))
              assert (header (CONTENT_LENGTH, secondResponse) === Some ("21"))
              assert (contentAsString (secondResponse) === "Don't push your luck!")
            }

            describe ("and a third time") {
              val thirdRequest = FakeRequest ("get", "/wurbly/woo?a=b&c=d")
              val thirdResponse = subject.respond (null)(thirdRequest)

              it ("responds as directed again") {
                assert (status (thirdResponse) === 302)
                assert (header (LOCATION, thirdResponse) === Some ("https://gorfblatt.com"))
                assert (header (CONTENT_TYPE, thirdResponse) === None)
                assert (header (CONTENT_LENGTH, thirdResponse) === None)
                assert (contentAsString (thirdResponse) === "")
              }

              describe ("and asked one more time") {
                val fourthRequest = FakeRequest ("get", "/wurbly/woo?a=b&c=d")
                val fourthResponse = subject.respond (null)(fourthRequest)

                it ("complains this time") {
                  assert (status (fourthResponse) === 499)
                  assert (contentAsString (fourthResponse) ===
                    """
                      |Response was demanded for:
                      |127.0.0.1: GET '/wurbly/woo?a=b&c=d'
                      |
                      |Responses are prepared only for:
                      |No responses are prepared.
                      |""".stripMargin)
                }

                describe ("and asked for a report") {
                  val request = FakeRequest ("get", "/mockability/get/wurbly/woo?a=b&c=d")
                  val response = subject.report (null) (request)

                  it ("sends one back") {
                    assert (status (response) === 200)
                    assert (contentAsJson (response) ===
                      Json.arr (
                        Json.obj (
                          "method" -> "GET",
                          "uri" -> "/wurbly/woo?a=b&c=d"
                        ),
                        Json.obj (
                          "method" -> "GET",
                          "uri" -> "/wurbly/woo?a=b&c=d"
                        ),
                        Json.obj (
                          "method" -> "GET",
                          "uri" -> "/wurbly/woo?a=b&c=d"
                        )
                      )
                    )
                  }
                }

                describe ("and asked for a spurious report") {
                  val badRequest = FakeRequest ("get", "/mockability/get/wurbly/woo?a=b&c=x")
                  val badResponse = subject.report (null) (badRequest)

                  it ("complains informatively") {
                    assert (status (badResponse) === 499)
                    assert (contentAsString (badResponse) ===
                      """
                        |Report was demanded for:
                        |127.0.0.1: GET '/wurbly/woo?a=b&c=x'
                        |
                        |Reports are prepared only for:
                        |127.0.0.1: GET '/wurbly/woo?a=b&c=d' (3)
                        |""".stripMargin)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
