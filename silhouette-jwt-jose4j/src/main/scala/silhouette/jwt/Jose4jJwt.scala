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

import org.jose4j.jws.AlgorithmIdentifiers._
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.jwt.{ JwtClaims => JJwtClaims }

import scala.util.Try

/**
 * Concrete implementations for the Jose4j JWT implementation.
 */
object Jose4jJwt {

  /**
   * HMAC based algorithms.
   */
  case object HS256 extends JwsHmacAlgorithm[String] { def get: String = HMAC_SHA256 }
  case object HS384 extends JwsHmacAlgorithm[String] { def get: String = HMAC_SHA384 }
  case object HS512 extends JwsHmacAlgorithm[String] { def get: String = HMAC_SHA512 }

  /**
   * Asymmetric RSA cryptography based algorithms.
   */
  case object RS256 extends JwsRsaAlgorithm[String] { def get: String = RSA_USING_SHA256 }
  case object RS384 extends JwsRsaAlgorithm[String] { def get: String = RSA_USING_SHA384 }
  case object RS512 extends JwsRsaAlgorithm[String] { def get: String = RSA_USING_SHA512 }

  /**
   * Asymmetric EC(elliptic-curve) cryptography based algorithms.
   */
  case object ES256 extends JwsEcAlgorithm[String] { def get: String = ECDSA_USING_P256_CURVE_AND_SHA256 }
  case object ES384 extends JwsEcAlgorithm[String] { def get: String = ECDSA_USING_P384_CURVE_AND_SHA384 }
  case object ES512 extends JwsEcAlgorithm[String] { def get: String = ECDSA_USING_P521_CURVE_AND_SHA512 }

  /**
   * Asymmetric RSA-PSS cryptography based algorithms.
   *
   * This algorithms requires the Bouncy Castle JCA provider (or another provider which supports RSASSA-PSS).
   */
  case object PS256 extends JwsRsaPssAlgorithm[String] { def get: String = RSA_PSS_USING_SHA256 }
  case object PS384 extends JwsRsaPssAlgorithm[String] { def get: String = RSA_PSS_USING_SHA384 }
  case object PS512 extends JwsRsaPssAlgorithm[String] { def get: String = RSA_PSS_USING_SHA512 }

  /**
   * A simple JWT producer which supports common JWS algorithms.
   *
   * @param jwsConfiguration The JWS configuration.
   */
  class SimpleProducer(jwsConfiguration: JwsConfiguration[String]) extends Jose4jJwtProducer {

    /**
     * Produces claims and returns a JWT as string.
     *
     * @param claims The claims to produce.
     * @return The produced JWT string.
     */
    override def produce(claims: JJwtClaims): String = {
      val jws = new JsonWebSignature()
      jws.setPayload(claims.toJson)
      jwsConfiguration match {
        case JwsHmacConfiguration(algorithm, key) =>
          jws.setAlgorithmHeaderValue(algorithm.get)
          jws.setKey(key)
        case JwsRsaConfiguration(algorithm, _, privateKey) =>
          jws.setAlgorithmHeaderValue(algorithm.get)
          jws.setKey(privateKey)
        case JwsEcConfiguration(algorithm, _, privateKey) =>
          jws.setAlgorithmHeaderValue(algorithm.get)
          jws.setKey(privateKey)
        case JwsRsaPssConfiguration(algorithm, _, privateKey) =>
          jws.setAlgorithmHeaderValue(algorithm.get)
          jws.setKey(privateKey)
      }
      jws.getCompactSerialization
    }
  }

  /**
   * A simple JWT consumer which supports common JWS algorithms.
   *
   * @param configuration The consumer configuration.
   */
  class SimpleConsumer(configuration: SimpleConsumerConfiguration) extends Jose4jJwtConsumer {
    type BuilderPipeline = PartialFunction[JwtConsumerBuilder, JwtConsumerBuilder]

