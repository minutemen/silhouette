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

import silhouette.RichSeq._
import sttp.model.{ CookieWithMeta, Header, Headers, StatusCode }

import scala.collection.immutable.Seq

/**
 * A Silhouette response implementation.
 *
 * @param status The HTTP status code.
 * @param headers The headers.
 * @param cookies The cookies.
 */
protected[silhouette] case class SilhouetteResponse(
  status: StatusCode,
  headers: Headers = Headers(Seq()),
  cookies: Seq[CookieWithMeta] = Seq()
)

/**
 * The response pipeline implementation based on the `SilhouetteResponse`.
 *
 * @param response The response this pipeline handles.
 */
final protected[silhouette] case class SilhouetteResponsePipeline(protected val response: SilhouetteResponse)
  extends ResponsePipeline[SilhouetteResponse] {

  /**
   * Gets the HTTP status code.
   *
   * @return The HTTP status code.
   */
  override def status: StatusCode = response.status

  /**
   * Gets all headers.
   *
   * @return All headers.
   */
  override def headers: Headers = response.headers

  /**
   * Creates a new response pipeline with the given headers.
   *
   * @inheritdoc
   *
   * @param headers The headers to add.
   * @return A new response pipeline instance with the added headers.
   */
  override def withHeaders(headers: Header*): SilhouetteResponsePipeline =
    copy(response.copy(headers = Headers(response.headers.headers ++ headers)))

  /**
   * Gets the list of cookies.
   *
   * @return The list of cookies.
   */
  override def cookies: Seq[CookieWithMeta] = response.cookies

  /**
   * Creates a new response pipeline with the given cookies.
   *
   * @inheritdoc
   *
   * @param cookies The cookies to add.
   * @return A new response pipeline instance with the added cookies.
   */
  override def withCookies(cookies: CookieWithMeta*): SilhouetteResponsePipeline = {
    val filteredCookies = cookies.groupByPreserveOrder(_.name).map(_._2.last)
    val newCookies = filteredCookies.foldLeft(this.cookies) {
      case (acc, cookie) =>
        acc.indexWhere(_.name == cookie.name) match {
          case -1 => acc :+ cookie
          case i  => acc.patch(i, Seq(cookie), 1)
        }
    }

    copy(response.copy(cookies = newCookies))
  }

  /**
   * Unboxes the framework specific response implementation.
   *
   * @return The framework specific response implementation.
   */
  override def unbox: SilhouetteResponse = response
}
