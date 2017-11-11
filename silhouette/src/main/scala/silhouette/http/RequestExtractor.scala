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

import com.typesafe.scalalogging.LazyLogging

/**
 * Adds the ability to extract values from a request.
 *
 * @tparam R The type of the request.
 */
trait RequestExtractor[R] extends LazyLogging {
  self: RequestPipeline[R] =>

  /**
   * The request parts from which a value can be extracted.
   */
  type Parts = Seq[RequestPart.Value]

  /**
   * The request body extractor used to extract values from request body.
   */
  val bodyExtractor: RequestBodyExtractor[R]

  /**
   * Extracts a string from a request.
   *
   * @param name  The name of the value to extract.
   * @param parts Some request parts from which a value can be extracted or None to extract values from any part
   *              of the request.
   * @return The extracted value as string.
   */
  def extractString(name: String, parts: Option[Parts] = None): Option[String] = {
    fromQueryString(name, parts)
      .orElse(fromHeaders(name, parts))
      .orElse(fromFormUrlEncoded(name, parts))
      .orElse(fromJson(name, parts))
      .orElse(fromXml(name, parts))
  }

  /**
   * Extracts a value from query string.
   *
   * @param name  The name of the value to extract.
   * @param parts The request parts from which a value can be extracted.
   * @return The extracted value as string.
   */
  protected def fromQueryString(name: String, parts: Option[Parts]): Option[String] = {
    isAllowed(RequestPart.QueryString, parts) {
      logger.debug(s"Try to extract value with name `$name` from query string: $rawQueryString")
      queryParam(name).headOption
    }
  }

  /**
   * Extracts a value from headers.
   *
   * @param name  The name of the value to extract.
   * @param parts The request parts from which a value can be extracted.
   * @return The extracted value as string.
   */
  protected def fromHeaders(name: String, parts: Option[Parts]): Option[String] = {
    isAllowed(RequestPart.Headers, parts) {
      logger.debug(s"Try to extract value with name `$name` from headers: $headers")
      header(name).headOption
    }
  }

  /**
   * Extracts a value from form url encoded body.
   *
   * @param name  The name of the value to extract.
   * @param parts The request parts from which a value can be extracted.
   * @return The extracted value as string.
   */
  protected def fromFormUrlEncoded(name: String, parts: Option[Parts]): Option[String] = {
    isAllowed(RequestPart.FormUrlEncodedBody, parts) {
      bodyExtractor.fromFormUrlEncoded(name).flatMap {
        case (body, maybeValue) =>
          logger.debug(s"Try to extract value with name `$name` from form url encoded body: $body")
          maybeValue
      }
    }
  }

  /**
   * Extracts a value from Json body.
   *
   * @param name  The name of the value to extract.
   * @param parts The request parts from which a value can be extracted.
   * @return The extracted value.
   */
  protected def fromJson(name: String, parts: Option[Parts]): Option[String] = {
    isAllowed(RequestPart.JsonBody, parts) {
      bodyExtractor.fromJson(name).flatMap {
        case (body, maybeValue) =>
          logger.debug(s"Try to extract value with name `$name` from Json body: $body")
          maybeValue
      }
    }
  }

  /**
   * Extracts a value from Xml body.
   *
   * @param name  The name of the value to extract.
   * @param parts The request parts from which a value can be extracted.
   * @return The extracted value.
   */
  protected def fromXml(name: String, parts: Option[Parts]): Option[String] = {
    isAllowed(RequestPart.XMLBody, parts) {
      bodyExtractor.fromJson(name).flatMap {
        case (body, maybeValue) =>
          logger.debug(s"Try to extract value with name `$name` from Xml body: $body")
          maybeValue
      }
    }
  }

  /**
   * Executes the given block if the given part is contained in the list of parts or if part validation is disabled.
   *
   * @param part  The part to check for.
   * @param parts The request parts from which a value can be extracted.
   * @param b The block to execute.
   * @return The found value if any.
   */
  private def isAllowed(part: RequestPart.Value, parts: Option[Parts])(b: => Option[String]): Option[String] = {
    parts match {
      case Some(p) if !p.contains(part) => None
      case _                            => b
    }
  }
}

/**
 * The body extractor helps to extract values from the body of a request.
 */
trait RequestBodyExtractor[R] {

  /**
   * The value of the body.
   *
   * If the body isn't from a specific type, then the value should be None.
   * If the body is from the specific type then the value should be Some.
   *   - The first tuple value should be the string representation of the body.
   *   - The second tuple value should be either the extracted value or None if the value couldn't
   *     be found for the given name.
   */
  type BodyValue = Option[(String, Option[String])]

  /**
   * Extracts a value from Json body.
   *
   * @param name The name of the value to extract.
   * @return [[BodyValue]]
   */
  def fromJson(name: String): BodyValue

  /**
   * Extracts a value from Xml body.
   *
   * @param name The name of the value to extract.
   * @return [[BodyValue]]
   */
  def fromXml(name: String): BodyValue

  /**
   * Extracts a value from form url encoded body.
   *
   * @param name The name of the value to extract.
   * @return [[BodyValue]]
   */
  def fromFormUrlEncoded(name: String): BodyValue
}

/**
 * The request parts from which a value can be extracted.
 */
object RequestPart extends Enumeration {

  /**
   * Allows to extract a request value from query string.
   */
  val QueryString = Value("query-string")

  /**
   * Allows to extract a request value from the headers.
   */
  val Headers = Value("headers")

  /**
   * Allows to extract a request value from a Json body.
   */
  val JsonBody = Value("json-body")

  /**
   * Allows to extract a request value from a XML body.
   */
  val XMLBody = Value("xml-body")

  /**
   * Allows to extract a request value from a form-urlencoded body.
   */
  val FormUrlEncodedBody = Value("form-urlencoded-body")
}
