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
import silhouette.TransformException
import silhouette.http.DefaultBodyReader._
import sttp.model.MediaType

import scala.annotation.implicitNotFound
import scala.collection.compat._
import scala.collection.compat.immutable.ArraySeq
import scala.collection.immutable.Seq
import scala.io.Codec
import scala.language.implicitConversions
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
 * Since Scala 2.13, [[scala.collection.mutable.WrappedArray]] is deprecated. `ArraySeq` should be used instead.
 * We use @see https://github.com/scala/scala-collection-compat to workaround this issue.
 *
 * @param contentType The content type of the body.
 * @param codec       The codec of the body.
 * @param data        The body data.
 */
final protected[silhouette] case class Body(
  contentType: MediaType,
  codec: Codec = Body.DefaultCodec,
  data: immutable.ArraySeq[Byte]
) {

  /**
   * Gets the content as raw string.
   *
   * @return The content as raw string.
   */
  def raw: String = new String(data.toArray, codec.charSet)

  /**
   * Tries to transforms the body into a T, throwing an exception if it can't. An implicit [[BodyReader]] must be
   * defined.
   *
   * @param reader The body reader.
   * @tparam T The type of the body to transform to.
   * @return The body transformed to the given type on success, a failure otherwise.
   */
  def as[T](
    implicit
    reader: BodyReader[T]
  ): Try[T] = reader(this)
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
   * Creates a [[Body]] from T. An implicit [[BodyWriter]] must be defined.
   *
   * @param value  The value to create the body from.
   * @param codec  The codec of the resulting body.
   * @param writer The body writer.
   * @tparam T The type of the value.
   * @return The body representation of the given value.
   */
  def from[T](value: T, codec: Codec = Body.DefaultCodec)(
    implicit
    writer: Codec => BodyWriter[T]
  ): Body =
    writer(codec)(value)

  /**
   * Converts an `Array[Byte]` to an `ArraySeq[Byte]` instance.
   *
   * @param data The data to convert.
   * @return The converted data.
   */
  implicit def toArraySeq(data: Array[Byte]): ArraySeq[Byte] = ArraySeq.unsafeWrapArray(data)
}

/**
 * An extractor for a text body.
 */
private[silhouette] object TextBody {
  type Type = String
  final val contentType = MediaType.TextPlain
  def unapply(body: Body): Option[(Codec, immutable.ArraySeq[Byte])] =
    body.contentType match {
      case this.contentType => Some((body.codec, body.data))
      case _                => None
    }
}

/**
 * An extractor for a form-url-encoded body.
 */
private[silhouette] object FormUrlEncodedBody {
  type Type = Map[String, Seq[String]]
  final val contentType = MediaType.ApplicationXWwwFormUrlencoded
  def unapply(body: Body): Option[(Codec, immutable.ArraySeq[Byte])] =
    body.contentType match {
      case this.contentType => Some((body.codec, body.data))
      case _                => None
    }
}

/**
 * An extractor for a JSON body.
 */
private[silhouette] object JsonBody {
  type Type = Json
  final val contentType = MediaType.ApplicationJson
  final val allowedTypes = Seq(contentType, MediaType("text", "json"))
  def unapply(body: Body): Option[(Codec, immutable.ArraySeq[Byte])] =
    body.contentType match {
      case ct if this.allowedTypes contains ct => Some((body.codec, body.data))
      case _                                   => None
    }
}

/**
 * An extractor for an XML body.
 */
private[silhouette] object XmlBody {
  type Type = Node
  final val contentType = MediaType.ApplicationXml
  final val allowedTypes = Seq(contentType, MediaType("text", "xml"))
  def unapply(body: Body): Option[(Codec, immutable.ArraySeq[Byte])] =
    body.contentType match {
      case ct if this.allowedTypes contains ct => Some((body.codec, body.data))
      case _                                   => None
    }
}

/**
 * Transforms a [[Body]] into an instance of `T`.
 *
 * @tparam T The target type on the read operation.
 */
@implicitNotFound("No Body reader found for type ${T}. Try to implement an implicit BodyReader for this type.")
protected[silhouette] trait BodyReader[T] extends (Body => Try[T])

/**
 * Transforms an instance of `T` into a [[Body]].
 *
 * @tparam T The source type on the write operation
 */
@implicitNotFound("No Body transformer found for type ${T}. Try to implement an implicit BodyWriter for this type.")
protected[silhouette] trait BodyWriter[T] extends (T => Body)

/**
 * The only aim of this object is to provide a default implicit [[BodyReader]], that uses
 * the [[DefaultBodyReader]] trait to provide the lowest implicit conversion chain.
 */
private[silhouette] object BodyReader extends DefaultBodyReader

/**
 * The only aim of this object is to provide a default implicit [[BodyWriter]], that uses
 * the [[DefaultBodyWriter]] trait to provide the lowest implicit conversion chain.
 */
private[silhouette] object BodyWriter extends DefaultBodyWriter

/**
 * Provides implicit [[BodyReader]] for the body types supported by Silhouette.
 */
