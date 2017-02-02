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

import java.time.ZonedDateTime

import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jws.AlgorithmIdentifiers._
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.jwt.{ JwtClaims => JJwtClaims }
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.exceptions.JwtException
import silhouette.jwt.Jose4JJwtFormat._
import silhouette.specs2.WithBouncyCastle

import scala.json.ast._
import scala.util.Try

/**
 * Test case for the [[Jose4JJwtFormat]] class.
 */
class Jose4jJwtFormatSpec extends Specification with WithBouncyCastle {

  "The `generator`" should {
    "generate a JWT with an `iss` claim" in new Context {
      generate(JwtClaims(issuer = Some("test")))
    }

    "generate a JWT with a `sub` claim" in new Context {
      generate(JwtClaims(subject = Some("test")))
    }

    "generate a JWT with an `aud` claim" in new Context {
      generate(JwtClaims(audience = Some(List("test1", "test2"))))
    }

    "generate a JWT with an `exp` claim" in new Context {
      generate(JwtClaims(expirationTime = Some(ZonedDateTime.now().toEpochSecond)))
    }

    "generate a JWT with a `nbf` claim" in new Context {
      generate(JwtClaims(notBefore = Some(ZonedDateTime.now().toEpochSecond)))
    }

    "generate a JWT with an `iat` claim" in new Context {
      generate(JwtClaims(issuedAt = Some(ZonedDateTime.now().toEpochSecond)))
    }

    "generate a JWT with a `jti` claim" in new Context {
      generate(JwtClaims(jwtID = Some("test")))
    }

    "generate a JWT with custom claims" in new Context {
      generate(JwtClaims(custom = customClaims))
    }

    "generate a complex JWT" in new Context {
      generate(JwtClaims(
        issuer = Some("test"),
        subject = Some("test"),
        audience = Some(List("test1", "test2")),
        expirationTime = Some(ZonedDateTime.now().toEpochSecond),
        notBefore = Some(ZonedDateTime.now().toEpochSecond),
        issuedAt = Some(ZonedDateTime.now().toEpochSecond),
        jwtID = Some("test"),
        custom = customClaims
      ))
    }
  }

  "The `write` method" should {
    "throw a JwtException if a custom claim tries to override the reserved claim `iss`" in new Context {
      reserved("iss")
    }

    "throw a JwtException if a custom claim tries to override the reserved claim `sub`" in new Context {
      reserved("sub")
    }

    "throw a JwtException if a custom claim tries to override the reserved claim `aud`" in new Context {
      reserved("aud")
    }

    "throw a JwtException if a custom claim tries to override the reserved claim `exp`" in new Context {
      reserved("exp")
    }

    "throw a JwtException if a custom claim tries to override the reserved claim `nbf`" in new Context {
      reserved("nbf")
    }

    "throw a JwtException if a custom claim tries to override the reserved claim `iat`" in new Context {
      reserved("iat")
    }

    "throw a JwtException if a custom claim tries to override the reserved claim `jti`" in new Context {
      reserved("jti")
    }
  }

  "The `read` method" should {
    "throw a JwtException if an error occurred during decoding" in new Context {
      generator.read("invalid.token") must beFailedTry.like {
        case e: JwtException => e.getMessage must be equalTo FraudulentJwtToken.format("invalid.token")
      }
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A simple producer for testing.
     */
    val producer = new Jose4jJwtProducer {
      override def produce(claims: JJwtClaims): String = {
        val jws = new JsonWebSignature()
        jws.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS)
        jws.setPayload(claims.toJson)
        jws.setAlgorithmHeaderValue(NONE)
        jws.getCompactSerialization
      }
    }

    /**
     * A simple consumer for testing.
     */
    val consumer = new Jose4jJwtConsumer {
      override def consume(jwt: String): Try[JJwtClaims] = {
        Try(new JwtConsumerBuilder())
          .map(builder => builder.setJwsAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS))
          .map(builder => builder.setDisableRequireSignature())
          .map(builder => builder.setSkipAllValidators())
          .map(builder => builder.setSkipAllDefaultValidators())
          .map(_.build().processToClaims(jwt))
      }
    }

    /**
     * The generator to test.
     */
    val generator = new Jose4JJwtFormat(producer, consumer)

    /**
     * Some custom claims.
     */
    lazy val customClaims = JObject(Map(
      "boolean" -> JTrue,
      "string" -> JString("string"),
      "int" -> JNumber(1234567890),
      "long" -> JNumber(1234567890L),
      "float" -> JNumber(1.2),
      "double" -> JNumber(1.2d),
      "null" -> JNull,
      "array" -> JArray(JNumber(1), JNumber(2)),
      "object" -> JObject(Map(
        "array" -> JArray(JString("string1"), JString("string2")),
        "object" -> JObject(Map(
          "array" -> JArray(JString("string"), JFalse, JObject(Map("number" -> JNumber(1))))
        ))
      ))
    ))

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
     * A helper method which overrides reserved claims and checks for an exception.
     *
     * @param claim The claim to override.
     * @return A Specs2 match result.
     */
    protected def reserved(claim: String): MatchResult[Any] = {
      val message = OverrideReservedClaim.format(claim, ReservedClaims.mkString(", "))
      generator.write(JwtClaims(custom = JObject(Map(claim -> JString("test"))))) must beFailedTry.like {
        case e: JwtException => e.getMessage must be equalTo message
      }
    }
  }
}
