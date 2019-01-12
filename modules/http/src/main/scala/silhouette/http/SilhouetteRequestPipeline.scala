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

import java.net.URI

import silhouette.RichSeq._

/**
 * The Silhouette request implementation.
 *
 * @param uri         The absolute URI of the request target.
 * @param method      The HTTP request method.
 * @param headers     The headers.
 * @param cookies     The cookies.
 * @param queryParams The query params.
 * @param body        An optional request body.
 */
final protected[silhouette] case class SilhouetteRequest(
  uri: URI,
  method: Method,
  headers: Seq[Header] = Seq(),
  cookies: Seq[Cookie] = Seq(),
  queryParams: Map[String, Seq[String]] = Map(),
  body: Option[Body] = None
)

/**
 * The request pipeline implementation based on the [[SilhouetteRequest]].
 *
 * @param request       The request this pipeline handles.
 * @param bodyExtractor The request body extractor used to extract values from request body.
 */
final protected[silhouette] case class SilhouetteRequestPipeline(
  request: SilhouetteRequest,
  bodyExtractor: RequestBodyExtractor[SilhouetteRequest] = new SilhouetteRequestBodyExtractor
) extends RequestPipeline[SilhouetteRequest] {

  /**
   * Gets the absolute URI of the request target.
   *
   * This must contain the absolute URI of the request target, because we need this to resolve relative URIs
   * against this.
   *
   * @return The absolute URI of the request target.
   */
  override def uri: URI = request.uri

  /**
   * Creates a new request pipeline with the given URI.
   *
   * This must contain the absolute URI of the request target, because we need this to resolve relative URIs
   * against this URI.
   *
   * @param uri The absolute URI of the request target.
   * @return A new request pipeline instance with the set URI.
   */
  override def withUri(uri: URI): SilhouetteRequestPipeline = copy(request.copy(uri = uri))

  /**
   * Gets the HTTP request method.
   *
   * @return The HTTP request method.
   */
  override def method: Method = request.method

  /**
   * Creates a new request pipeline with the given HTTP request method.
   *
   * @param method The HTTP request method to set.
   * @return A new request pipeline instance with the set HTTP request method.
   */
  override def withMethod(method: Method): SilhouetteRequestPipeline = copy(request.copy(method = method))

  /**
   * Gets all headers.
   *
   * @return All headers.
   */
  override def headers: Seq[Header] = request.headers

  /**
   * Creates a new request pipeline with the given headers.
   *
   * @inheritdoc
   *
   * @param headers The headers to set.
   * @return A new request pipeline instance with the set headers.
   */
  override def withHeaders(headers: Header*): SilhouetteRequestPipeline = {
    val groupedHeaders = headers.groupByPreserveOrder(_.name).map {
      case (key, h) => Header(key, h.flatMap(_.values): _*)
    }
    val newHeaders = groupedHeaders.foldLeft(request.headers) {
      case (acc, header) =>
        acc.indexWhere(_.name == header.name) match {
          case -1 => acc :+ header
          case i  => acc.patch(i, Seq(header), 1)
        }
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
  override def withCookies(cookies: Cookie*): SilhouetteRequestPipeline = {
    val filteredCookies = cookies.groupByPreserveOrder(_.name).map(_._2.last)
    val newCookies = filteredCookies.foldLeft(request.cookies) {
      case (acc, cookie) =>
        acc.indexWhere(_.name == cookie.name) match {
          case -1 => acc :+ cookie
          case i  => acc.patch(i, Seq(cookie), 1)
        }
    }

    copy(request.copy(cookies = newCookies))
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
  override def withQueryParams(params: (String, String)*): SilhouetteRequestPipeline = {
    val newParams = params.groupByPreserveOrder(_._1).map {
      case (key, value) => key -> value.map(_._2)
    }.foldLeft(request.queryParams) {
      case (acc, (key, value)) => acc + (key -> value)
    }

    copy(request.copy(queryParams = newParams))
  }

  /**
   * Creates a new request pipeline with the given body extractor.
   *
   * @param bodyExtractor The body extractor to set.
   * @return A new request pipeline instance with the set body extractor.
   */
  override def withBodyExtractor(bodyExtractor: RequestBodyExtractor[SilhouetteRequest]): SilhouetteRequestPipeline = {
    copy(bodyExtractor = bodyExtractor)
  }

  /**
   * Unboxes the request this pipeline handles.
   *
   * @return The request this pipeline handles.
   */
  override def unbox: SilhouetteRequest = request
}
