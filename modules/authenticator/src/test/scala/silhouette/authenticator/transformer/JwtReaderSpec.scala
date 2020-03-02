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
package silhouette.authenticator.transformer

import java.time.Instant

import cats.effect.IO
import io.circe.syntax._
import io.circe.{ Json, JsonObject }
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.LoginInfo
import silhouette.authenticator.transformer.JwtReader._
import silhouette.authenticator.{ Authenticator, AuthenticatorException }
import silhouette.crypto.Base64
import silhouette.jwt.{ Claims, JwtClaimReader }

/**
 * Test case for the [[JwtReader]] class.
 */
class JwtReaderSpec extends Specification with Mockito {

  "The `read` method" should {
    "return a failed future if the underlying reads returns a `Failure`" in new Context {
      val exception = new RuntimeException("test")
      claimReader(jwt) returns Left(exception)

      jwtReader(jwt).unsafeRunSync() must throwA[RuntimeException].like {
        case e =>
          e must be equalTo exception
      }
    }

    "throw an `AuthenticatorException` if the `jwtID` isn't set in a claim" in new Context {
      claimReader(jwt) returns Right(claims.copy(jwtID = None))

      jwtReader(jwt).unsafeRunSync() must throwA[AuthenticatorException].like {
        case e =>
          e.getMessage must be equalTo MissingClaimValue.format("jwtID")
      }
    }

    "throw an `AuthenticatorException` if the `subject` isn't set in a claim" in new Context {
      claimReader(jwt) returns Right(claims.copy(subject = None))

      jwtReader(jwt).unsafeRunSync() must throwA[AuthenticatorException].like {
        case e =>
          e.getMessage must be equalTo MissingClaimValue.format("subject")
      }
    }

    "throw an `AuthenticatorException` if the `subject` claim cannot be parsed" in new Context {
      claimReader(jwt) returns Right(claims.copy(subject = Some(Base64.encode("invalid"))))

      jwtReader(jwt).unsafeRunSync() must throwA[AuthenticatorException].like {
        case e =>
          e.getMessage must be equalTo JsonParseError.format("invalid")
      }
    }

    "return the tags as empty array if the `tags` in the custom claim JSON isn't an array" in new Context {
      claimReader(jwt) returns Right(claims.copy(custom = JsonObject(
        "tags" -> Json.fromString("some string")
      )))

      jwtReader(jwt).unsafeRunSync() must beLike[Authenticator] {
        case value =>
          value.tags must be equalTo Seq()
      }
    }

    "return the `tags` as empty array if the values in the JSON `tags` array are not strings" in new Context {
      claimReader(jwt) returns Right(claims.copy(custom = JsonObject("tags" -> Json.arr(Json.fromInt(1)))))

      jwtReader(jwt).unsafeRunSync() must beLike[Authenticator] {
        case value =>
          value.tags must be equalTo Seq()
      }
    }

    "return the `fingerprint` as None if the `fingerprint` in the custom claim JSON isn't a string" in new Context {
      claimReader(jwt) returns Right(claims.copy(custom = JsonObject("fingerprint" -> Json.fromInt(1))))

      jwtReader(jwt).unsafeRunSync() must beLike[Authenticator] {
        case value =>
          value.fingerprint must beNone
      }
    }

    "create an authenticator representation from the JWT" in new Context {
      claimReader(jwt) returns Right(claims)

      jwtReader(jwt).unsafeRunSync() must be equalTo authenticator
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A test JWT.
     *
     * This isn't a valid JWT, but a valid JWT is not needed because we mock the JWT implementation.
     */
    val jwt = "a.test.jwt"

    /**
     * An instant of time.
     */
    val instant = Instant.parse("2017-10-22T20:50:45.0Z")

    /**
     * The login info.
     */
    val loginInfo = LoginInfo("credentials", "john@doe.com")

    /**
     * A JWT claims object.
     */
    val claims = Claims(
      issuer = Some("issuer"),
      subject = Some(Base64.encode(loginInfo.asJson.toString())),
      audience = Some(List("test")),
      expirationTime = Some(instant),
      notBefore = Some(instant),
      issuedAt = Some(instant),
      jwtID = Some("id"),
      custom = JsonObject(
        "tags" -> Json.arr(Json.fromString("tag1"), Json.fromString("tag2")),
        "fingerprint" -> Json.fromString("fingerprint"),
        "payload" -> Json.obj("secure" -> Json.True)
      )
    )

    /**
     * The authenticator representation of the claims object.
     */
    val authenticator = Authenticator(
      id = "id",
      loginInfo = loginInfo,
      touched = Some(instant),
      expires = Some(instant),
      fingerprint = Some("fingerprint"),
      tags = Seq("tag1", "tag2"),
      payload = Some(Json.obj("secure" -> Json.True))
    )

    /**
     * A mock of the underlying JWT claim reader.
     */
    val claimReader = mock[JwtClaimReader]

    /**
     * The JWT authenticator reader.
     */
    val jwtReader = JwtReader[IO](claimReader)
  }
}
