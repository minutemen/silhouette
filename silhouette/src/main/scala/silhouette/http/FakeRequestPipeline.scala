/**
 * Licensed to the Minutemen Group under one or more contributor license
 * agreements. See the COPYRIGHT file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package silhouette.http

import java.net.URLEncoder

/**
 * A fake request implementation.
 *
 * @param headers     The headers.
 * @param cookies     The cookies.
 * @param session     The session.
 * @param queryParams The query params
 */
protected[silhouette] case class FakeRequest(
  headers: Map[String, Seq[String]] = Map(),
  cookies: Seq[Cookie] = Seq(),
  session: Map[String, String] = Map(),
  queryParams: Map[String, Seq[String]] = Map())

/**
 * The request pipeline implementation based on the `FakeRequest`.
 *
 * @param request The request this pipeline handles.
 */
protected[silhouette] case class FakeRequestPipeline(request: FakeRequest = FakeRequest())
  extends RequestPipeline[FakeRequest] {

  /**
   * Helper which converts a `List[(String, String)]` into a `Map[String, List[String]]`.
   */
  private implicit class Pair(p: List[(String, String)]) {
    def toMultiMap: Map[String, List[String]] = p.groupBy(_._1).mapValues(_.map(_._2))
  }

  /**
   * Gets all headers.
   *
   * @return All headers.
   */
  override def headers: Map[String, Seq[String]] = request.headers

  /**
   * Creates a new request pipeline with the given headers.
   *
   * @inheritdoc
   *
   * @param headers The headers to set.
   * @return A new request pipeline instance with the set headers.
   */
  override def withHeaders(headers: (String, String)*): RequestPipeline[FakeRequest] = {
    val newHeaders = headers.toList.toMultiMap.foldLeft(request.headers) {
      case (h, (k, v)) =>
        h + (k -> v)
    }

    copy(request.copy(headers = newHeaders))
  }

  /**
   * Gets the list of cookies.
   *
   * @return The list of cookies.
   */
  override def cookies: Seq[Cookie] = request.cookies

  /**
   * Creates a new request pipeline with the given cookies.
   *
   * @inheritdoc
   *
   * @param cookies The cookies to set.
   * @return A new request pipeline instance with the set cookies.
   */
  override def withCookies(cookies: Cookie*): RequestPipeline[FakeRequest] = {
    val filteredCookies = cookies.groupBy(_.name).map(_._2.last)
    val newCookies = filteredCookies.foldLeft(request.cookies) {
      case (l, c) =>
        l.indexWhere(_.name == c.name) match {
          case -1 => l :+ c
          case i  => l.patch(i, Seq(c), 1)
        }
    }

    copy(request.copy(cookies = newCookies))
  }

  /**
   * Gets the session data.
   *
   * @return The session data.
   */
  override def session: Map[String, String] = request.session

  /**
   * Creates a new request pipeline with the given session data.
   *
   * @inheritdoc
   *
   * @param data The session data to set.
   * @return A new request pipeline instance with the set session data.
   */
  override def withSession(data: (String, String)*): RequestPipeline[FakeRequest] = {
    val filteredData = data.groupBy(_._1).map(_._2.last)
    val newData = filteredData.foldLeft(request.session) {
      case (d, (k, v)) =>
        d + (k -> v)
    }

    copy(request.copy(session = newData))
  }

  /**
   * Gets the raw query string.
   *
   * @return The raw query string.
   */
  override def rawQueryString: String = {
    queryParams.foldLeft(Seq[String]()) {
      case (s, (k, v)) =>
        s :+ v.map(URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(_, "UTF-8")).mkString("&")
    }.mkString("&")
  }

  /**
   * Gets all query params.
   *
   * @return All query params.
   */
  def queryParams: Map[String, Seq[String]] = request.queryParams

  /**
   * Creates a new request pipeline with the given query params.
   *
   * @inheritdoc
   *
   * @param params The query params to set.
   * @return A new request pipeline instance with the set query params.
   */
  override def withQueryParams(params: (String, String)*): RequestPipeline[FakeRequest] = {
    val newParams = params.toList.toMultiMap.foldLeft(request.queryParams) {
      case (h, (k, v)) =>
        h + (k -> v)
    }

    copy(request.copy(queryParams = newParams))
  }

  /**
   * Unboxes the request this pipeline handles.
   *
   * @return The request this pipeline handles.
   */
  override def unbox: FakeRequest = request

  /**
   * The request body extractor used to extract values from request body.
   */
  override val bodyExtractor: RequestBodyExtractor[FakeRequest] = new RequestBodyExtractor[FakeRequest] {

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
