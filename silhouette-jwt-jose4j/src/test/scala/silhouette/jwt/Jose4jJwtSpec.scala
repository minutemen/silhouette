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

import org.jose4j.jwk.{ EcJwkGenerator, RsaJwkGenerator }
import org.jose4j.keys.{ EllipticCurves, HmacKey }
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.crypto.Hash._
import silhouette.exceptions.JwtException
import silhouette.jwt.Jose4jJwt._
import silhouette.jwt.Jose4JJwtFormat._
import silhouette.specs2.WithBouncyCastle

/**
 * Test case for the [[Jose4jJwt]] default implementations.
 */
class Jose4jJwtSpec extends Specification with Mockito with WithBouncyCastle {

  "The `generator`" should {
    "generate a JWT with the `HS256` algorithm" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))

      generate(claims)
    }

    "generate a JWT with the `HS384` algorithm" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS384, new HmacKey(sha384("some.secret")))

      generate(claims)
    }

    "generate a JWT with the `HS512` algorithm" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS512, new HmacKey(sha512("some.secret")))

      generate(claims)
    }

    "generate a JWT with the `RS256` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaConfiguration(RS256, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      generate(claims)
    }

    "generate a JWT with the `RS384` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaConfiguration(RS384, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      generate(claims)
    }

    "generate a JWT with the `RS512` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaConfiguration(RS512, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      generate(claims)
    }

    "generate a JWT with the `ES256` algorithm" in new Context {
      override val ecParameterSpec = EllipticCurves.P256
      override val jwsConfiguration = JwsEcConfiguration(ES256, ecJwk.getECPublicKey, ecJwk.getEcPrivateKey)

      generate(claims)
    }

    "generate a JWT with the `ES384` algorithm" in new Context {
      override val ecParameterSpec = EllipticCurves.P384
      override val jwsConfiguration = JwsEcConfiguration(ES384, ecJwk.getECPublicKey, ecJwk.getEcPrivateKey)

      generate(claims)
    }

    "generate a JWT with the `ES512` algorithm" in new Context {
      override val ecParameterSpec = EllipticCurves.P521
      override val jwsConfiguration = JwsEcConfiguration(ES512, ecJwk.getECPublicKey, ecJwk.getEcPrivateKey)

      generate(claims)
    }

    "generate a JWT with the `PS256` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaPssConfiguration(PS256, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      generate(claims)
    }

    "generate a JWT with the `PS384` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaPssConfiguration(PS384, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      generate(claims)
    }

    "generate a JWT with the `PS512` algorithm" in new Context {
      override val jwsConfiguration = JwsRsaPssConfiguration(PS512, rsaJwk.getRsaPublicKey, rsaJwk.getRsaPrivateKey)

      generate(claims)
    }
  }

  "The `consumer`" should {
    "throw an JwtException if the 'sub' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      simpleConsumerConfiguration.requireSubject returns true

      fraudulent(JwtClaims())
    }

    "throw an JwtException if the 'jti' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      simpleConsumerConfiguration.requireJwtID returns true

      fraudulent(JwtClaims())
    }

    "throw an JwtException if the 'exp' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      simpleConsumerConfiguration.requireExpirationTime returns true

      fraudulent(JwtClaims())
    }

    "throw an JwtException if the 'iat' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      simpleConsumerConfiguration.requireIssuedAt returns true

      fraudulent(JwtClaims())
    }

    "throw an JwtException if the 'nbf' claim is required but missed" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      simpleConsumerConfiguration.requireNotBefore returns true

      fraudulent(JwtClaims())
    }

    "throw an JwtException if the expected issuer is not available in the JWT" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      simpleConsumerConfiguration.expectedIssuer returns Some("test1")

      fraudulent(JwtClaims(issuer = Some("test2")))
    }

    "throw an JwtException if the expected audience is not available in the JWT" in new Context {
      override val jwsConfiguration = JwsHmacConfiguration(HS256, new HmacKey(sha256("some.secret")))
      simpleConsumerConfiguration.expectedAudience returns Some(List("test1", "test2"))

      fraudulent(JwtClaims(audience = Some(List("test3"))))
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The test claims.
     */
    val claims = JwtClaims(issuer = Some("test"))

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
    lazy val rsaJwk = RsaJwkGenerator.generateJwk(2048)

    /**
     * A EC JWK.
     *
     * This is used to generate a key pair for EC based algorithms.
     */
    lazy val ecJwk = EcJwkGenerator.generateJwk(ecParameterSpec)

    /**
     * The simple consumer configuration.
     */
    lazy val simpleConsumerConfiguration = spy(SimpleConsumerConfiguration(jwsConfiguration))

    /**
     * The simple producer.
     */
    lazy val producer = new SimpleProducer(jwsConfiguration)

    /**
     * The simple consumer.
     */
    lazy val consumer = new SimpleConsumer(simpleConsumerConfiguration)

    /**
     * The generator to test.
     */
    lazy val generator = new Jose4JJwtFormat(producer, consumer)

    /**
     * A helper method which transforms claims into a JWT and vice versa to check if the same
     * claims were transformed.
     *
     * @param claims The claims to check for.
     * @return A Specs2 match result.
     */
    protected def generate(claims: JwtClaims): MatchResult[Any] = {
      generator.write(claims) must beSuccessfulTry.like {
        case jwt =>
          generator.read(jwt) must beSuccessfulTry.withValue(claims)
      }
    }

    /**
     * A helper method which transforms claims into a JWT and vice versa to check if the consumer
     * throws an exception which indicates that the token is fraudulent.
     *
     * @param claims The claims to check for.
     * @return A Specs2 match result.
     */
    protected def fraudulent(claims: JwtClaims): MatchResult[Any] = {
      generator.write(claims) must beSuccessfulTry.like {
        case jwt =>
          generator.read(jwt) must beFailedTry.like {
            case e: JwtException => e.getMessage must startWith(FraudulentJwtToken.format(""))
          }
      }
    }
  }
}
