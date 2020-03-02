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
import silhouette.jwt.{ Claims, JwtClaimReader, JwtException, ReservedClaims }

import scala.jdk.CollectionConverters._

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
   * @return The transformed JWT claims object on the or an error if the string couldn't be transformed.
   */
  override def apply(str: String): Either[Throwable, Claims] = {
    (for {
      jwtClaims <- consumer.consume(str)
      silhouetteClaims <- toSilhouette(jwtClaims)
    } yield {
      silhouetteClaims
    }).left.map { e =>
      new JwtException(FraudulentJwtToken.format(str), Some(e))
    }
  }

  /**
   * Converts the jose4j claims instance to a Silhouette claims instance.
   *
   * @param claims The jose4j claims instance.
   * @return The Silhouette claims instance on the right or an error on the left.
   */
  private def toSilhouette(claims: JwtClaims): Either[Throwable, Claims] = {
    val maybeCustom = claims.getClaimsMap(ReservedClaims.asJava).asScala match {
      case l if l.isEmpty => Right(JsonObject.empty)
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
   * @return A Json object representing the custom claims on the right or an error on the left.
   */
  private def transformCustomClaims(claims: java.util.Map[String, Object]): Either[Throwable, JsonObject] = {
    import cats.instances.either._
    import cats.instances.list._
    import cats.syntax.traverse._
    def fromNumber(number: Number): Json = number match {
      case v: java.lang.Integer => Json.fromInt(v)
      case v: java.lang.Long    => Json.fromLong(v)
      case v: java.lang.Float   => Json.fromFloatOrNull(v)
      case v: java.lang.Double  => Json.fromDoubleOrNull(v)
    }
    def fromList(list: java.util.List[_]): Either[Throwable, Json] = {
      list.asScala.toList.map(value => toJson(value)).sequence.map(Json.arr)
    }
    def fromMap(map: java.util.Map[_, _]): Either[Throwable, JsonObject] = {
      transformCustomClaims(map.asInstanceOf[java.util.Map[String, Object]])
    }
    def toJson(value: Any): Either[Throwable, Json] = Option(value) match {
      case None                          => Right(Json.Null)
      case Some(v: java.lang.String)     => Right(Json.fromString(v))
      case Some(v: java.math.BigInteger) => Right(Json.fromBigInt(v))
      case Some(v: java.math.BigDecimal) => Right(Json.fromBigDecimal(v))
      case Some(v: java.lang.Number)     => Right(fromNumber(v))
      case Some(v: java.lang.Boolean)    => Right(Json.fromBoolean(v))
      case Some(v: java.util.List[_])    => fromList(v)
      case Some(v: java.util.Map[_, _])  => fromMap(v).map(Json.fromJsonObject)
      case Some(v)                       => Left(new JwtException(UnexpectedJsonValue.format(v)))
    }

    fromMap(claims)
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
