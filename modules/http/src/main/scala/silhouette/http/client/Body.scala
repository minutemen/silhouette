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

import java.net.{ URLDecoder, URLEncoder }

import io.circe.parser.parse
import io.circe.{ Json, ParsingFailure }
import silhouette.http.MimeType
import silhouette.http.MimeType._
import silhouette.http.client.DefaultBodyFormat._
import silhouette.{ Reads, TransformException, Writes }

import scala.collection.mutable
import scala.io.Codec
import scala.util.{ Failure, Success, Try }
import scala.xml._

/**
 * Represents the body of a request.
 *
 * We use a [[scala.collection.mutable.WrappedArray]] instance to get a proper `equals` and `hashCode` method for
 * the body. With this we can write better tests.
 *
 * Since Scala 2.8, Scala arrays are just Java arrays, so we aren't free to make the hashCode method return
 * a different answer than Java does. (https://issues.scala-lang.org/browse/SI-1607)
 *
 * @param contentType The content type of the body.
 * @param codec       The codec of the body.
 * @param data        The body data.
 */
protected[silhouette] final case class Body(
  contentType: MimeType,
  codec: Codec = Body.DefaultCodec,
  data: mutable.WrappedArray[Byte]
) {

  /**
   * Gets the content as raw string.
   *
   * @return The content as raw string.
   */
  def raw: String = new String(data.array, codec.charSet)

  /**
   * Transforms the body with the help of a reads into the given format.
   *
   * @param reads The reads transformer.
   * @tparam T The type of the body to transform to.
   * @return The body transformed to the given type on success, a failure otherwise.
   */
  def as[T](implicit reads: BodyReads[T]): Try[T] = reads.read(this)
}

/**
 * The companion object of the [[Body]].
 */
private[silhouette] object Body {

  /**
   * The default codec.
   */
  val DefaultCodec: Codec = Codec.UTF8

  /**
   * Creates a [Body] from a value with the help of a `BodyWrites`.
   *
   * @param value  The value to create the body from.
   * @param writes The writes transformer.
   * @tparam T The type of the value.
   * @return The body representation of the given value.
   */
  def from[T](value: T)(implicit writes: BodyWrites[T]): Body = writes.write(value)
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
protected[silhouette] trait BodyFormat[T] extends BodyReads[T] with BodyWrites[T]

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
  implicit val stringFormat: BodyFormat[String] = new BodyFormat[String] {
    override def read(body: Body): Try[String] = body match {
      case Body(`text/plain`, codec, bytes) =>
        Try(new String(bytes.array, codec.charSet))
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(`text/plain`, ct)))
    }
    override def write(str: String): Body = {
      val bytes = str.getBytes(Body.DefaultCodec.charSet)
      Body(`text/plain`, Body.DefaultCodec, bytes)
    }
  }

  /**
   * Transforms a [[Body]] into form URL encoded string and vice versa.
   *
   * @return A [[BodyFormat]] instance that transforms a [[Body]] into a form URL encoded string and vice versa.
   */
  implicit val formUrlEncodedFormat: BodyFormat[Map[String, Seq[String]]] = new BodyFormat[Map[String, Seq[String]]] {
    override def read(body: Body): Try[Map[String, Seq[String]]] = body match {
      case Body(`application/x-www-form-urlencoded`, codec, bytes) =>
        val data = new String(bytes.array, codec.charSet)
        val split = "[&;]".r.split(data)
        val pairs: Seq[(String, String)] = if (split.length == 1 && split(0).isEmpty) {
          Seq.empty
        } else {
          split.map { param =>
            val parts = param.split("=", -1)
            val key = URLDecoder.decode(parts(0), codec.charSet.name())
            val value = URLDecoder.decode(parts.lift(1).getOrElse(""), codec.charSet.name())
            key -> value
          }
        }
        Try(
          pairs
            .groupBy(_._1)
            .map(param => param._1 -> param._2.map(_._2))(scala.collection.breakOut)
        )
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(
          UnsupportedContentType.format(`application/x-www-form-urlencoded`, ct)
        ))
    }
    override def write(formData: Map[String, Seq[String]]): Body = {
      val charset = Body.DefaultCodec.charSet.name()
      val bytes = formData.flatMap(item => item._2.map(c => s"${item._1}=${URLEncoder.encode(c, charset)}"))
        .mkString("&")
        .getBytes(Body.DefaultCodec.charSet)
      Body(`application/x-www-form-urlencoded`, Body.DefaultCodec, bytes)
    }
  }

  /**
   * Transforms a [[Body]] into a Circe JSON object and vice versa.
   *
   * @return A [[BodyFormat]] instance that transforms a [[Body]] into a Circe JSON object and vice versa.
   */
  implicit val circeJsonFormat: BodyFormat[Json] = new BodyFormat[Json] {
    override def read(body: Body): Try[Json] = body match {
      case Body(`application/json`, codec, bytes) =>
        parse(new String(bytes.array, codec.charSet)) match {
          case Left(ParsingFailure(msg, e)) => Failure(new TransformException(msg, Option(e)))
          case Right(json)                  => Success(json)
        }
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(`application/json`, ct)))
    }
    override def write(json: Json): Body = {
      val bytes = json.noSpaces.getBytes(Body.DefaultCodec.charSet)
      Body(`application/json`, Body.DefaultCodec, bytes)
    }
  }

  /**
   * Transforms a [[Body]] into a Scala XML object and vice versa.
   *
   * @return A [[BodyFormat]] instance that transforms a [[Body]] into a Scala XML object and vice versa.
   */
  implicit val scalaXmlFormat: BodyFormat[Node] = new BodyFormat[Node] {
    override def read(body: Body): Try[Node] = body match {
      case Body(`application/xml`, codec, bytes) =>
        Try(XML.loadString(new String(bytes.array, codec.charSet))).recover {
          case e: SAXParseException => throw new TransformException(e.getMessage, Option(e))
        }
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(`application/xml`, ct)))
    }
    override def write(xml: Node): Body = {
      val bytes = xml.mkString.getBytes(Body.DefaultCodec.charSet)
      Body(`application/xml`, Body.DefaultCodec, bytes)
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
  val UnsupportedContentType: String = "Expected %s but found %s"
}