private[silhouette] trait DefaultBodyReader {

  /**
   * Transforms a [[Body]] into string.
   *
   * @return A [[BodyReader]] instance that transforms a [[Body]] into a string.
   */
  implicit val stringReads: BodyReader[TextBody.Type] = {
    case TextBody(codec, bytes) =>
      Try(new String(bytes.toArray, codec.charSet))
    case Body(ct, _, _) =>
      Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(TextBody.contentType, ct)))
  }

  /**
   * Transforms a [[Body]] into form URL encoded string.
   *
   * @return A [[BodyReader]] instance that transforms a [[Body]] into a form URL encoded string.
   */
  implicit val formUrlEncodedReads: BodyReader[FormUrlEncodedBody.Type] = {
    case FormUrlEncodedBody(codec, bytes) =>
      Try {
        val data = new String(bytes.toArray, codec.charSet)
        val split = "[&;]".r.split(data)
        val pairs: Seq[(String, String)] =
          if (split.length == 1 && split(0).isEmpty)
            Seq.empty
          else
            ArraySeq.unsafeWrapArray(split).map { param =>
              val parts = param.split("=", -1)
              val key = URLDecoder.decode(parts(0), codec.charSet.name())
              val value = URLDecoder.decode(parts.lift(1).getOrElse(""), codec.charSet.name())
              key -> value
            }

        pairs
          .groupBy(_._1)
          .iterator
          .map(param => param._1 -> param._2.map(_._2))
          .toMap
      }
    case Body(ct, _, _) =>
      Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(FormUrlEncodedBody.contentType, ct)))
  }

  /**
   * Transforms a [[Body]] into a Circe JSON object.
   *
   * @return A [[BodyReader]] instance that transforms a [[Body]] into a Circe JSON object.
   */
  implicit val circeJsonReads: BodyReader[JsonBody.Type] = {
    case JsonBody(codec, bytes) =>
      parse(new String(bytes.toArray, codec.charSet)) match {
        case Left(ParsingFailure(msg, e)) => Failure(new TransformException(msg, Option(e)))
        case Right(json)                  => Success(json)
      }
    case Body(ct, _, _) =>
      Failure(
        new UnsupportedContentTypeException(
          UnsupportedContentType.format(JsonBody.allowedTypes.mkString(", "), ct)
        )
      )
  }

  /**
   * Transforms a [[Body]] into a Scala XML object.
   *
   * @return A [[BodyReader]] instance that transforms a [[Body]] into a Scala XML object.
   */
  implicit val scalaXmlReads: BodyReader[XmlBody.Type] = {
    case XmlBody(codec, bytes) =>
      Try(XML.loadString(new String(bytes.toArray, codec.charSet))).recoverWith { case e: SAXParseException =>
        Failure(new TransformException(e.getMessage, Option(e)))
      }
    case Body(ct, _, _) =>
      Failure(
        new UnsupportedContentTypeException(
          UnsupportedContentType.format(XmlBody.allowedTypes.mkString(", "), ct)
        )
      )
  }
}

/**
 * Provides implicit [[BodyWriter]] for the body types supported by Silhouette.
 */
private[silhouette] trait DefaultBodyWriter {
  import Body._

  /**
   * Transforms a string into a [[Body]].
   *
   * @return A [[BodyWriter]] instance that transforms a string into a [[Body]].
   */
  implicit val stringWrites: Codec => BodyWriter[TextBody.Type] = (codec: Codec) =>
    (str: TextBody.Type) => {
      Body(MediaType.TextPlain, codec, str.getBytes(codec.charSet))
    }

  /**
   * Transforms a form URL encoded string into [[Body]].
   *
   * @return A [[BodyWriter]] instance that transforms a form URL encoded string into [[Body]].
   */
  implicit val formUrlEncodedWrites: Codec => BodyWriter[FormUrlEncodedBody.Type] = {
    codec: Codec => formData: FormUrlEncodedBody.Type =>
      val charset = codec.charSet.name()
      val urlEncodedString = formData
        .flatMap { item =>
          item._2.map(c => s"${item._1}=${URLEncoder.encode(c, charset)}")
        }
        .mkString("&")
      Body(FormUrlEncodedBody.contentType, codec, urlEncodedString.getBytes(codec.charSet))
  }

  /**
   * Transforms a Circe JSON object into a [[Body]].
   *
   * @return A [[BodyWriter]] instance that transforms a Circe JSON object into a [[Body]].
   */
  implicit val circeJsonWrites: Codec => BodyWriter[JsonBody.Type] = (codec: Codec) =>
    (json: JsonBody.Type) => {
      Body(JsonBody.contentType, codec, json.noSpaces.getBytes(codec.charSet))
    }

  /**
   * Transforms a Scala XML object into a [[Body]] and vice versa.
   *
   * @return A [[BodyWriter]] instance that transforms a Scala XML object into a [[Body]] and vice versa.
   */
  implicit val scalaXmlWrites: Codec => BodyWriter[XmlBody.Type] = (codec: Codec) =>
    (xml: XmlBody.Type) => {
      Body(XmlBody.contentType, codec, xml.mkString.getBytes(codec.charSet))
    }
}

/**
 * The companion object.
 */
private[silhouette] object DefaultBodyReader {

  /**
   * The error messages.
   */
  val UnsupportedContentType: String = "Expected `%s` but found `%s`"
}
