package mockability.utils

import play.api.libs.json.{JsArray, JsValue}
import sun.misc.BASE64Decoder

/**
 * Created by dnwiebe on 7/16/15.
 */

object RecordedResponse {
  private val decoder = new BASE64Decoder ()

  def apply (status: Int, headers: List[(String, String)], body: Array[Byte]): RecordedResponse = {
    new RecordedResponse (status, headers, body)
  }

  def makeList (jsValue: JsValue): List[RecordedResponse] = {
    jsValue.asInstanceOf[JsArray].value.map {jsObj =>
      val status = (jsObj \ "status").as[Int]
      val headers = (jsObj \ "headers").asInstanceOf[JsArray].value.map {hdr =>
        val name = (hdr \ "name").as[String]
        val value = (hdr \ "value").as[String]
        (name, value)
      }.toList
      val bodyEncodedOpt = (jsObj \ "body").asOpt[String]
      val body = bodyEncodedOpt match {
        case Some (be) => decoder.decodeBuffer (be)
        case None => Array[Byte] ()
      }
      RecordedResponse (status, headers, body)
    }.toList
  }
}

class RecordedResponse (val status: Int, val headers: List[(String, String)], val body: Array[Byte]) {

  override def toString = {
    val buf = new StringBuilder ()
    buf
      .append ("\n------\n")
      .append (s"HTTP/1.x ${status} xxx\n")
      .append (headers.map {pair => s"${pair._1}: ${pair._2}\n"}.mkString (""))
      .append ("\n")
    if (body.length > 0) {
      buf.append (s"${new String (body)}\n")
    }
    buf.append ("------\n")
    buf.toString ()
  }
}
