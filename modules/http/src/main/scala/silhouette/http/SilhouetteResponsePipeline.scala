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

/**
 * A Silhouette response implementation.
 *
 * @param status The HTTP status code.
 * @param headers The headers.
 * @param cookies The cookies.
 */
protected[silhouette] case class SilhouetteResponse(
  status: Status,
  headers: Seq[Header] = Seq(),
  cookies: Seq[Cookie] = Seq()
)

/**
 * The response pipeline implementation based on the `SilhouetteResponse`.
 *
 * @param response The response this pipeline handles.
 */
final protected[silhouette] case class SilhouetteResponsePipeline(response: SilhouetteResponse)
  extends ResponsePipeline[SilhouetteResponse] {

  /**
   * Gets the HTTP status code.
   *
   * @return The HTTP status code.
   */
  override def status: Status = response.status

  /**
   * Gets all headers.
   *
   * @return All headers.
   */
  override def headers: Seq[Header] = response.headers

  /**
   * Creates a new response pipeline with the given headers.
   *
   * @inheritdoc
   *
   * @param headers The headers to add.
   * @return A new response pipeline instance with the added headers.
   */
  override def withHeaders(headers: Header*): SilhouetteResponsePipeline = {
    val groupedHeaders = headers.groupByPreserveOrder(_.name).map {
      case (key, h) => Header(key, h.flatMap(_.values): _*)
    }
    val newHeaders = groupedHeaders.foldLeft(this.headers) {
      case (acc, header) =>
        acc.indexWhere(_.name == header.name) match {
          case -1 => acc :+ header
          case i  => acc.patch(i, Seq(header), 1)
        }
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
   * @param cookies The cookies to add.
   * @return A new response pipeline instance with the added cookies.
   */
  override def withCookies(cookies: Cookie*): SilhouetteResponsePipeline = {
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
