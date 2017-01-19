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
package silhouette.http.decoder

import io.circe.{ Json, ParsingFailure }
import io.circe.parser._
import silhouette.exceptions.{ DecoderException, UnsupportedContentTypeException }
import silhouette.http.client.{ Body, ContentTypes }
import scala.util.{ Failure, Try }
import scala.xml.NodeSeq
import DefaultBodyDecoder._

/**
 * Provide implicit decoder for default value.
 */
trait DefaultBodyDecoder {
  implicit def circeJsonDecoder: BodyDecoder[Json] = new BodyDecoder[Json] {
    override def decode(in: Body): Try[Json] = in match {
      case Body(ContentTypes.`application/json`, charset, bytes) =>
        parse(new String(bytes, charset)).toTry.recover {
          case e @ ParsingFailure(msg, underlying) => throw new DecoderException(msg, Option(e))
        }
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(ContentTypes.`application/json`, ct)))
    }
  }

  implicit def scalaXmlDecoder: BodyDecoder[NodeSeq] = new BodyDecoder[NodeSeq] {
    override def decode(in: Body): Try[NodeSeq] = in match {
      case Body(ContentTypes.`application/xml`, charset, bytes) =>
        Try(scala.xml.XML.loadString(new String(bytes, charset))).recover {
          case e: org.xml.sax.SAXParseException => throw new DecoderException(e.getMessage, Option(e))
        }
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(ContentTypes.`application/xml`, ct)))
    }
  }

  implicit def stringDecoder: BodyDecoder[String] = new BodyDecoder[String] {
    override def decode(in: Body): Try[String] = in match {
      case Body(_, charset, bytes) => Try(new String(bytes, charset))
    }
  }
}

/**
 * The companion object.
 */
object DefaultBodyDecoder {

  /**
   * The error messages.
   */
  val UnsupportedContentType: String = "[Silhouette][DefaultBodyDecoder] Expected %s but found %s"

}
