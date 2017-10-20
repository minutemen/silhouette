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
package silhouette.authenticator.format

import java.time.Instant
import javax.inject.Inject

import silhouette.authenticator.Format
import silhouette.authenticator.format.JwtAuthenticatorFormat._
import io.circe.jawn.decode
import io.circe.syntax._
import silhouette.crypto.Base64
import silhouette.jwt
import silhouette.{ Authenticator, AuthenticatorException, LoginInfo }

import scala.concurrent.Future
import scala.json.ast.{ JObject, JString }
import scala.util.{ Failure, Success, Try }

/**
 * A format which transforms an authenticator into a JWT and vice versa.
 *
 * @param jwtFormat The JWT transformer.
 * @param settings  The JWT transformer settings.
 */
final case class JwtFormat @Inject() (
  jwtFormat: jwt.JwtFormat,
  settings: JwtAuthenticatorFormatSettings
) extends Format {

  /**
   * Transforms a JWT into an [[Authenticator]].
   *
   * @param jwt The JWT to transform.
   * @return An authenticator on success, an error on failure.
   */
  override def read(jwt: String): Future[Authenticator] = Future.fromTry {
    jwtFormat.read(jwt).map { claims =>
      Authenticator(
        id = claims.jwtID.getOrElse(throw new AuthenticatorException(MissingClaimValue.format("jwtID"))),
        loginInfo = buildLoginInfo(Base64.decode(claims.subject
          .getOrElse(throw new AuthenticatorException(MissingClaimValue.format("subject"))))).get,
        lastUsedDateTime = claims.issuedAt
          .getOrElse(throw new AuthenticatorException(MissingClaimValue.format("issuedAt"))),
        expirationDateTime = claims.expirationTime
          .getOrElse(throw new AuthenticatorException(MissingClaimValue.format("expirationTime"))),
        fingerprint = claims.custom.value.get("fingerprint").map {
          case s: JString => s.value
          case v          => throw new AuthenticatorException(UnexpectedJsonValue.format(v, "JString"))
        },
        payload = claims.custom.value.get("payload").map {
          case o: JObject => o
          case v          => throw new AuthenticatorException(UnexpectedJsonValue.format(v, "JObject"))
        }
      )
    }
  }

  /**
   * Transforms an [[Authenticator]] into a JWT.
   *
   * @param authenticator The authenticator to transform.
   * @return A JWT on success, an error on failure.
   */
  override def write(authenticator: Authenticator): Future[String] = Future.fromTry {
    jwtFormat.write(jwt.JwtClaims(
      issuer = settings.issuer,
      subject = Some(Base64.encode(authenticator.loginInfo.asJson.toString())),
      audience = settings.audience,
      expirationTime = Some(authenticator.expirationDateTime),
      notBefore = settings.notBefore,
      issuedAt = Some(authenticator.lastUsedDateTime),
      jwtID = Some(authenticator.id),
      custom = JObject(Seq(
        authenticator.fingerprint.map("fingerprint" -> JString(_)),
        authenticator.payload.map("payload" -> _)
      ).flatten.toMap)
    ))
  }

  /**
   * Builds the login info from Json.
   *
   * @param str The string representation of the login info.
   * @return The login info on success, otherwise a failure.
   */
  private def buildLoginInfo(str: String): Try[LoginInfo] = {
    decode[LoginInfo](str) match {
      case Left(error) =>
        Failure(new AuthenticatorException(JsonParseError.format(str), Some(error.getCause)))
      case Right(loginInfo) =>
        Success(loginInfo)
    }
  }
}

/**
 * The companion object.
 */
object JwtAuthenticatorFormat {
  val JsonParseError = "[Silhouette][JwtAuthenticatorFormat] Cannot parse Json: %s"
  val UnexpectedJsonValue = "[Silhouette][JwtAuthenticatorFormat] Unexpected Json value: %s; expected %s"
  val MissingClaimValue = "[Silhouette][JwtAuthenticatorFormat] Cannot get value for claim `%s` from JWT"
}

/**
 * The settings for the Jwt authenticator format.
 *
 * @param issuer    The JWT 'iss' claim.
 * @param audience  The JWT 'aud' claim.
 * @param notBefore The JWT 'nbf' claim.
 */
case class JwtAuthenticatorFormatSettings(
  issuer: Option[String] = None,
  audience: Option[List[String]] = None,
  notBefore: Option[Instant] = None
)
