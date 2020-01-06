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

import cats.effect.Sync
import io.circe.syntax._
import io.circe.{ Json, JsonObject }
import silhouette.authenticator.{ Authenticator, AuthenticatorWriter }
import silhouette.crypto.Base64
import silhouette.jwt
import silhouette.jwt.ClaimWriter

/**
 * A transformation function that transforms an [[Authenticator]] into a JWT.
 *
 * Because of the fact that a JWT itself stores a complete serialized form of the authenticator, it's normally not
 * needed to use a backing store, because on subsequent requests the authenticator can be fully unserialized from the
 * JWT. But this has the disadvantage that a JWT cannot be easily invalidated. But with a backing store that creates a
 * mapping between the JWT and a stored instance, it's possible to invalidate the authenticators server side. Therefore
 * this write can be used in a stateless and a stateful manner.
 *
 * @param claimWriter The JWT claim writer function.
 * @param issuer      The JWT 'iss' claim.
 * @param audience    The JWT 'aud' claim.
 * @param notBefore   The JWT 'nbf' claim.
 * @tparam F The type of the IO monad.
 */
final case class JwtWriter[F[_]: Sync](
  claimWriter: ClaimWriter,
  issuer: Option[String] = None,
  audience: Option[List[String]] = None,
  notBefore: Option[Instant] = None
) extends AuthenticatorWriter[F, String] {

  /**
   * Transforms an [[Authenticator]] into a JWT.
   *
   * @param authenticator The authenticator to transform.
   * @return A JWT on success, an error on failure.
   */
  override def apply(authenticator: Authenticator): F[String] = {
    Sync[F].fromTry {
      claimWriter(jwt.Claims(
        issuer = issuer,
        subject = Some(Base64.encode(authenticator.loginInfo.asJson.toString())),
        audience = audience,
        expirationTime = authenticator.expires,
        notBefore = notBefore,
        issuedAt = authenticator.touched,
        jwtID = Some(authenticator.id),
        custom = JsonObject.fromIterable(Seq(
          Some("tags" -> Json.arr(authenticator.tags.map(Json.fromString): _*)),
          authenticator.fingerprint.map("fingerprint" -> Json.fromString(_)),
          authenticator.payload.map("payload" -> _)
        ).flatten)
      ))
    }
  }
}
