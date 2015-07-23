package mockability.utils

import org.scalatest.path
import play.api.mvc.{Request, AnyContent}
import org.mockito.Mockito._

/**
 * Created by dnwiebe on 7/16/15.
 */
class DifferentiatorTest extends path.FunSpec {
  describe ("Constructing a Differentiator out of an IP, a method and a uri") {
    val subject = Differentiator ("IP", "method", "uRi")

    it ("works") {
      assert (subject.ip === "IP")
      assert (subject.method === "METHOD")
      assert (subject.uri === "/uRi")
    }
  }

  describe ("Constructing a Differentiator out of a request") {
    val request = mock (classOf[Request[AnyContent]])
    when (request.remoteAddress).thenReturn ("IP")
    when (request.method).thenReturn ("method")
    when (request.uri).thenReturn ("/uRi")
    val subject = Differentiator (request)

    it ("works") {
      assert (subject.ip === "IP")
      assert (subject.method === "METHOD")
      assert (subject.uri === "/uRi")
    }
  }

  describe ("A Differentiator") {
    val subject = Differentiator ("IP", "method", "/uri")

    describe ("when compared with other Differentiators") {
      val a = Differentiator ("IP", "method", "uri")
      val b = Differentiator ("IP", "METHOD", "uri")
      val c = Differentiator ("PI", "method", "uri")
      val d = Differentiator ("IP", "thodme", "uri")
      val e = Differentiator ("IP", "method", "iur")

      it ("does business equals correctly") {
        assert (subject.equals (null) === false)
        assert (subject.equals ("blah") === false)
        assert (subject.equals (subject) === true)
        assert (subject.equals (a) === true)
        assert (subject.equals (b) === true)
        assert (subject.equals (c) === false)
        assert (subject.equals (d) === false)
        assert (subject.equals (e) === false)
      }

      it ("does hash codes correctly") {
        assert (subject.hashCode () === subject.hashCode ())
        assert (subject.hashCode () === a.hashCode ())
        assert (subject.hashCode () === b.hashCode ())
        assert (subject.hashCode () !== c.hashCode ())
        assert (subject.hashCode () !== d.hashCode ())
        assert (subject.hashCode () !== e.hashCode ())
      }
    }

    describe ("when converted into a string") {
      val result = subject.toString ()

      it ("is as expected") {
        assert (result === "IP: METHOD '/uri'")
      }
    }
  }
}
