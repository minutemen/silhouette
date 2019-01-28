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

import java.net.{ URLDecoder, URLEncoder }

import io.circe.parser.parse
import io.circe.{ Json, ParsingFailure }
import silhouette.http.DefaultBodyReads._
import silhouette.http.MimeType._
import silhouette.{ Reads, TransformException, Writes }

import scala.annotation.implicitNotFound
import scala.collection.mutable
import scala.io.Codec
import scala.util.{ Failure, Success, Try }
import scala.xml._

/**
 * Represents an HTTP request/response body.
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
   * Tries to transforms the body into a T, throwing an exception if it can't. An implicit BodyReads[T] must be defined.
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
   * Creates a [[Body]] from T. An implicit BodyWrites[T] must be defined.
   *
   * @param value  The value to create the body from.
   * @param codec  The codec of the resulting body.
   * @param writes The writes transformer.
   * @tparam T The type of the value.
   * @return The body representation of the given value.
   */
  def from[T](value: T, codec: Codec = Body.DefaultCodec)(implicit writes: Codec => BodyWrites[T]): Body =
    writes(codec).write(value)
}

/**
 * An extractor for a text body.
 */
private[silhouette] object TextBody {
  type Type = String
  final val contentType = `text/plain`
  def unapply(body: Body): Option[(Codec, mutable.WrappedArray[Byte])] = body.contentType match {
    case this.contentType => Some((body.codec, body.data))
    case _                => None
  }
}

/**
 * An extractor for a form-url-encoded body.
 */
private[silhouette] object FormUrlEncodedBody {
  type Type = Map[String, Seq[String]]
  final val contentType = `application/x-www-form-urlencoded`
  def unapply(body: Body): Option[(Codec, mutable.WrappedArray[Byte])] = body.contentType match {
    case this.contentType => Some((body.codec, body.data))
    case _                => None
  }
}

/**
 * An extractor for a JSON body.
 */
private[silhouette] object JsonBody {
  type Type = Json
  final val contentType = `application/json`
  final val allowedTypes = Seq(contentType, `text/json`)
  def unapply(body: Body): Option[(Codec, mutable.WrappedArray[Byte])] = body.contentType match {
    case ct if this.allowedTypes contains ct => Some((body.codec, body.data))
    case _                                   => None
  }
}

/**
 * An extractor for an XML body.
 */
private[silhouette] object XmlBody {
  type Type = Node
  final val contentType = `application/xml`
  final val allowedTypes = Seq(contentType, `text/xml`)
  def unapply(body: Body): Option[(Codec, mutable.WrappedArray[Byte])] = body.contentType match {
    case ct if this.allowedTypes contains ct => Some((body.codec, body.data))
    case _                                   => None
  }
}

/**
 * Transforms a [[Body]] into an instance of `T`.
 *
 * @tparam T The target type on the read operation.
 */
@implicitNotFound("No Body transformer found for type ${T}. Try to implement an implicit BodyReads for this type.")
protected[silhouette] trait BodyReads[T] extends Reads[Body, Try[T]]

/**
 * Transforms an instance of `T` into a [[Body]].
 *
 * @tparam T The source type on the write operation
 */
@implicitNotFound("No Body transformer found for type ${T}. Try to implement an implicit BodyWrites for this type.")
protected[silhouette] trait BodyWrites[T] extends Writes[T, Body]

/**
 * The only aim of this object is to provide a default implicit [[BodyReads]], that uses
 * the [[DefaultBodyReads]] trait to provide the lowest implicit conversion chain.
 */
private[silhouette] object BodyReads extends DefaultBodyReads

/**
 * The only aim of this object is to provide a default implicit [[BodyWrites]], that uses
 * the [[DefaultBodyWrites]] trait to provide the lowest implicit conversion chain.
 */
private[silhouette] object BodyWrites extends DefaultBodyWrites

/**
 * Provides implicit [[BodyReads]] for the body types supported by Silhouette.
 */
