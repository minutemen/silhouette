package silhouette.akka.http

import akka.http.scaladsl.model.{HttpHeader, HttpResponse}
import akka.http.scaladsl.model.headers.{RawHeader, `Set-Cookie`}
import silhouette.akka.http.conversions.HttpCookieConversion._
import silhouette.http.{Cookie, ResponsePipeline}

case class AkkaHttpResponsePipeline(response: HttpResponse) extends ResponsePipeline[HttpResponse] {

  private def isCookie: HttpHeader => Boolean = _.is(`Set-Cookie`.lowercaseName)

  /**
    * Gets all headers.
    *
    * The HTTP RFC2616 allows duplicate response headers with the same name. Therefore we must define a
    * header values as sequence of values.
    *
    * @see https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
    * @return All headers.
    */
  override def headers: Map[String, Seq[String]] = response.headers.foldLeft(Map(): Map[String, Seq[String]]) {
    case (acc, curr) if !isCookie(curr) =>
      val r = (curr.name(), curr.value() +: acc.getOrElse(curr.name(), Nil))
      acc + r
    case (acc, _) => acc
  }

  /**
    * Creates a new response pipeline with the given headers.
    *
    * This method must override any existing header with the same name. If multiple headers with the
    * same name are given to this method, then the values must be composed into a list.
    *
    * If a response holds the following headers, then this method must implement the following behaviour:
    * {{{
    *   Map(
    *     "TEST1" -> Seq("value1", "value2"),
    *     "TEST2" -> Seq("value1")
    *   )
    * }}}
    *
    * Append a new header:
    * {{{
    *   withHeaders("TEST3" -> "value1")
    *
    *   Map(
    *     "TEST1" -> Seq("value1", "value2"),
    *     "TEST2" -> Seq("value1"),
    *     "TEST3" -> Seq("value1")
    *   )
    * }}}
    *
    * Override the header `TEST1` with a new value:
    * {{{
    *   withHeaders("TEST1" -> "value3")
    *
    *   Map(
    *     "TEST1" -> Seq("value3"),
    *     "TEST2" -> Seq("value1")
    *   )
    * }}}
    *
    * Compose headers with the same name:
    * {{{
    *   withHeaders("TEST1" -> "value3", "TEST1" -> "value4")
    *
    *   Map(
    *     "TEST1" -> Seq("value3", "value4"),
    *     "TEST2" -> Seq("value1")
    *   )
    * }}}
    *
    * @param headers The headers to set.
    * @return A new response pipeline instance with the set headers.
    */
  override def withHeaders(headers: (String, String)*): ResponsePipeline[HttpResponse] = {
    val newHeaders = headers.map(p => RawHeader(name = p._1, value = p._2))
    val newRes = response.copy(headers = response.headers.filter(h => !headers.exists(p => h.is(p._1.toLowerCase))) ++ newHeaders)
    AkkaHttpResponsePipeline(newRes)
  }

  /**
    * Gets the list of cookies.
    *
    * @return The list of cookies.
    */
  override def cookies: Seq[Cookie] = {
    response.headers.collect {
      case `Set-Cookie`(cookie) => cookie
    }.map(c => httpCookieToCookie(c))
  }

  /**
    * Creates a new response pipeline with the given cookies.
    *
    * This method must override any existing cookie with the same name. If multiple cookies with the
    * same name are given to this method, then the last cookie in the list wins.
    *
    * If a response holds the following cookies, then this method must implement the following behaviour:
    * {{{
    *   Seq(
    *     Cookie("test1", "value1"),
    *     Cookie("test2", "value2")
    *   )
    * }}}
    *
    * Append a new cookie:
    * {{{
    *   withCookies(Cookie("test3", "value3"))
    *
    *   Seq(
    *     Cookie("test1", "value1"),
    *     Cookie("test2", "value2"),
    *     Cookie("test3", "value3")
    *   )
    * }}}
    *
    * Override the cookie `test1`:
    * {{{
    *   withCookies(Cookie("test1", "value3"))
    *
    *   Seq(
    *     Cookie("test1", "value3"),
    *     Cookie("test2", "value2")
    *   )
    * }}}
    *
    * Use the last cookie if multiple cookies with the same name are given:
    * {{{
    *   withCookies(Cookie("test1", "value3"), Cookie("test1", "value4"))
    *
    *   Seq(
    *     Cookie("test1", "value4"),
    *     Cookie("test2", "value2")
    *   )
    * }}}
    *
    * @param cookies The cookies to set.
    * @return A new response pipeline instance with the set cookies.
    */
  override def withCookies(cookies: Cookie*): ResponsePipeline[HttpResponse] = {
    val httpCookies = cookies.foldRight(List.empty[Cookie]) {
      case (c, Nil) => c :: Nil
      case (c, acc) if acc.exists(_.name == c.name) => acc
      case (c, acc) => c :: acc
    }
    val newCookie = (this.cookies.filter(c => !httpCookies.exists(_.name == c.name)) ++ httpCookies).map(cookieToHttpCookie)
    AkkaHttpResponsePipeline(response.withHeaders(response.headers.filterNot(isCookie) ++ newCookie.map(c => `Set-Cookie`(c))))
  }

  /**
    * Gets the session data.
    *
    * @return The session data.
    */
  override def session: Map[String, String] = ???

  /**
    * Creates a new response pipeline with the given session data.
    *
    * This method must override any existing session data with the same name. If multiple session data with the
    * same key are given to this method, then the last session data in the list wins.
    *
    * If a response holds the following session data, then this method must implement the following behaviour:
    * {{{
    *   Map(
    *     "test1" -> "value1",
    *     "test2" -> "value2"
    *   )
    * }}}
    *
    * Append new session data:
    * {{{
    *   withSession("test3" -> "value3")
    *
    *   Map(
    *     "test1" -> "value1",
    *     "test2" -> "value2",
    *     "test3" -> "value3"
    *   )
    * }}}
    *
    * Override the session data with the key `test1`:
    * {{{
    *   withSession("test1" -> "value3")
    *
    *   Map(
    *     "test1" -> "value3",
    *     "test2" -> "value2"
    *   )
    * }}}
    *
    * Use the last session data if multiple session data with the same key are given:
    * {{{
    *   withSession("test1" -> "value3", "test1" -> "value4")
    *
    *   Map(
    *     "test1" -> "value4",
    *     "test2" -> "value2"
    *   )
    * }}}
    *
    * @param data The session data to set.
    * @return A new response pipeline instance with the set session data.
    */
  override def withSession(data: (String, String)*): ResponsePipeline[HttpResponse] = ???

  /**
    * Creates a new response pipeline without the given session keys.
    *
    * @param keys The session keys to remove.
    * @return A new response pipeline instance with the removed session data.
    */
  override def withoutSession(keys: String*): ResponsePipeline[HttpResponse] = ???

  /**
    * Unboxes the framework specific response implementation.
    *
    * @return The framework specific response implementation.
    */
  override def unbox: HttpResponse = response

  /**
    * Touches a response.
    *
    * @return A touched response pipeline.
    */
  override protected[silhouette] def touch: ResponsePipeline[HttpResponse] = {
    new AkkaHttpResponsePipeline(response) {
      override protected[silhouette] val touched = true
    }
  }
}
