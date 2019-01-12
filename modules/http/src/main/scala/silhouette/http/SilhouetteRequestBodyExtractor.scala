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

import silhouette.http.BodyFormat._

import scala.util.{ Failure, Success }

/**
 * The request body extractor based on the [[SilhouetteRequest]].
 */
class SilhouetteRequestBodyExtractor extends RequestBodyExtractor[SilhouetteRequest] {

  /**
   * Gets the raw string representation of the body for debugging purpose.
   *
   * @param request The request from which the body should be extracted.
   * @return The raw string representation of the body for debugging purpose.
   */
  override def raw(request: SilhouetteRequest): String = request.body.map(_.raw).getOrElse("")

  /**
   * Extracts a value from JSON body.
   *
   * @param request The request from which the body should be extracted.
   * @param name    The name of the value to extract.
   * @return The extracted value on success, otherwise an error on failure.
   */
  override def fromJson(request: SilhouetteRequest, name: String): ExtractionResult = {
    request.body match {
      case Some(body @ JsonBody(_, _)) =>
        body.as[JsonBody.Type] match {
          case Success(json) =>
            json.hcursor.get[String](name).toOption match {
              case Some(value) => ExtractedValue(value)
              case None        => NotFound
            }
          case Failure(error) => ExtractionError(error)
        }
      case Some(body) => WrongContentType(body.contentType, JsonBody.allowedTypes)
      case None       => EmptyBody
    }
  }

  /**
   * Extracts a value from XML body.
   *
   * @param request The request from which the body should be extracted.
   * @param name    The name of the value to extract.
   * @return The extracted value on success, otherwise an error on failure.
   */
  override def fromXml(request: SilhouetteRequest, name: String): ExtractionResult = {
    request.body match {
      case Some(body @ XmlBody(_, _)) =>
        body.as[XmlBody.Type] match {
          case Success(node) =>
            node.\\(name).headOption.map(_.text) match {
              case Some(value) => ExtractedValue(value)
              case None        => NotFound
            }
          case Failure(error) => ExtractionError(error)
        }
      case Some(body) => WrongContentType(body.contentType, XmlBody.allowedTypes)
      case None       => EmptyBody
    }
  }

  /**
   * Extracts a value from form-url-encoded body.
   *
   * @param request The request from which the body should be extracted.
   * @param name    The name of the value to extract.
   * @return The extracted value on success, otherwise an error on failure.
   */
  override def fromFormUrlEncoded(request: SilhouetteRequest, name: String): ExtractionResult = {
    request.body match {
      case Some(body @ FormUrlEncodedBody(_, _)) =>
        body.as[FormUrlEncodedBody.Type] match {
          case Success(formUrlEncoded) =>
            formUrlEncoded.get(name).flatMap(_.headOption) match {
              case Some(value) => ExtractedValue(value)
              case None        => NotFound
            }
          case Failure(error) => ExtractionError(error)
        }
      case Some(body) => WrongContentType(body.contentType, JsonBody.allowedTypes)
      case None       => EmptyBody
    }
  }
}
