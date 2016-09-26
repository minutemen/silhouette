/**
 * Copyright 2016 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

/**
 * A fake response implementation.
 *
 * @param headers The headers.
 * @param cookies The cookies.
 * @param session The session.
 */
protected[silhouette] case class FakeResponse(
  headers: Map[String, Seq[String]] = Map(),
  cookies: Seq[Cookie] = Seq(),
  session: Map[String, String] = Map())

/**
 * The response pipeline implementation based on the `FakeResponse`.
 *
 * @param response The response this pipeline handles.
 */
protected[silhouette] case class FakeResponsePipeline(response: FakeResponse = FakeResponse())
  extends ResponsePipeline[FakeResponse] {

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
  override def headers: Map[String, Seq[String]] = response.headers

  /**
   * Creates a new response pipeline with the given headers.
   *
   * @inheritdoc
   *
   *  @param headers The headers to add.
   * @return A new response pipeline instance with the added headers.
   */
  override def withHeaders(headers: (String, String)*): ResponsePipeline[FakeResponse] = {
    val newHeaders = headers.toList.toMultiMap.foldLeft(response.headers) {
      case (h, (k, v)) =>
        h + (k -> v)
    }

    copy(response.copy(headers = newHeaders))
  }

  /**
   * Gets the list of cookies.
   *
   * @return The list of cookies.
   */
  override def cookies: Seq[Cookie] = response.cookies

  /**
   * Creates a new response pipeline with the given cookies.
   *
   * @inheritdoc
   *
   *  @param cookies The cookies to add.
   * @return A new response pipeline instance with the added cookies.
   */
  override def withCookies(cookies: Cookie*): ResponsePipeline[FakeResponse] = {
    val filteredCookies = cookies.groupBy(_.name).map(_._2.last)
    val newCookies = filteredCookies.foldLeft(response.cookies) {
      case (l, c) =>
        l.indexWhere(_.name == c.name) match {
          case -1 => l :+ c
          case i  => l.patch(i, Seq(c), 1)
        }
    }

    copy(response.copy(cookies = newCookies))
  }

  /**
   * Gets the session data.
   *
   * @return The session data.
   */
  override def session: Map[String, String] = response.session

  /**
   * Creates a new response pipeline with the given session data.
   *
   * @inheritdoc
   *
   * @param data The session data to add.
   * @return A new response pipeline instance with the added session data.
   */
  override def withSession(data: (String, String)*): ResponsePipeline[FakeResponse] = {
    val filteredData = data.groupBy(_._1).map(_._2.last)
    val newData = filteredData.foldLeft(response.session) {
      case (d, (k, v)) =>
        d + (k -> v)
    }

    copy(response.copy(session = newData))
  }

  /**
   * Removes session data from response.
   *
   * @param keys The session keys to remove.
   * @return A new response pipeline instance with the removed session data.
   */
  override def withoutSession(keys: String*): ResponsePipeline[FakeResponse] = {
    val newData = keys.foldLeft(response.session) {
      case (d, k) =>
        d - k
    }

    copy(response.copy(session = newData))
  }

  /**
   * Unboxes the framework specific response implementation.
   *
   * @return The framework specific response implementation.
   */
  override def unbox: FakeResponse = response

  /**
   * Touches a response.
   *
   * @return A touched response pipeline.
   */
  override protected[silhouette] def touch: ResponsePipeline[FakeResponse] = new FakeResponsePipeline(response) {
    override protected[silhouette] val touched = true
  }
}