    /**
     * Consumes a JWT and returns [[org.jose4j.jwt.JwtClaims]].
     *
     * @param jwt The JWT token to consume.
     * @return The [[org.jose4j.jwt.JwtClaims]] extracted from the JWT token on success, otherwise an failure.
     */
    override def consume(jwt: String): Try[JJwtClaims] = {
      Try(new JwtConsumerBuilder())
        .map(jws)
        .map(requireSubject)
        .map(requireJwtID)
        .map(requireExpirationTime)
        .map(requireIssuedAt)
        .map(requireNotBefore)
        .map(expectedIssuer)
        .map(expectedAudience)
        .map(_.build().processToClaims(jwt))
    }

    /**
     * Maps the `jws` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
     *
     * @return The builder pipeline.
     */
    private def jws: BuilderPipeline = {
      case builder =>
        configuration.jws match {
          case JwsHmacConfiguration(_, key) =>
            builder.setVerificationKey(key)
          case JwsRsaConfiguration(_, publicKey, _) =>
            builder.setVerificationKey(publicKey)
          case JwsEcConfiguration(_, publicKey, _) =>
            builder.setVerificationKey(publicKey)
          case JwsRsaPssConfiguration(_, publicKey, _) =>
            builder.setVerificationKey(publicKey)
        }
    }

    /**
     * Maps the `requireSubject` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
     *
     * @return The builder pipeline.
     */
    private def requireSubject: BuilderPipeline = {
      case builder if configuration.requireSubject => builder.setRequireSubject()
      case builder                                 => builder
    }

    /**
     * Maps the `requireJwtID` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
     *
     * @return The builder pipeline.
     */
    private def requireJwtID: BuilderPipeline = {
      case builder if configuration.requireJwtID => builder.setRequireJwtId()
      case builder                               => builder
    }

    /**
     * Maps the `requireExpirationTime` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
     *
     * @return The builder pipeline.
     */
    private def requireExpirationTime: BuilderPipeline = {
      case builder if configuration.requireExpirationTime => builder.setRequireExpirationTime()
      case builder                                        => builder
    }

    /**
     * Maps the `requireIssuedAt` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
     *
     * @return The builder pipeline.
     */
    private def requireIssuedAt: BuilderPipeline = {
      case builder if configuration.requireIssuedAt => builder.setRequireIssuedAt()
      case builder                                  => builder
    }

    /**
     * Maps the `requireNotBefore` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
     *
     * @return The builder pipeline.
     */
    private def requireNotBefore: BuilderPipeline = {
      case builder if configuration.requireNotBefore => builder.setRequireNotBefore()
      case builder                                   => builder
    }

    /**
     * Maps the `expectedIssuer` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
     *
     * @return The builder pipeline.
     */
    private def expectedIssuer: BuilderPipeline = {
      case builder =>
        configuration.expectedIssuer.map(issuer => builder.setExpectedIssuer(issuer)).getOrElse(builder)
    }

    /**
     * Maps the `expectedAudience` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
     *
     * @return The builder pipeline.
     */
    private def expectedAudience: BuilderPipeline = {
      case builder =>
        configuration.expectedAudience.map(audience => builder.setExpectedAudience(audience: _*)).getOrElse(builder)
    }
  }

  /**
   * The simple consumer configuration.
   *
   * @param jws                   The JWS configuration.
   * @param requireSubject        Indicates if the 'sub' claim must be set during JWT consumption.
   * @param requireJwtID          Indicates if the 'jti' claim must be set during JWT consumption.
   * @param requireExpirationTime Indicates if the 'exp' claim must be set during JWT consumption.
   * @param requireIssuedAt       Indicates if the 'iat' claim must be set during JWT consumption.
   * @param requireNotBefore      Indicates if the 'nbf' claim must be set during JWT consumption.
   * @param expectedIssuer        Indicates the expected value of the issuer ("iss") claim and that the claim is
   *                              required.
   * @param expectedAudience      Set the audience value(s) to use when validating the audience ("aud") claim of a JWT
   *                              and require that an audience claim be present.
   */
  case class SimpleConsumerConfiguration(
    jws: JwsConfiguration[String],
    requireSubject: Boolean = false,
    requireJwtID: Boolean = false,
    requireExpirationTime: Boolean = false,
    requireIssuedAt: Boolean = false,
    requireNotBefore: Boolean = false,
    expectedIssuer: Option[String] = None,
    expectedAudience: Option[List[String]] = None
  )
}
