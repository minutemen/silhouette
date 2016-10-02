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

import scala.json.ast.JObject
import scala.util.Try

/**
 * JWT reserved claims and also optional custom claims in the form of a JSON string.
 *
 * See the [JWT RFC](https://tools.ietf.org/html/rfc7519#section-4) for
 * the full description of the claims.
 *
 * @param issuer         The JWT 'iss' claim.
 * @param subject        The JWT 'sub' claim.
 * @param audience       The JWT 'aud' claim.
 * @param expirationTime The JWT 'exp' claim in seconds.
 * @param notBefore      The JWT 'nbf' claim in seconds.
 * @param issuedAt       The JWT 'iat' claim in seconds.
 * @param jwtID          The JWT 'jti' claim.
 * @param custom         Some custom claims as JSON.
 */
case class JwtClaims(
  issuer: Option[String] = None,
  subject: Option[String] = None,
  audience: Option[List[String]] = None,
  expirationTime: Option[Long] = None,
  notBefore: Option[Long] = None,
  issuedAt: Option[Long] = None,
  jwtID: Option[String] = None,
  custom: JObject = JObject())

/**
 * Specifies encoding of JWTs.
 */
trait JwtEncoder {

  /**
   * Encodes a JWT claims object and returns a JWT as string.
   *
   * @param jwt The JWT claims object to encode.
   * @return The JWT string representation or an error if the JWT claims object couldn't be encoded.
   */
  def encode(jwt: JwtClaims): Try[String]
}

/**
 * Specifies decoding of JWTs.
 */
trait JwtDecoder {

  /**
   * Decodes a JWT string and returns a JWT claims object.
   *
   * @param str A JWT string.
   * @return The decoded JWT claims object or an error if the string couldn't be decoded.
   */
  def decode(str: String): Try[JwtClaims]
}

/**
 * JWT encoder/decoder.
 */
trait JwtGenerator extends JwtEncoder with JwtDecoder
