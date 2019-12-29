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

import cats.effect.Sync
import io.circe.Json
import io.circe.jawn.decode
import silhouette.authenticator.format.JwtReads._
import silhouette.authenticator.{ Authenticator, AuthenticatorException, Reads }
import silhouette.crypto.Base64
import silhouette.{ LoginInfo, jwt }

import scala.util.{ Failure, Success, Try }

/**
 * A reads which transforms a JWT into an [[Authenticator]].
 *
 * Because of the fact that a JWT itself stores a complete serialized form of the authenticator, it's normally not
 * needed to use a backing store, because on subsequent requests the authenticator can be fully unserialized from the
 * JWT. But this has the disadvantage that a JWT cannot be easily invalidated. But with a backing store that creates a
 * mapping between the JWT and a stored instance, it's possible to invalidate the authenticators server side. Therefore
 * this reads can be used in a stateless and a stateful manner.
 *
 * @param jwtReads The underlying JWT reads implementation.
 * @tparam F The type of the IO monad.
 */
final case class JwtReads[F[_]: Sync](jwtReads: jwt.Reads) extends Reads[F, String] {

  /**
   * Transforms a JWT into an [[Authenticator]].
   *
   * @param jwt The JWT to transform.
   * @return An authenticator on success, an error on failure.
   */
  override def read(jwt: String): F[Authenticator] = Sync[F].fromTry {
    jwtReads.read(jwt).flatMap { claims =>
      val custom = Json.fromJsonObject(claims.custom)
      val maybeID = claims.jwtID.map(Success.apply)
        .getOrElse(Failure(new AuthenticatorException(MissingClaimValue.format("jwtID"))))
      val maybeSubject = claims.subject.map(Success.apply)
        .getOrElse(Failure(new AuthenticatorException(MissingClaimValue.format("subject"))))

      for {
        id <- maybeID
        subject <- maybeSubject
        loginInfo <- buildLoginInfo(Base64.decode(subject))
      } yield {
        Authenticator(
          id = id,
          loginInfo = loginInfo,
          touched = claims.issuedAt,
          expires = claims.expirationTime,
          tags = custom.hcursor.downField("tags").as[Seq[String]].getOrElse(Seq()),
          fingerprint = custom.hcursor.downField("fingerprint").as[String].toOption,
          payload = custom.hcursor.downField("payload").focus
        )
      }
    }
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
object JwtReads {
  val JsonParseError = "Cannot parse Json: %s"
  val UnexpectedJsonValue = "Unexpected Json value: %s; expected %s"
  val MissingClaimValue = "Cannot get value for claim `%s` from JWT"
}
