package silhouette.akka.http

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.model.headers.{HttpCookie, RawHeader, `Set-Cookie`}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.http.Cookie

/**
  * Test case for the [[AkkaHttpRequestPipeline]] class.
  */
class AkkaHttpRequestPipelineSpec extends Specification {

  "The `headers` method" should {
    "return all headers" in new Context {
      requestPipeline.headers.map(_._2.sorted) must be equalTo headers.map(_._2.sorted)
    }
  }

  "The `header` method" should {
    "return the list of header values" in new Context {
      requestPipeline.header("TEST1") must contain(exactly("value1", "value2"))
    }

    "return an empty list if no header with the given name was found" in new Context {
      requestPipeline.header("TEST3") must beEmpty
    }
  }

  "The `withHeaders` method" should {
    "append a new header" in new Context {
      requestPipeline.withHeaders("TEST3" -> "value1").headers must be equalTo Map(
        "TEST1" -> Seq("value2", "value1"),
        "TEST2" -> Seq("value1"),
        "TEST3" -> Seq("value1")
      )
    }

    "append multiple headers" in new Context {
      requestPipeline.withHeaders("TEST3" -> "value1", "TEST4" -> "value1").headers must be equalTo Map(
        "TEST1" -> Seq("value2", "value1"),
        "TEST2" -> Seq("value1"),
        "TEST3" -> Seq("value1"),
        "TEST4" -> Seq("value1")
      )
    }

    "append multiple headers with the same name" in new Context {
      requestPipeline.withHeaders("TEST3" -> "value1", "TEST3" -> "value2").headers must be equalTo Map(
        "TEST1" -> Seq("value2", "value1"),
        "TEST2" -> Seq("value1"),
        "TEST3" -> Seq("value2", "value1")
      )
    }

    "override an existing header" in new Context {
      requestPipeline.withHeaders("TEST2" -> "value2", "TEST2" -> "value3").headers must be equalTo Map(
        "TEST1" -> Seq("value2", "value1"),
        "TEST2" -> Seq("value3", "value2")
      )
    }

    "override multiple existing headers" in new Context {
      requestPipeline.withHeaders("TEST1" -> "value3", "TEST2" -> "value2").headers must be equalTo Map(
        "TEST1" -> Seq("value3"),
        "TEST2" -> Seq("value2")
      )
    }
  }

  "The `cookies` method" should {
    "return all cookies" in new Context {
      requestPipeline.cookies must be equalTo cookies
    }
  }

  "The `cookie` method" should {
    "return some cookie for the given name" in new Context {
      requestPipeline.cookie("test1") must beSome(Cookie("test1", "value1"))
    }

    "return None if no cookie with the given name was found" in new Context {
      requestPipeline.cookie("test3") must beNone
    }
  }

  "The `withCookies` method" should {
    "append a new cookie" in new Context {
      requestPipeline.withCookies(Cookie("test3", "value3")).cookies must be equalTo Seq(
        Cookie("test1", "value1"),
        Cookie("test2", "value2"),
        Cookie("test3", "value3")
      )
    }

    "override an existing cookie" in new Context {
      requestPipeline.withCookies(Cookie("test1", "value3")).cookies must contain(exactly(
        Cookie("test1", "value3"),
        Cookie("test2", "value2")
      ))
    }

    "use the last cookie if multiple cookies with the same name are given" in new Context {
      requestPipeline.withCookies(Cookie("test1", "value3"), Cookie("test1", "value4")).cookies must contain(exactly(
        Cookie("test1", "value4"),
        Cookie("test2", "value2")
      ))
    }
  }

  "The `session` method" should {
    "return all session data" in new Context {
      requestPipeline.session must be equalTo session
    }
  }

  "The `withSession` method" should {
    "append new session data" in new Context {
      requestPipeline.withSession("test3" -> "value3").session must be equalTo Map(
        "test1" -> "value1",
        "test2" -> "value2",
        "test3" -> "value3"
      )
    }

    "override existing session data" in new Context {
      requestPipeline.withSession("test1" -> "value3").session must be equalTo Map(
        "test1" -> "value3",
        "test2" -> "value2"
      )
    }

    "use the last session data if multiple session data with the same name are given" in new Context {
      requestPipeline.withSession("test1" -> "value3", "test1" -> "value4").session must be equalTo Map(
        "test1" -> "value4",
        "test2" -> "value2"
      )
    }
  }

  "The `rawQueryString` method" should {
    "return the raw query string" in new Context {
      requestPipeline.rawQueryString must be equalTo "test1=value1&test1=value2&test2=value1"
    }

    "be URL encoded" in new Context {
      requestPipeline.withQueryParams("test=3" -> "value=4").rawQueryString must be equalTo
        "test1=value1&test1=value2&test2=value1&test%3D3=value%3D4"
    }
  }

  "The `queryParams` method" should {
    "return all query params" in new Context {
      requestPipeline.queryParams.map(_._2.sorted) must be equalTo queryParams.map(_._2.sorted)
    }
  }

  "The `queryParam` method" should {
    "return the list of query params" in new Context {
      requestPipeline.queryParam("test1") must contain(exactly("value1", "value2"))
    }

    "return an empty list if no query param with the given name was found" in new Context {
      requestPipeline.queryParam("test3") must beEmpty
    }
  }

  "The `withQueryParams` method" should {
    "append a new header" in new Context {
      requestPipeline.withQueryParams("test3" -> "value1").queryParams must be equalTo Map(
        "test1" -> Seq("value2", "value1"),
        "test2" -> Seq("value1"),
        "test3" -> Seq("value1")
      )
    }

    "append multiple headers" in new Context {
      requestPipeline.withQueryParams("test3" -> "value1", "test4" -> "value1").queryParams must be equalTo Map(
        "test1" -> Seq("value2", "value1"),
        "test2" -> Seq("value1"),
        "test3" -> Seq("value1"),
        "test4" -> Seq("value1")
      )
    }

    "append multiple headers with the same name" in new Context {
      requestPipeline.withQueryParams("test3" -> "value1", "test3" -> "value2").queryParams must be equalTo Map(
        "test1" -> Seq("value2", "value1"),
        "test2" -> Seq("value1"),
        "test3" -> Seq("value2", "value1")
      )
    }

    "override an existing header" in new Context {
      requestPipeline.withQueryParams("test2" -> "value2", "test2" -> "value3").queryParams must be equalTo Map(
        "test1" -> Seq("value2", "value1"),
        "test2" -> Seq("value3", "value2")
      )
    }

    "override multiple existing headers" in new Context {
      requestPipeline.withQueryParams("test1" -> "value3", "test2" -> "value2").queryParams must be equalTo Map(
        "test1" -> Seq("value3"),
        "test2" -> Seq("value2")
      )
    }
  }

  "The `unbox` method" should {
    "return the handled request" in new Context {
      requestPipeline.unbox must be equalTo request
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
    val queryParams = Map(
      "test1" -> Seq("value1", "value2"),
      "test2" -> Seq("value1")
    )

    val akkaHeaders = headers.flatMap(p => p._2.map(v => RawHeader(p._1, v))).toList
    val akkaCookie = cookies.map(c => akka.http.scaladsl.model.headers.`Cookie`(c.name, c.value))
    val akkaQueryParams = Query(queryParams.map{case (name, values) => values.map(v => name -> v)}.flatten.toList: _*)
    val request = HttpRequest(
      headers = akkaHeaders ++ akkaCookie,
      uri = Uri().withQuery(akkaQueryParams)
    )

    /**
      * A request pipeline which handles a fake request.
      */
    val requestPipeline = AkkaHttpRequestPipeline(request)
  }
}

