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
package silhouette.jwt

import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.jwt.{ NumericDate, JwtClaims => JJwtClaims }
import org.jose4j.jwx.JsonWebStructure
import silhouette.exceptions.JwtException
import silhouette.jwt.Jose4jJwtGenerator._

import scala.collection.JavaConverters._
import scala.json.ast._
import scala.util.{ Failure, Try }

/**
 * JWT encoder/decoder which is based on the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The library supports the JWS/JWE compact serializations with the complete suite of JOSE algorithms.
 *
 * @param producer The JWT producer.
 * @param consumer The JWT consumer.
 */
final class Jose4jJwtGenerator(
  producer: Jose4jJwtProducer,
  consumer: Jose4jJwtConsumer)
  extends JwtGenerator {

  /**
   * Monkey patch the [org.jose4j.jwt.JwtClaims] instance.
   *
   * @param claims The jose4j claims instance to convert.
   */
  private implicit class RichJJwtClaims(claims: JJwtClaims) {

    /**
     * Converts the jose4j claims instance to a Silhouette claims instance.
     *
     * @return The Silhouette claims instance.
     */
    def toSilhouette: JwtClaims = {
      JwtClaims(
        issuer = Option(claims.getIssuer),
        subject = Option(claims.getSubject),
        audience = Option(claims.getAudience).map(_.asScala.toList) match {
          case Some(Nil) => None
          case s         => s
        },
        expirationTime = Option(claims.getExpirationTime).map(_.getValue),
        notBefore = Option(claims.getNotBefore).map(_.getValue),
        issuedAt = Option(claims.getIssuedAt).map(_.getValue),
        jwtID = Option(claims.getJwtId),
        custom = claims.getClaimsMap(ReservedClaims.asJava).asScala match {
          case l if l.isEmpty => JObject()
          case l              => decodeCustomClaims(l.asJava)
        }
      )
    }
  }

  /**
   * Monkey patch the [JwtClaims] instance.
   *
   * @param claims The Silhouette claims instance to convert.
   * @return The jose4j claims instance.
   */
  private implicit class RichJwtClaims(claims: JwtClaims) {

    /**
     * Converts the Silhouette claims instance to a jose4j claims instance.
     *
     * @return The jose4j claims instance.
     */
    def toJose4j: JJwtClaims = {
      val result = new JJwtClaims()
      claims.issuer.foreach(result.setIssuer)
      claims.subject.foreach(result.setSubject)
      claims.audience.foreach(v => result.setAudience(v.asJava))
      claims.expirationTime.foreach(v => result.setExpirationTime(NumericDate.fromSeconds(v)))
      claims.notBefore.foreach(v => result.setNotBefore(NumericDate.fromSeconds(v)))
      claims.issuedAt.foreach(v => result.setIssuedAt(NumericDate.fromSeconds(v)))
      claims.jwtID.foreach(result.setJwtId)
      encodeCustomClaims(claims.custom).asScala.foreach {
        case (k, v) =>
          if (ReservedClaims.contains(k)) {
            throw new JwtException(OverrideReservedClaim.format(k, ReservedClaims.mkString(", ")))
          }
          result.setClaim(k, v)
      }
      result
    }
  }

  /**
   * Encodes a JWT claims object and returns a JWT as string.
   *
   * @param jwt The JWT claims object to encode.
   * @return The JWT string representation or an error if the JWT claims object couldn't be encoded.
   */
  override def encode(jwt: JwtClaims): Try[String] = Try(producer.produce(jwt.toJose4j))

  /**
   * Decodes a JWT string and returns a JWT claims object.
   *
   * @param str A JWT string.
   * @return The decoded JWT claims object or an error if the string couldn't be decoded.
   */
  override def decode(str: String): Try[JwtClaims] = {
    consumer.consume(str).map(_.toSilhouette).recoverWith {
      case e =>
        Failure(new JwtException(FraudulentJwtToken.format(str), Some(e)))
    }
  }

  /**
   * Encodes recursively the custom claims.
   *
   * @param claims The custom claims to encode.
   * @return A map containing custom claims.
   */
  private def encodeCustomClaims(claims: JObject): java.util.Map[String, Object] = {
    def toJava(value: JValue): Object = value match {
      case JNull       => None.orNull
      case v: JString  => v.value
      case v: JNumber  => BigDecimal(v.value)
      case v: JBoolean => Boolean.box(v.get)
      case v: JArray   => v.value.map(toJava).asJava
      case v: JObject  => encodeCustomClaims(v)
    }

    claims.value.map { case (name, value) => name -> toJava(value) }.asJava
  }

  /**
   * Decodes recursively the custom claims.
   *
   * @param claims The custom claims to decode.
   * @return A Json object representing the custom claims.
   */
  private def decodeCustomClaims(claims: java.util.Map[String, Object]): JObject = {
    def toJson(value: Any): JValue = Option(value) match {
      case None                         => JNull
      case Some(v: java.lang.String)    => JString(v)
      case Some(v: java.lang.Number)    => JNumber(v.toString)
      case Some(v: java.lang.Boolean)   => JBoolean(v)
      case Some(v: java.util.List[_])   => JArray(v.asScala.toVector.map(toJson))
      case Some(v: java.util.Map[_, _]) => decodeCustomClaims(v.asInstanceOf[java.util.Map[String, Object]])
      case Some(v)                      => throw new JwtException(UnexpectedJsonValue.format(v))
    }

    JObject(claims.asScala.toMap.map { case (name, value) => name -> toJson(value) })
  }
}

