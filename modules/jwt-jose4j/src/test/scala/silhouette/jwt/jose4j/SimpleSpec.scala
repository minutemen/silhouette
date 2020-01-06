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

import org.jose4j.jwk.{ EcJwkGenerator, RsaJwkGenerator }
import org.jose4j.keys.{ EllipticCurves, HmacKey }
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.crypto.Hash._
import silhouette.jwt.{ JwtException, _ }
import silhouette.jwt.jose4j.Jose4jClaimReader._
import silhouette.specs2.WithBouncyCastle

/**
 * Test case for the [[SimpleJose4jConsumer]] and [[SimpleJose4jProducer]] implementations.
 */
class SimpleSpec extends Specification with Mockito with WithBouncyCastle {

  "The `transformer`" should {
    "transform a JWT with the `HS256` algorithm" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))

      transform(claims)
    }

    "transform a JWT with the `HS384` algorithm" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS384, new HmacKey(sha384("some.secret")))

      transform(claims)
    }

    "transform a JWT with the `HS512` algorithm" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS512, new HmacKey(sha512("some.secret")))

      transform(claims)
    }

    "transform a JWT with the `RS256` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaConfiguration(RS256, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      transform(claims)
    }

    "transform a JWT with the `RS384` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaConfiguration(RS384, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      transform(claims)
    }

    "transform a JWT with the `RS512` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaConfiguration(RS512, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      transform(claims)
    }

    "transform a JWT with the `ES256` algorithm" in new Context {
      override val ecParameterSpec = EllipticCurves.P256
      override val jwsConfiguration = JwsEcConfiguration(ES256, ecJwk.getECPublicKey, ecJwk.getEcPrivateKey)

      transform(claims)
    }

    "transform a JWT with the `ES384` algorithm" in new Context {
      override val ecParameterSpec = EllipticCurves.P384
      override val jwsConfiguration = JwsEcConfiguration(ES384, ecJwk.getECPublicKey, ecJwk.getEcPrivateKey)

      transform(claims)
    }

    "transform a JWT with the `ES512` algorithm" in new Context {
      override val ecParameterSpec = EllipticCurves.P521
      override val jwsConfiguration = JwsEcConfiguration(ES512, ecJwk.getECPublicKey, ecJwk.getEcPrivateKey)

      transform(claims)
    }

    "transform a JWT with the `PS256` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaPssConfiguration(PS256, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      transform(claims)
    }

    "transform a JWT with the `PS384` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaPssConfiguration(PS384, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      transform(claims)
    }

    "transform a JWT with the `PS512` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaPssConfiguration(PS512, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      transform(claims)
    }
  }

  "The `consumer`" should {
    "throw an JwtException if the 'sub' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      override lazy val claimReader = Jose4jClaimReader(consumer.copy(requireSubject = true))

      fraudulent(Claims())
    }

    "throw an JwtException if the 'jti' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      override lazy val claimReader = Jose4jClaimReader(consumer.copy(requireJwtID = true))

      fraudulent(Claims())
    }

    "throw an JwtException if the 'exp' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      override lazy val claimReader = Jose4jClaimReader(consumer.copy(requireExpirationTime = true))

      fraudulent(Claims())
    }

    "throw an JwtException if the 'iat' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      override lazy val claimReader = Jose4jClaimReader(consumer.copy(requireIssuedAt = true))

      fraudulent(Claims())
    }

    "throw an JwtException if the 'nbf' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      override lazy val claimReader = Jose4jClaimReader(consumer.copy(requireNotBefore = true))

      fraudulent(Claims())
    }

    "throw an JwtException if the expected issuer is not available in the JWT" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      override lazy val claimReader = Jose4jClaimReader(consumer.copy(expectedIssuer = Some("test1")))

      fraudulent(Claims(issuer = Some("test2")))
    }

    "throw an JwtException if the expected audience is not available in the JWT" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      override lazy val claimReader = Jose4jClaimReader(consumer.copy(expectedAudience = Some(List("test1", "test2"))))

      fraudulent(Claims(audience = Some(List("test3"))))
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The test claims.
     */
    val claims = Claims()

    /**
     * The JWS configuration.
     */
    val jwsConfiguration: JwsConfiguration[String]

    /**
     * The EC parameter spec.
     */
    val ecParameterSpec = EllipticCurves.P256

    /**
     * A RSA JWK.
     *
     * This is used to generate a key pair for RSA based algorithms.
     */
    val rsaJwk = RsaJwkGenerator.generateJwk(2048)

    /**
     * A EC JWK.
     *
     * This is used to generate a key pair for EC based algorithms.
     */
    lazy val ecJwk = EcJwkGenerator.generateJwk(ecParameterSpec)

    /**
     * The simple producer.
     */
    lazy val producer = SimpleJose4jProducer(jwsConfiguration)

    /**
     * The simple consumer.
     */
    lazy val consumer = SimpleJose4jConsumer(jwsConfiguration)

    /**
     * The reads to test.
     */
    lazy val claimReader = Jose4jClaimReader(consumer)

    /**
     * The writes to test.
     */
    lazy val claimWriter = Jose4jClaimWriter(producer)

    /**
     * A helper method which transforms claims into a JWT and vice versa to check if the same
     * claims were transformed.
     *
     * @param claims The claims to check for.
     * @return A Specs2 match result.
     */
    protected def transform(claims: Claims): MatchResult[Any] = {
      claimWriter(claims) must beSuccessfulTry.like {
        case jwt =>
          claimReader(jwt) must beSuccessfulTry.withValue(claims)
      }
    }

    /**
     * A helper method which transforms claims into a JWT and vice versa to check if the consumer
     * throws an exception which indicates that the token is fraudulent.
     *
     * @param claims The claims to check for.
     * @return A Specs2 match result.
     */
    protected def fraudulent(claims: Claims): MatchResult[Any] = {
      claimWriter(claims) must beSuccessfulTry.like {
        case jwt =>
          claimReader(jwt) must beFailedTry.like {
            case e: JwtException => e.getMessage must startWith(FraudulentJwtToken.format(""))
          }
      }
    }
  }
}
