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

import io.circe.{ Json, JsonObject }
import org.jose4j.jwt.JwtClaims
import silhouette.jwt.jose4j.Jose4jClaimReader._
import silhouette.jwt.{ JwtClaimReader, Claims, JwtException, ReservedClaims }

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

/**
 * JWT claim reader based on the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The library supports the JWS/JWE compact serializations with the complete suite of JOSE algorithms.
 *
 * @param consumer The JWT consumer.
 */
final case class Jose4jClaimReader(consumer: Jose4jConsumer) extends JwtClaimReader {

  /**
   * Transforms a JWT string into a JWT claims object.
   *
   * @param str A JWT string.
   * @return The transformed JWT claims object or an error if the string couldn't be transformed.
   */
  override def apply(str: String): Try[Claims] = {
    (for {
      jwtClaims <- consumer.consume(str)
      silhouetteClaims <- toSilhouette(jwtClaims)
    } yield {
      silhouetteClaims
    }).recoverWith {
      case e =>
        Failure(new JwtException(FraudulentJwtToken.format(str), Some(e)))
    }
  }

  /**
   * Converts the jose4j claims instance to a Silhouette claims instance.
   *
   * @param claims The jose4j claims instance.
   * @return The Silhouette claims instance on success, a failure on error.
   */
  private def toSilhouette(claims: JwtClaims): Try[Claims] = {
    val maybeCustom = claims.getClaimsMap(ReservedClaims.asJava).asScala match {
      case l if l.isEmpty => Success(JsonObject.empty)
      case l              => transformCustomClaims(l.asJava)
    }

    maybeCustom.map { custom =>
      Claims(
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
        custom = custom
      )
    }
  }

  /**
   * Transforms recursively the custom claims.
   *
   * @param claims The custom claims to Transforms.
   * @return A Json object representing the custom claims on success, otherwise a failure.
   */
  private def transformCustomClaims(claims: java.util.Map[String, Object]): Try[JsonObject] = {
    def fromNumber(number: Number): Json = number match {
      case v: java.lang.Integer => Json.fromInt(v)
      case v: java.lang.Long    => Json.fromLong(v)
      case v: java.lang.Float   => Json.fromFloatOrNull(v)
      case v: java.lang.Double  => Json.fromDoubleOrNull(v)
    }
    def fromMap(map: java.util.Map[_, _]): Try[Json] = {
      transformCustomClaims(map.asInstanceOf[java.util.Map[String, Object]]).map(Json.fromJsonObject)
    }
    def toJson(value: Any): Try[Json] = Option(value) match {
      case None                          => Success(Json.Null)
      case Some(v: java.lang.String)     => Success(Json.fromString(v))
      case Some(v: java.math.BigInteger) => Success(Json.fromBigInt(v))
      case Some(v: java.math.BigDecimal) => Success(Json.fromBigDecimal(v))
      case Some(v: java.lang.Number)     => Success(fromNumber(v))
      case Some(v: java.lang.Boolean)    => Success(Json.fromBoolean(v))
      case Some(v: java.util.List[_])    => Try(Json.arr(v.asScala.toVector.map(value => toJson(value).get): _*))
      case Some(v: java.util.Map[_, _])  => fromMap(v)
      case Some(v)                       => Failure(new JwtException(UnexpectedJsonValue.format(v)))
    }

    Try(JsonObject.fromIterable(claims.asScala.toMap.map { case (name, value) => name -> toJson(value).get }))
  }
}

/**
 * The companion object.
 */
object Jose4jClaimReader {

  /**
   * The error messages.
   */
  val FraudulentJwtToken: String = "Fraudulent JWT token: %s"
  val UnexpectedJsonValue: String = "Unexpected Json value: %s"
}
