package silhouette.akka.http

import akka.http.scaladsl.model.{HttpHeader, HttpRequest, Uri}
import akka.http.scaladsl.model.headers.RawHeader
import silhouette.http.{Cookie, RequestBodyExtractor, RequestPipeline}
import silhouette.akka.http.conversions.HttpCookieConversion._


case class AkkaHttpRequestPipeline(request: HttpRequest) extends RequestPipeline[HttpRequest] {

  private def isCookie: HttpHeader => Boolean = _.is(akka.http.scaladsl.model.headers.Cookie.lowercaseName)

  /**
    * Gets all headers.
    *
    * The HTTP RFC2616 allows duplicate request headers with the same name. Therefore we must define a
    * header values as sequence of values.
    *
    * @see https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
    * @return All headers.
    */
  override def headers: Map[String, Seq[String]] = request.headers.foldLeft(Map(): Map[String, Seq[String]]) {
    case (acc, curr) if !isCookie(curr) =>
      val r = (curr.name(), curr.value() +: acc.getOrElse(curr.name(), Nil))
      acc + r
    case (acc, _) => acc
  }

  /**
    * Creates a new request pipeline with the given headers.
    *
    * This method must override any existing header with the same name. If multiple headers with the
    * same name are given to this method, then the values must be composed into a list.
    *
    * If a request holds the following headers, then this method must implement the following behaviour:
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
    * @return A new request pipeline instance with the set headers.
    */
  override def withHeaders(headers: (String, String)*): RequestPipeline[HttpRequest] = {
    val newHeaders = headers.map(p => RawHeader(name = p._1, value = p._2))
    val newReq = request.copy(headers = request.headers.filter(h => !headers.exists(p => h.is(p._1.toLowerCase))) ++ newHeaders)
    AkkaHttpRequestPipeline(newReq)
  }

  /**
    * Gets the list of cookies.
    *
    * @return The list of cookies.
    */
  override def cookies: Seq[Cookie] = request.cookies.map(c => httpCookieToCookie(c.toCookie()))

  /**
    * Creates a new request pipeline with the given cookies.
    *
    * This method must override any existing cookie with the same name. If multiple cookies with the
    * same name are given to this method, then the last cookie in the list wins.
    *
    * If a request holds the following cookies, then this method must implement the following behaviour:
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
    * @return A new request pipeline instance with the set cookies.
    */
  override def withCookies(cookies: Cookie*): RequestPipeline[HttpRequest] = {
    val httpCookies = cookies.foldRight(List.empty[Cookie]) {
      case (c, Nil) => c :: Nil
      case (c, acc) if acc.exists(_.name == c.name) => acc
      case (c, acc) => c :: acc
    }
    val newCookie = request.cookies.filter(c => !httpCookies.exists(_.name == c.name)).map(_.toCookie()) ++ httpCookies.map(cookieToHttpCookie)
    AkkaHttpRequestPipeline(request.withHeaders(request.headers.filterNot(isCookie) ++ newCookie.map(c => akka.http.scaladsl.model.headers.`Cookie`(c.pair()))))
  }

  /**
    * Gets the session data.
    *
    * @return The session data.
    */
  override def session: Map[String, String] = ???

  /**
    * Creates a new request pipeline with the given session data.
    *
    * This method must override any existing session data with the same name. If multiple session data with the
    * same key are given to this method, then the last session data in the list wins.
    *
    * If a request holds the following session data, then this method must implement the following behaviour:
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
    * @return A new request pipeline instance with the set session data.
    */
  override def withSession(data: (String, String)*): RequestPipeline[HttpRequest] = ???

  /**
    * Gets the raw query string.
    *
    * @return The raw query string.
    */
  override def rawQueryString: String = request.uri.rawQueryString.getOrElse("")

  /**
    * Gets all query params.
    *
    * While there is no definitive standard, most web frameworks allow duplicate params with the
    * same name. Therefore we must define a query param values as sequence of values.
    *
    * @return All query params.
    */
  override def queryParams: Map[String, Seq[String]] = request.uri.query().toMultiMap

  /**
    * Creates a new request pipeline with the given query params.
    *
    * This method must override any existing query param with the same name. If multiple query params with the
    * same name are given to this method, then the values must be composed into a list.
    *
    * If a request holds the following query params, then this method must implement the following behaviour:
    * {{{
    *   Map(
    *     "TEST1" -> Seq("value1", "value2"),
    *     "TEST2" -> Seq("value1")
    *   )
    * }}}
    *
    * Append a new query param:
    * {{{
    *   withQueryParams("test3" -> "value1")
    *
    *   Map(
    *     "test1" -> Seq("value1", "value2"),
    *     "test2" -> Seq("value1"),
    *     "test3" -> Seq("value1")
    *   )
    * }}}
    *
    * Override the query param `test1` with a new value:
    * {{{
    *   withQueryParams("test1" -> "value3")
    *
    *   Map(
    *     "test1" -> Seq("value3"),
    *     "test2" -> Seq("value1")
    *   )
    * }}}
    *
    * Compose query params with the same name:
    * {{{
    *   withQueryParams("test1" -> "value3", "test1" -> "value4")
    *
    *   Map(
    *     "test1" -> Seq("value3", "value4"),
    *     "test2" -> Seq("value1")
    *   )
    * }}}
    *
    * @param params The query params to set.
    * @return A new request pipeline instance with the set query params.
    */
  override def withQueryParams(params: (String, String)*): RequestPipeline[HttpRequest] = {
    val newQueryParams = request.uri.query().filter(p => !params.exists(_._1 == p._1)).toList ++ params
    AkkaHttpRequestPipeline(request.withUri(request.uri.withQuery(Uri.Query(newQueryParams: _*))))
  }

  /**
    * Unboxes the framework specific request implementation.
    *
    * @return The framework specific request implementation.
    */
  override def unbox: HttpRequest = request

  override val bodyExtractor: RequestBodyExtractor[HttpRequest] = new RequestBodyExtractor[HttpRequest] {

    /**
      * Extracts a value from Json body.
      *
      * @param name The name of the value to extract.
      * @return [[BodyValue]]
      */
    override def fromJson(name: String): BodyValue = None

    /**
      * Extracts a value from Xml body.
      *
      * @param name The name of the value to extract.
      * @return [[BodyValue]]
      */
    override def fromXml(name: String): BodyValue = None

    /**
      * Extracts a value from form url encoded body.
      *
      * @param name The name of the value to extract.
      * @return [[BodyValue]]
      */
    override def fromFormUrlEncoded(name: String): BodyValue = None
  }
}
