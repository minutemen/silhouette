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
package silhouette.jwt.jose4j

import java.time.Instant

import org.jose4j.jwt.JwtClaims
import silhouette.jwt
import silhouette.jwt.exceptions.JwtException
import silhouette.jwt.jose4j.Reads._

import scala.collection.JavaConverters._
import scala.json.ast._
import scala.util.{ Failure, Try }

/**
 * JWT reads based on the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The library supports the JWS/JWE compact serializations with the complete suite of JOSE algorithms.
 *
 * @param consumer The JWT consumer.
 */
final case class Reads(consumer: Consumer) extends jwt.Reads {

  /**
   * Transforms a JWT string into a JWT claims object.
   *
   * @param str A JWT string.
   * @return The transformed JWT claims object or an error if the string couldn't be transformed.
   */
  override def read(str: String): Try[jwt.Claims] = {
    consumer.consume(str).map(toSilhouette).recoverWith {
      case e =>
        Failure(new JwtException(FraudulentJwtToken.format(str), Some(e)))
    }
  }

  /**
   * Converts the jose4j claims instance to a Silhouette claims instance.
   *
   * @param claims The jose4j claims instance.
   * @return The Silhouette claims instance.
   */
  private def toSilhouette(claims: JwtClaims): jwt.Claims = {
    jwt.Claims(
      issuer = Option(claims.getIssuer),
      subject = Option(claims.getSubject),
      audience = Option(claims.getAudience).map(_.asScala.toList) match {
        case Some(Nil) => None
        case s         => s
      },
      expirationTime = Option(claims.getExpirationTime).map(_.getValue)
        .map(seconds => Instant.ofEpochSecond(seconds)),
      notBefore = Option(claims.getNotBefore).map(_.getValue)
        .map(seconds => Instant.ofEpochSecond(seconds)),
      issuedAt = Option(claims.getIssuedAt).map(_.getValue)
        .map(seconds => Instant.ofEpochSecond(seconds)),
      jwtID = Option(claims.getJwtId),
      custom = claims.getClaimsMap(jwt.ReservedClaims.asJava).asScala match {
        case l if l.isEmpty => JObject()
        case l              => transformCustomClaims(l.asJava)
      }
    )
  }

  /**
   * Transforms recursively the custom claims.
   *
   * @param claims The custom claims to Transforms.
   * @return A Json object representing the custom claims.
   */
  private def transformCustomClaims(claims: java.util.Map[String, Object]): JObject = {
    def toJson(value: Any): JValue = Option(value) match {
      case None                         => JNull
      case Some(v: java.lang.String)    => JString(v)
      case Some(v: java.lang.Number)    => JNumber(v.toString)
      case Some(v: java.lang.Boolean)   => JBoolean(v)
      case Some(v: java.util.List[_])   => JArray(v.asScala.toVector.map(toJson))
      case Some(v: java.util.Map[_, _]) => transformCustomClaims(v.asInstanceOf[java.util.Map[String, Object]])
      case Some(v)                      => throw new JwtException(UnexpectedJsonValue.format(v))
    }

    JObject(claims.asScala.toMap.map { case (name, value) => name -> toJson(value) })
  }
}

/**
 * The companion object.
 */
object Reads {

  /**
   * The error messages.
   */
  val FraudulentJwtToken: String = "Fraudulent JWT token: %s"
  val UnexpectedJsonValue: String = "Unexpected Json value: %s"
}
