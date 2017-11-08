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

import io.circe.syntax._
import silhouette.authenticator.Writes
import silhouette.crypto.Base64
import silhouette.{ Authenticator, jwt }

import scala.concurrent.Future
import scala.json.ast.{ JArray, JObject, JString }

/**
 * A format which transforms an [[Authenticator]] into a JWT.
 *
 * @param writes    The JWT writes.
 * @param issuer    The JWT 'iss' claim.
 * @param audience  The JWT 'aud' claim.
 * @param notBefore The JWT 'nbf' claim.
 */
final case class JwtWrites(
  writes: jwt.Writes,
  issuer: Option[String] = None,
  audience: Option[List[String]] = None,
  notBefore: Option[Instant] = None
) extends Writes {

  /**
   * Transforms an [[Authenticator]] into a JWT.
   *
   * @param authenticator The authenticator to transform.
   * @return A JWT on success, an error on failure.
   */
  override def write(authenticator: Authenticator): Future[String] = Future.fromTry {
    writes.write(jwt.Claims(
      issuer = issuer,
      subject = Some(Base64.encode(authenticator.loginInfo.asJson.toString())),
      audience = audience,
      expirationTime = Some(authenticator.expires),
      notBefore = notBefore,
      issuedAt = Some(authenticator.lastUsed),
      jwtID = Some(authenticator.id),
      custom = JObject(Seq(
        Some("tags" -> JArray(authenticator.tags.map(JString).toVector)),
        authenticator.fingerprint.map("fingerprint" -> JString(_)),
        authenticator.payload.map("payload" -> _)
      ).flatten.toMap)
    ))
  }
}
