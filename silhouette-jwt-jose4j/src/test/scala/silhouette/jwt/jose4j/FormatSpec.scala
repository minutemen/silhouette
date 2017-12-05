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

import java.time.Clock
import java.time.temporal.ChronoUnit

import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jws.AlgorithmIdentifiers._
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.exceptions.JwtException
import silhouette.jwt.jose4j.Reads._
import silhouette.jwt.jose4j.Writes._
import silhouette.jwt.{ Claims, ReservedClaims }
import silhouette.specs2.WithBouncyCastle

import scala.json.ast._
import scala.util.Try

/**
 * Test case for the [[Reads]] and [[Writes]] classes.
 */
class FormatSpec extends Specification with WithBouncyCastle {

  "The `transformer`" should {
    "transform a JWT with an `iss` claim" in new Context {
      transform(Claims(issuer = Some("test")))
    }

    "transform a JWT with a `sub` claim" in new Context {
      transform(Claims(subject = Some("test")))
    }

    "transform a JWT with an `aud` claim" in new Context {
      transform(Claims(audience = Some(List("test1", "test2"))))
    }

    "transform a JWT with an `exp` claim" in new Context {
      transform(Claims(expirationTime = Some(Clock.systemUTC().instant())))
    }

    "transform a JWT with a `nbf` claim" in new Context {
      transform(Claims(notBefore = Some(Clock.systemUTC().instant())))
    }

    "transform a JWT with an `iat` claim" in new Context {
      transform(Claims(issuedAt = Some(Clock.systemUTC().instant())))
    }

    "transform a JWT with a `jti` claim" in new Context {
      transform(Claims(jwtID = Some("test")))
    }

    "transform a JWT with custom claims" in new Context {
      transform(Claims(custom = customClaims))
    }

    "transform a complex JWT" in new Context {
      transform(Claims(
        issuer = Some("test"),
        subject = Some("test"),
        audience = Some(List("test1", "test2")),
        expirationTime = Some(Clock.systemUTC().instant()),
        notBefore = Some(Clock.systemUTC().instant()),
        issuedAt = Some(Clock.systemUTC().instant()),
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
      reads.read("invalid.token") must beFailedTry.like {
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
    val producer = new Producer {
      override def produce(claims: JwtClaims): String = {
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
    val consumer = new Consumer {
      override def consume(jwt: String): Try[JwtClaims] = {
        Try(new JwtConsumerBuilder())
          .map(builder => builder.setJwsAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS))
          .map(builder => builder.setDisableRequireSignature())
          .map(builder => builder.setSkipAllValidators())
          .map(builder => builder.setSkipAllDefaultValidators())
          .map(_.build().processToClaims(jwt))
      }
    }

    /**
     * The reads to test.
     */
    val reads = new Reads(consumer)

    /**
     * The writes to test.
     */
    val writes = new Writes(producer)

    /**
     * Some custom claims.
     */
    val customClaims = JObject(Map(
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
    protected def transform(claims: Claims): MatchResult[Any] = {
      writes.write(claims) must beSuccessfulTry.like {
        case jwt =>
          reads.read(jwt) must beSuccessfulTry.withValue(claims.copy(
            expirationTime = claims.expirationTime.map(_.truncatedTo(ChronoUnit.SECONDS)),
            notBefore = claims.notBefore.map(_.truncatedTo(ChronoUnit.SECONDS)),
            issuedAt = claims.issuedAt.map(_.truncatedTo(ChronoUnit.SECONDS))
          ))
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
      writes.write(Claims(custom = JObject(Map(claim -> JString("test"))))) must beFailedTry.like {
        case e: JwtException => e.getMessage must be equalTo message
      }
    }
  }
}