private[silhouette] trait DefaultBodyReads {

  /**
   * Transforms a [[Body]] into string.
   *
   * @return A [[BodyReads]] instance that transforms a [[Body]] into a string.
   */
  implicit val stringReads: BodyReads[TextBody.Type] = {
    case TextBody(codec, bytes) =>
      Try(new String(bytes.array, codec.charSet))
    case Body(ct, _, _) =>
      Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(TextBody.contentType, ct)))
  }

  /**
   * Transforms a [[Body]] into form URL encoded string.
   *
   * @return A [[BodyReads]] instance that transforms a [[Body]] into a form URL encoded string.
   */
  implicit val formUrlEncodedReads: BodyReads[FormUrlEncodedBody.Type] = {
    case FormUrlEncodedBody(codec, bytes) =>
      Try {
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

        pairs
          .groupBy(_._1)
          .map(param => param._1 -> param._2.map(_._2))(scala.collection.breakOut)
      }
    case Body(ct, _, _) =>
      Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(FormUrlEncodedBody.contentType, ct)))
  }

  /**
   * Transforms a [[Body]] into a Circe JSON object.
   *
   * @return A [[BodyReads]] instance that transforms a [[Body]] into a Circe JSON object.
   */
  implicit val circeJsonReads: BodyReads[JsonBody.Type] = {
    case JsonBody(codec, bytes) =>
      parse(new String(bytes.array, codec.charSet)) match {
        case Left(ParsingFailure(msg, e)) => Failure(new TransformException(msg, Option(e)))
        case Right(json)                  => Success(json)
      }
    case Body(ct, _, _) =>
      Failure(new UnsupportedContentTypeException(
        UnsupportedContentType.format(JsonBody.allowedTypes.mkString(", "), ct)
      ))
  }

  /**
   * Transforms a [[Body]] into a Scala XML object.
   *
   * @return A [[BodyReads]] instance that transforms a [[Body]] into a Scala XML object.
   */
  implicit val scalaXmlReads: BodyReads[XmlBody.Type] = {
    case XmlBody(codec, bytes) =>
      Try(XML.loadString(new String(bytes.array, codec.charSet))).recover {
        case e: SAXParseException => throw new TransformException(e.getMessage, Option(e))
      }
    case Body(ct, _, _) =>
      Failure(new UnsupportedContentTypeException(
        UnsupportedContentType.format(XmlBody.allowedTypes.mkString(", "), ct)
      ))
  }
}

/**
 * Provides implicit [[BodyWrites]] for the body types supported by Silhouette.
 */
private[silhouette] trait DefaultBodyWrites {

  /**
   * Transforms a string into a [[Body]].
   *
   * @return A [[BodyWrites]] instance that transforms a string into a [[Body]].
   */
  implicit val stringWrites: Codec => BodyWrites[TextBody.Type] = (codec: Codec) => (str: TextBody.Type) => {
    Body(`text/plain`, codec, str.getBytes(codec.charSet))
  }

  /**
   * Transforms a form URL encoded string into [[Body]].
   *
   * @return A [[BodyWrites]] instance that transforms a form URL encoded string into [[Body]].
   */
  implicit val formUrlEncodedWrites: Codec => BodyWrites[FormUrlEncodedBody.Type] = {
    codec: Codec =>
      {
        formData: FormUrlEncodedBody.Type =>
          {
            val charset = codec.charSet.name()
            val urlEncodedString = formData.flatMap { item =>
              item._2.map(c => s"${item._1}=${URLEncoder.encode(c, charset)}")
            }.mkString("&")
            Body(FormUrlEncodedBody.contentType, codec, urlEncodedString.getBytes(codec.charSet))
          }
      }
  }

  /**
   * Transforms a Circe JSON object into a [[Body]].
   *
   * @return A [[BodyWrites]] instance that transforms a Circe JSON object into a [[Body]].
   */
  implicit val circeJsonWrites: Codec => BodyWrites[JsonBody.Type] = (codec: Codec) => (json: JsonBody.Type) => {
    Body(JsonBody.contentType, codec, json.noSpaces.getBytes(codec.charSet))
  }

  /**
   * Transforms a Scala XML object into a [[Body]] and vice versa.
   *
   * @return A [[BodyWrites]] instance that transforms a Scala XML object into a [[Body]] and vice versa.
   */
  implicit val scalaXmlWrites: Codec => BodyWrites[XmlBody.Type] = (codec: Codec) => (xml: XmlBody.Type) => {
    Body(XmlBody.contentType, codec, xml.mkString.getBytes(codec.charSet))
  }
}

/**
 * The companion object.
 */
private[silhouette] object DefaultBodyReads {

  /**
   * The error messages.
   */
  val UnsupportedContentType: String = "Expected `%s` but found `%s`"
}
