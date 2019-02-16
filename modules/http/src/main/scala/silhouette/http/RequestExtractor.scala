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
 * Represents the result of an extraction try.
 */
protected[silhouette] sealed trait ExtractionResult

/**
 * Indicates that no body was sent in the request.
 */
protected[silhouette] case object EmptyBody extends ExtractionResult

/**
 * Indicates that the body isn't of the specific content type.
 *
 * @param found    The found content type.
 * @param expected The list of expected content types.
 */
protected[silhouette] final case class WrongContentType(found: MimeType, expected: Seq[MimeType])
  extends ExtractionResult

/**
 * Indicates that the value couldn't be found in the body.
 */
protected[silhouette] case object NotFound extends ExtractionResult

/**
 * Indicates that an error occurred during the extraction try.
 *
 * @param error The extraction error.
 */
protected[silhouette] final case class ExtractionError(error: Throwable) extends ExtractionResult

/**
 * Represents the extracted value.
 *
 * @param value The extracted value.
 */
protected[silhouette] final case class ExtractedValue(value: String) extends ExtractionResult

/**
 * Adds the ability to extract values from a request.
 *
 * @tparam R The type of the request.
 */
protected[silhouette] trait RequestExtractor[R] extends LazyLogging {
  self: RequestPipeline[R] =>

  /**
   * The request parts from which a value can be extracted.
   */
  type Parts = Seq[RequestPart.Value]

  /**
   * The framework specific request implementation.
   */
  protected val request: R

  /**
   * The body extractor implementation.
   */
  protected val bodyExtractor: RequestBodyExtractor[R]

  /**
   * Extracts a string from a request.
   *
   * @param name  The name of the value to extract.
   * @param parts Some request parts from which a value can be extracted or None to extract values from any part
   *              of the request.
   * @return Maybe the extracted value as string.
   */
  def extractString(name: String, parts: Option[Parts] = None): Option[String] = {
    fromQueryString(name, parts)
      .orElse(fromHeaders(name, parts))
      .orElse(fromFormUrlEncodedBody(name, parts))
      .orElse(fromJsonBody(name, parts))
      .orElse(fromXmlBody(name, parts))
  }

  /**
   * Extracts a value from query string.
   *
   * @param name  The name of the value to extract.
   * @param parts The request parts from which a value can be extracted.
   * @return Maybe the extracted value as string.
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
   * @return Maybe the extracted value as string.
   */
  protected def fromHeaders(name: String, parts: Option[Parts]): Option[String] = {
    isAllowed(RequestPart.Headers, parts) {
      logger.debug(s"Try to extract value with name `$name` from headers: $headers")
      header(name).map(_.value)
    }
  }

  /**
   * Extracts a value from form url encoded body.
   *
   * @param name  The name of the value to extract.
   * @param parts The request parts from which a value can be extracted.
   * @return Maybe the extracted value as string.
   */
  protected def fromFormUrlEncodedBody(name: String, parts: Option[Parts]): Option[String] = {
    isAllowed(RequestPart.FormUrlEncodedBody, parts) {
      fromBody("form-url-encoded", name, (bodyExtractor.fromFormUrlEncoded _).curried)
    }
  }

  /**
   * Extracts a value from Json body.
   *
   * @param name  The name of the value to extract.
   * @param parts The request parts from which a value can be extracted.
   * @return Maybe the extracted value as string.
   */
  protected def fromJsonBody(name: String, parts: Option[Parts]): Option[String] = {
    isAllowed(RequestPart.JsonBody, parts) {
      fromBody("JSON", name, (bodyExtractor.fromJson _).curried)
    }
  }

  /**
   * Extracts a value from Xml body.
   *
   * @param name  The name of the value to extract.
   * @param parts The request parts from which a value can be extracted.
   * @return Maybe the extracted value as string.
   */
  protected def fromXmlBody(name: String, parts: Option[Parts]): Option[String] = {
    isAllowed(RequestPart.XMLBody, parts) {
      fromBody("XML", name, (bodyExtractor.fromXml _).curried)
    }
  }

  /**
   * Extracts a value from a body with the help of the concrete extractor.
   *
   * @param `type`    The type of the body as string.
   * @param name      The name of the value to extract.
   * @param extractor The extractor function.
   * @return Maybe the extracted value as string.
   */
  private def fromBody(`type`: String, name: String, extractor: R => String => ExtractionResult): Option[String] = {
    logger.debug(s"Try to extract value with name `$name` from ${`type`} body")
    extractor(request)(name) match {
      case EmptyBody =>
        logger.debug("Body is empty")
        None
      case WrongContentType(found, expected) =>
        logger.debug(s"Body isn't of content type ${expected.mkString(", ")}; instead found $found")
        None
      case NotFound =>
        logger.debug(s"Couldn't found value with name `$name`in body: ${bodyExtractor.raw(request)}")
        None
      case ExtractedValue(value) =>
        logger.debug(
          s"Successfully extracted value with name `$name` from ${`type`} body: ${bodyExtractor.raw(request)}"
        )
        Some(value)
      case ExtractionError(error) =>
        logger.error(
          s"Failed to extract value with name `$name` from ${`type`} body: ${bodyExtractor.raw(request)}",
          error
        )
        None
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
   * Gets the raw string representation of the body for debugging purpose.
   *
   * @param request The request from which the body should be extracted.
   * @return The raw string representation of the body for debugging purpose.
   */
  def raw(request: R): String

  /**
   * Extracts a value from JSON body.
   *
   * @param request The request from which the body should be extracted.
   * @param name    The name of the value to extract.
   * @return The extracted value on success, otherwise an error on failure.
   */
  def fromJson(request: R, name: String): ExtractionResult

  /**
   * Extracts a value from XML body.
   *
   * @param request The request from which the body should be extracted.
   * @param name    The name of the value to extract.
   * @return The extracted value on success, otherwise an error on failure.
   */
  def fromXml(request: R, name: String): ExtractionResult

  /**
   * Extracts a value from form-url-encoded body.
   *
   * @param request The request from which the body should be extracted.
   * @param name    The name of the value to extract.
   * @return The extracted value on success, otherwise an error on failure.
   */
  def fromFormUrlEncoded(request: R, name: String): ExtractionResult
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
   * Allows to extract a request value from a form-url-encoded body.
   */
  val FormUrlEncodedBody = Value("form-url-encoded-body")
}
