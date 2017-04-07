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
package silhouette.http.client

import io.circe.parser.parse
import io.circe.{ Json, ParsingFailure }
import silhouette.exceptions.{ TransformException, UnsupportedContentTypeException }
import silhouette.http.client.DefaultBodyFormat._
import silhouette.util.{ Reads, Writes }

import scala.io.Codec
import scala.util.{ Failure, Success, Try }
import scala.xml._

/**
 * Represents the body of a request.
 *
 * @param contentType The content type of the body.
 * @param codec       The codec of the body.
 * @param data        The body data.
 */
private[silhouette] final case class Body(
  contentType: ContentType,
  codec: Codec = Body.DefaultCodec,
  data: Array[Byte])

/**
 * The companion object of the [[Body]].
 */
private[silhouette] object Body {

  /**
   * The default codec.
   */
  val DefaultCodec: Codec = Codec.UTF8
}

/**
 * Transforms a [[Body]] into an instance of `T`.
 *
 * @tparam T The target type on the read operation.
 */
private[silhouette] trait BodyReads[T] extends Reads[Body, Try[T]]

/**
 * Transforms an instance of `T` into a [[Body]].
 *
 * @tparam T The source type on the write operation
 */
private[silhouette] trait BodyWrites[T] extends Writes[T, Body]

/**
 * Body transformer combinator.
 *
 * @tparam T The target type on the read operation and the source type on the write operation.
 */
private[silhouette] trait BodyFormat[T] extends BodyReads[T] with BodyWrites[T]

/**
 * The only aim of this object is to provide a default implicit [[BodyFormat]], that uses
 * the [[DefaultBodyFormat]] trait to provide the lowest implicit conversion chain.
 */
private[silhouette] object BodyFormat extends DefaultBodyFormat

/**
 * Provides implicit transformers for the body types supported by Silhouette.
 */
private[silhouette] trait DefaultBodyFormat {

  /**
   * Transforms a [[Body]] into string and vice versa.
   *
   * @return A [[BodyFormat]] instance that transforms a [[Body]] into a string and vice versa.
   */
  implicit def stringFormat: BodyFormat[String] = new BodyFormat[String] {
    override def read(body: Body): Try[String] = body match {
      case Body(ContentTypes.`text/plain`, codec, bytes) =>
        Try(new String(bytes, codec.charSet))
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(ContentTypes.`text/plain`, ct)))
    }
    override def write(str: String): Body = {
      val bytes = str.getBytes(Body.DefaultCodec.charSet)
      Body(ContentTypes.`text/plain`, Body.DefaultCodec, bytes)
    }
  }

  /**
   * Transforms a [[Body]] into a Circe JSON object and vice versa.
   *
   * @return A [[BodyFormat]] instance that transforms a [[Body]] into a Circe JSON object and vice versa.
   */
  implicit def circeJsonFormat: BodyFormat[Json] = new BodyFormat[Json] {
    override def read(body: Body): Try[Json] = body match {
      case Body(ContentTypes.`application/json`, codec, bytes) =>
        parse(new String(bytes, codec.charSet)) match {
          case Left(ParsingFailure(msg, e)) => Failure(new TransformException(msg, Option(e)))
          case Right(json)                  => Success(json)
        }
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(ContentTypes.`application/json`, ct)))
    }
    override def write(json: Json): Body = {
      val bytes = json.noSpaces.getBytes(Body.DefaultCodec.charSet)
      Body(ContentTypes.`application/json`, Body.DefaultCodec, bytes)
    }
  }

  /**
   * Transforms a [[Body]] into a Scala XML object and vice versa.
   *
   * @return A [[BodyFormat]] instance that transforms a [[Body]] into a Scala XML object and vice versa.
   */
  implicit def scalaXmlFormat: BodyFormat[Node] = new BodyFormat[Node] {
    override def read(body: Body): Try[Node] = body match {
      case Body(ContentTypes.`application/xml`, codec, bytes) =>
        Try(XML.loadString(new String(bytes, codec.charSet))).recover {
          case e: SAXParseException => throw new TransformException(e.getMessage, Option(e))
        }
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(ContentTypes.`application/xml`, ct)))
    }
    override def write(xml: Node): Body = {
      val bytes = xml.mkString.getBytes(Body.DefaultCodec.charSet)
      Body(ContentTypes.`application/xml`, Body.DefaultCodec, bytes)
    }
  }
}

/**
 * The companion object.
 */
private[silhouette] object DefaultBodyFormat {

  /**
   * The error messages.
   */
  val UnsupportedContentType: String = "[Silhouette][DefaultBodyFormat] Expected %s but found %s"
}
