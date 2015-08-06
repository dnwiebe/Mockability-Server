package controllers

import mockability.utils.{RecordedRequest, RecordedResponse, Differentiator}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.mvc._
import mockability.utils.Utils._

import scala.collection.mutable.ListBuffer

/**
 * Created by dnwiebe on 7/16/15.
 */

object MockController extends Controller with MockController

trait MockController {
  this: Controller =>

  case class RecordValue (
    timestamp: Long,
    requests: ListBuffer[RecordedRequest],
    responses: ListBuffer[RecordedResponse]
  )

  private val recordedResponses = scala.collection.mutable.Map[Differentiator, RecordValue] ()

  def prepare (ignored: String) = Action {request =>
    extractMethodAndUri (request) match {
      case None => TEST_DRIVE_ME
      case Some ((method, uri)) => {
        request.body.asJson match {
          case Some (jsValue) => {
            recordResponses (method, uri, request, jsValue)
            Ok (makeResponseBody (recordedResponses))
          }
          case None => BadRequest (makeBadPrepareResponseBody (request.body.asText))
        }
      }
    }
  }

  def clear (ignored: String) = Action {request =>
    extractMethodAndUri (request) match {
      case None => {
        val toRemove = recordedResponses.filter {pair =>
          val (key, _) = pair
          key.ip == request.remoteAddress
        }.keys
        toRemove.foreach {key => recordedResponses.remove (key)}
      }
      case Some ((method, uri)) => {
        recordedResponses.remove (Differentiator (request.remoteAddress, method.toUpperCase, uri))
      }
    }
    Ok (makeResponseBody (recordedResponses))
  }

  def report (ignored: String) = Action {request =>
    extractMethodAndUri (request) match {
      case None => TEST_DRIVE_ME
      case Some ((method, uri)) => {
        recordedResponses.get (Differentiator (request.remoteAddress, method, uri)) match {
          case Some (recordValue) => {
            val jsRecordedRequests = recordValue.requests.map {_.toJson}
            val wrappers = jsRecordedRequests.map {Json.toJsFieldJsValueWrapper(_)}
            Ok (Json.arr (wrappers:_*))
          }
          case None => {
            Result (
              header = ResponseHeader (499, Map (CONTENT_TYPE -> "text/plain")),
              body = Enumerator (requestMessage499 (request).getBytes)
            )
          }
        }
      }
    }
  }

  def respond (ignored: String) = Action {request =>
    val differentiator = Differentiator (request)
    retrieveNextRecordedResponse (differentiator) match {
      case Some (recordedResponse) => {
        recordedResponses (differentiator).requests += RecordedRequest (request)
        Result (
          header = ResponseHeader (recordedResponse.status, recordedResponse.headers.toMap),
          body = Enumerator (recordedResponse.body)
        )
      }
      case None => Result (
        header = ResponseHeader (499, Map (CONTENT_TYPE -> "text/plain")),
        body = Enumerator (responseMessage499 (request).getBytes)
      )
    }
  }

  private def extractMethodAndUri[T] (request: Request[T]): Option[(String, String)] = {
    val components = request.uri.toString.split ("/", 4)
    if (components.length < 4) {
      None
    }
    else {
      val method = components(2)
      val uri = components(3)
      Some ((method, uri))
    }
  }

  private def responseMessage499[T] (request: Request[T]): String = {
    val demand = Differentiator (request)
    val preparations: Seq[Differentiator] = sortedDifferentiators (recordedResponses)
      .filter {recordedResponses(_).responses.nonEmpty}
    val demandText = s"\nResponse was demanded for:\n${demand}\n\n"
    val preparationText = "Responses are prepared only for:\n" + (preparations match {
      case ps if ps.isEmpty => "No responses are prepared."
      case ps => ps.map {p => s"${p} (${recordedResponses (p).responses.size})"}.mkString ("\n")
    })
    demandText + preparationText + "\n"
  }

  private def requestMessage499[T] (request: Request[T]): String = {
    extractMethodAndUri (request) match {
      case None => TEST_DRIVE_ME
      case Some ((method, uri)) => {
        val demand = Differentiator (request.remoteAddress, method, uri)
        val recordings: Seq[Differentiator] = sortedDifferentiators (recordedResponses)
          .filter {recordedResponses(_).requests.nonEmpty}
        val demandText = s"\nReport was demanded for:\n${demand}\n\n"
        val recordingText = "Reports are prepared only for:\n" + (recordings match {
          case ps if ps.isEmpty => "No reports were prepared."
          case ps => ps.map {p => s"${p} (${recordedResponses (p).requests.size})"}.mkString ("\n")
        })
        demandText + recordingText + "\n"
      }
    }
  }

  private def retrieveNextRecordedResponse (differentiator: Differentiator): Option[RecordedResponse] = {
    recordedResponses.get (differentiator) match {
      case Some (recordValue) => recordValue.responses match {
        case l if l.nonEmpty => Some (l.remove (0))
        case _ => None
      }
      case None => None
    }
  }

  private def recordResponses (method: String, uri: String, request: Request[AnyContent], jsValue: JsValue) {
    val differentiator = Differentiator (request.remoteAddress, method.toUpperCase, uri)
    val recordedResponseList = RecordedResponse.makeList (jsValue)
    val buf = recordedResponses.get (differentiator) match {
      case Some (recordValue) => recordValue.responses
      case None => {
        val recordValue = RecordValue (0L, new ListBuffer[RecordedRequest](), new ListBuffer[RecordedResponse]())
        recordedResponses.put (differentiator, recordValue)
        recordValue.responses
      }
    }
    buf ++= recordedResponseList
  }

  private def makeBadPrepareResponseBody (requestBodyOpt: Option[String]): String = {
    s"""
      |Error: Body format was unexpected:
      |${requestBodyOpt.getOrElse (" <no body supplied> ")}
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
      |""".stripMargin
  }

  private def makeResponseBody (recordedResponses: scala.collection.mutable.Map[Differentiator, RecordValue]): String = {
    sortedDifferentiators (recordedResponses).map {d =>
      val buf = new StringBuilder ()
      buf
        .append ("======\n")
        .append (s"${d.method} '${d.uri}'\n")
        .append ("======")
        .append (recordedResponses (d).responses.map {rr => rr.toString}.mkString (""))
      buf.toString ()
    }.mkString ("")
  }

  private def sortedDifferentiators (recordedResponses: scala.collection.mutable.Map[Differentiator, RecordValue]): Seq[Differentiator] = {
    recordedResponses.keySet.toList.sortWith {(a, b) =>
      if (a.uri == b.uri) {
        a.method < b.method
      }
      else {
        a.uri < b.uri
      }
    }
  }
}
