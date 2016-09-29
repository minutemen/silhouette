package silhouette.akka.http

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.{HttpCookie, RawHeader, `Set-Cookie`}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.http.Cookie

/**
  * Test case for the [[AkkaHttpResponsePipeline]] class.
  */
class AkkaHttpResponsePipelineSpec extends Specification {

  "The `headers` method" should {
    "return all headers" in new Context {
      responsePipeline.headers.map(_._2.sorted) must be equalTo headers.map(_._2.sorted)
    }
  }

  "The `header` method" should {
    "return the list of header values" in new Context {
      responsePipeline.header("TEST1") must contain(exactly("value1", "value2"))
    }

    "return an empty list if no header with the given name was found" in new Context {
      responsePipeline.header("TEST3") must beEmpty
    }
  }

  "The `withHeaders` method" should {
    "append a new header" in new Context {
      responsePipeline.withHeaders("TEST3" -> "value1").headers must be equalTo Map(
        "TEST1" -> Seq("value2", "value1"),
        "TEST2" -> Seq("value1"),
        "TEST3" -> Seq("value1")
      )
    }

    "append multiple headers" in new Context {
      responsePipeline.withHeaders("TEST3" -> "value1", "TEST4" -> "value1").headers must be equalTo Map(
        "TEST1" -> Seq("value2", "value1"),
        "TEST2" -> Seq("value1"),
        "TEST3" -> Seq("value1"),
        "TEST4" -> Seq("value1")
      )
    }

    "append multiple headers with the same name" in new Context {
      responsePipeline.withHeaders("TEST3" -> "value1", "TEST3" -> "value2").headers must be equalTo Map(
        "TEST1" -> Seq("value2", "value1"),
        "TEST2" -> Seq("value1"),
        "TEST3" -> Seq("value2", "value1")
      )
    }

    "override an existing header" in new Context {
      responsePipeline.withHeaders("TEST2" -> "value2", "TEST2" -> "value3").headers must be equalTo Map(
        "TEST1" -> Seq("value2", "value1"),
        "TEST2" -> Seq("value3", "value2")
      )
    }

    "override multiple existing headers" in new Context {
      responsePipeline.withHeaders("TEST1" -> "value3", "TEST2" -> "value2").headers must be equalTo Map(
        "TEST1" -> Seq("value3"),
        "TEST2" -> Seq("value2")
      )
    }
  }

  "The `cookies` method" should {
    "return all cookies" in new Context {
      responsePipeline.cookies must be equalTo cookies
    }
  }

  "The `cookie` method" should {
    "return some cookie for the given name" in new Context {
      responsePipeline.cookie("test1") must beSome(Cookie("test1", "value1"))
    }

    "return None if no cookie with the given name was found" in new Context {
      responsePipeline.cookie("test3") must beNone
    }
  }

  "The `withCookies` method" should {
    "append a new cookie" in new Context {
      responsePipeline.withCookies(Cookie("test3", "value3")).cookies must be equalTo Seq(
        Cookie("test1", "value1"),
        Cookie("test2", "value2"),
        Cookie("test3", "value3")
      )
    }

    "override an existing cookie" in new Context {
      responsePipeline.withCookies(Cookie("test1", "value3")).cookies must contain(exactly(
        Cookie("test1", "value3"),
        Cookie("test2", "value2")
      ))
    }

    "use the last cookie if multiple cookies with the same name are given" in new Context {
      responsePipeline.withCookies(Cookie("test1", "value3"), Cookie("test1", "value4")).cookies must contain(exactly(
        Cookie("test1", "value4"),
        Cookie("test2", "value2")
      ))
    }
  }

  "The `session` method" should {
    "return all session data" in new Context {
      responsePipeline.session must be equalTo session
    }
  }

  "The `withSession` method" should {
    "append new session data" in new Context {
      responsePipeline.withSession("test3" -> "value3").session must be equalTo Map(
        "test1" -> "value1",
        "test2" -> "value2",
        "test3" -> "value3"
      )
    }

    "override existing session data" in new Context {
      responsePipeline.withSession("test1" -> "value3").session must be equalTo Map(
        "test1" -> "value3",
        "test2" -> "value2"
      )
    }

    "use the last session data if multiple session data with the same name are given" in new Context {
      responsePipeline.withSession("test1" -> "value3", "test1" -> "value4").session must be equalTo Map(
        "test1" -> "value4",
        "test2" -> "value2"
      )
    }
  }

  "The `withSession` method" should {
    "remove session data" in new Context {
      responsePipeline.withoutSession("test1").session must be equalTo Map(
        "test2" -> "value2"
      )
    }

    "remove multiple keys" in new Context {
      responsePipeline.withoutSession("test1", "test2").session must beEmpty
    }
  }

  "The `unbox` method" should {
    "return the handled response" in new Context {
      responsePipeline.unbox must be equalTo response
    }
  }

  "The touch method" should {
    "touch a response" in new Context {
      responsePipeline.touch.touched must beTrue
    }
  }

  /**
    * The context.
    */
  trait Context extends Scope {

    val headers = Map(
      "TEST1" -> Seq("value1", "value2"),
      "TEST2" -> Seq("value1")
    )
    val cookies = Seq(
      Cookie("test1", "value1"),
      Cookie("test2", "value2")
    )
    val session = Map(
      "test1" -> "value1",
      "test2" -> "value2"
    )

    val akkaHeaders = headers.flatMap(p => p._2.map(v => RawHeader(p._1, v))).toList
    val akkaCookie = cookies.map(c => `Set-Cookie`(HttpCookie(c.name, c.value)))
    val response = HttpResponse(
      headers = akkaHeaders ++ akkaCookie
    )

    /**
      * A response pipeline which handles a fake response.
      */
    val responsePipeline = AkkaHttpResponsePipeline(response)
  }
}