/**
 * The companion object.
 */
object Jose4jJwtGenerator {

  /**
   * The reserved claims.
   */
  val ReservedClaims: Set[String] = Set("iss", "sub", "aud", "exp", "nbf", "iat", "jti")

  /**
   * The error messages.
   */
  val FraudulentJwtToken: String = "[Silhouette][Jose4jJwtGenerator] Fraudulent JWT token: %s"
  val OverrideReservedClaim: String = "[Silhouette][Jose4jJwtGenerator] Try to overriding a reserved claim `%s`; " +
    "list of reserved claims: %s"
  val UnexpectedJsonValue: String = "[Silhouette][Jose4jJwtGenerator] Unexpected Json value: %s"
}

/**
 * Produces JWT tokens with the help of the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The production of a JWT can be very complex, especially with the jose4j library because of it's feature richness.
 * A JWT can use different encryption and signing algorithms, it can be nested or it can use the two-pass consumption
 * approach. Therefore we allow a user to define it's own [[org.jose4j.jwx.JsonWebStructure]] implementation for the
 * given claims.
 *
 * Please visit the [documentation](https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples) to see how a
 * [[org.jose4j.jwe.JsonWebEncryption]] or a [[org.jose4j.jws.JsonWebSignature]] can be configured.
 */
trait Jose4jJwtProducer {

  /**
   * Produces claims and returns a JWT as string.
   *
   * @param claims The claims to produce.
   * @return The produced JWT string.
   */
  def produce(claims: JJwtClaims): String
}

/**
 * Consumes JWT tokens with the help of the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The consumption of a JWT can be very complex, especially with the jose4j library because of it's feature richness.
 * A JWT can use different encryption and signing algorithms, it can be nested or it can use the two-pass consumption
 * approach. Therefore we allow a user to define it's own [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
 *
 * Please visit the [documentation](https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples) to see how the
 * [[org.jose4j.jwt.consumer.JwtConsumerBuilder]] can be configured.
 */
trait Jose4jJwtConsumer {

  /**
   * Consumes a JWT and returns [[org.jose4j.jwt.JwtClaims]].
   *
   * @param jwt The JWT token to consume.
   * @return The [[org.jose4j.jwt.JwtClaims]] extracted from the JWT token on success, otherwise an failure.
   */
  def consume(jwt: String): Try[JJwtClaims]
}
