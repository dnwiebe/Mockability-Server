package mockability.utils

import play.api.mvc.Request

/**
 * Created by dnwiebe on 7/16/15.
 */

object Differentiator {

  def apply (ip: String, method: String, uri: String): Differentiator = {
    new Differentiator (ip, method.toUpperCase, ensureLeadingSlash (uri))
  }

  def apply (request: Request[_]): Differentiator = {
    new Differentiator (request.remoteAddress, request.method.toUpperCase, ensureLeadingSlash (request.uri))
  }

  private def ensureLeadingSlash (uri: String): String = {
    if (uri.startsWith ("/")) uri else s"/${uri}"
  }
}

class Differentiator (val ip: String, val method: String, val uri: String) {
  override def equals (o: Any): Boolean = {
    o match {
      case null => false
      case that: Differentiator => (this.ip == that.ip) && (this.method == that.method) && (this.uri == that.uri)
      case _ => false
    }
  }

  override def hashCode (): Int = {
    ip.hashCode () + method.hashCode () + uri.hashCode ();
  }

  override def toString: String = {
    s"${ip}: ${method} '${uri}'"
  }
}
