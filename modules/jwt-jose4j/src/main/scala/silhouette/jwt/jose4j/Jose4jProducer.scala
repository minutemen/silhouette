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

import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import silhouette.jwt._

import scala.util.Try

/**
 * Produces JWT tokens with the help of the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The production of a JWT can be very complex, especially with the jose4j library because of it's feature richness.
 * A JWT can use different encryption and signing algorithms, it can be nested or it can use the two-pass consumption
 * approach. Therefore we allow a user to define it's own [[org.jose4j.jwx.JsonWebStructure]] implementation for the
 * given claims.
 *
 * Please visit the [documentation](https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples) to see how a
 * [[org.jose4j.jwe.JsonWebEncryption]] or a [[org.jose4j.jws.JsonWebSignature]] can be configured.
 */
trait Jose4jProducer {

  /**
   * Produces claims and returns a JWT as string.
   *
   * @param claims The claims to produce.
   * @return The produced JWT string on the right or an error on the left.
   */
  def produce(claims: JwtClaims): Either[Throwable, String]
}

/**
 * A simple JWT producer which supports common JWS algorithms.
 *
 * @param jwsConfiguration The JWS configuration.
 */
final case class SimpleJose4jProducer(jwsConfiguration: JwsConfiguration[String]) extends Jose4jProducer {

  /**
   * Produces claims and returns a JWT as string.
   *
   * @param claims The claims to produce.
   * @return The produced JWT string on the right or an error on the left.
   */
  override def produce(claims: JwtClaims): Either[Throwable, String] = Try {
    val jws = new JsonWebSignature()
    jws.setPayload(claims.toJson)
    jwsConfiguration match {
      case JwsHmacConfiguration(algorithm, key) =>
        jws.setAlgorithmHeaderValue(algorithm.get)
        jws.setKey(key)
      case JwsRsaConfiguration(algorithm, _, privateKey) =>
        jws.setAlgorithmHeaderValue(algorithm.get)
        jws.setKey(privateKey)
      case JwsEcConfiguration(algorithm, _, privateKey) =>
        jws.setAlgorithmHeaderValue(algorithm.get)
        jws.setKey(privateKey)
      case JwsRsaPssConfiguration(algorithm, _, privateKey) =>
        jws.setAlgorithmHeaderValue(algorithm.get)
        jws.setKey(privateKey)
    }
    jws.getCompactSerialization
  }.toEither
}
