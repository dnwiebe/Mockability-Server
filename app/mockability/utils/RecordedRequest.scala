package mockability.utils

import mockability.utils.Utils._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsString, Json, JsValue}
import play.api.mvc.{AnyContent, Request}

/**
 * Created by dnwiebe on 7/18/15.
 */

object RecordedRequest {

  private val encoder = new sun.misc.BASE64Encoder ()

  def apply (method: String, uri: String, headers: List[(String, String)], body: Array[Byte]): RecordedRequest = {
    new RecordedRequest (method.toUpperCase, uri, headers, body)
  }

  def apply (request: Request[AnyContent]): RecordedRequest = {
    val method = request.method.toUpperCase
    val uri = request.uri
    val headers = request.headers.keys.flatMap {key =>
      request.headers.getAll (key).map {value => (key, value)}
    }.toList
    val body = request.body.asRaw match {
      case Some (buf) => buf.asBytes (buf.size.toInt).get
      case None => Array[Byte] ()
    }
    new RecordedRequest (method, uri, headers, body)
  }
}

class RecordedRequest (val method: String, val uri: String, val headers: List[(String, String)], val body: Array[Byte]) {
  import RecordedRequest._

  def toJson: JsValue = {
    var json = Json.obj (
      "method" -> method,
      "uri" -> uri
    )
    if (headers.nonEmpty) {
      val headerArray = headers.map {header => Json.obj ("name" -> header._1, "value" -> header._2)}
      val wrappers = headerArray.map {Json.toJsFieldJsValueWrapper(_)}
      json = json + (("headers", Json.arr (wrappers:_*)))
    }
    if (body.length > 0) {
      json = json + (("body", JsString (encoder.encode (body))))
    }
    json
  }
}
