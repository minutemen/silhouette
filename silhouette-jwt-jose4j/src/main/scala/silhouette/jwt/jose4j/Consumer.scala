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

import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import silhouette.jwt._

import scala.util.Try

/**
 * Consumes JWT tokens with the help of the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The consumption of a JWT can be very complex, especially with the jose4j library because of it's feature richness.
 * A JWT can use different encryption and signing algorithms, it can be nested or it can use the two-pass consumption
 * approach. Therefore we allow a user to define it's own [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
 *
 * Please visit the [documentation](https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples) to see how the
 * [[org.jose4j.jwt.consumer.JwtConsumerBuilder]] can be configured.
 */
trait Consumer {

  /**
   * Consumes a JWT and returns [[org.jose4j.jwt.JwtClaims]].
   *
   * @param jwt The JWT token to consume.
   * @return The [[org.jose4j.jwt.JwtClaims]] extracted from the JWT token on success, otherwise an failure.
   */
  def consume(jwt: String): Try[JwtClaims]
}

/**
 * A simple JWT consumer which supports common JWS algorithms.
 *
 * @param jwsConfiguration      The JWS configuration.
 * @param requireSubject        Indicates if the 'sub' claim must be set during JWT consumption.
 * @param requireJwtID          Indicates if the 'jti' claim must be set during JWT consumption.
 * @param requireExpirationTime Indicates if the 'exp' claim must be set during JWT consumption.
 * @param requireIssuedAt       Indicates if the 'iat' claim must be set during JWT consumption.
 * @param requireNotBefore      Indicates if the 'nbf' claim must be set during JWT consumption.
 * @param expectedIssuer        Indicates the expected value of the issuer ("iss") claim and that the claim is
 *                              required.
 * @param expectedAudience      Set the audience value(s) to use when validating the audience ("aud") claim of a JWT
 *                              and require that an audience claim be present.
 */
final case class SimpleConsumer(
  jwsConfiguration: JwsConfiguration[String],
  requireSubject: Boolean = false,
  requireJwtID: Boolean = false,
  requireExpirationTime: Boolean = false,
  requireIssuedAt: Boolean = false,
  requireNotBefore: Boolean = false,
  expectedIssuer: Option[String] = None,
  expectedAudience: Option[List[String]] = None
) extends Consumer {
  type BuilderPipeline = PartialFunction[JwtConsumerBuilder, JwtConsumerBuilder]

  /**
   * Consumes a JWT and returns [[org.jose4j.jwt.JwtClaims]].
   *
   * @param jwt The JWT token to consume.
   * @return The [[org.jose4j.jwt.JwtClaims]] extracted from the JWT token on success, otherwise an failure.
   */
  override def consume(jwt: String): Try[JwtClaims] = {
    Try(new JwtConsumerBuilder())
      .map(jwsBuilder)
      .map(requireSubjectBuilder)
      .map(requireJwtIDBuilder)
      .map(requireExpirationTimeBuilder)
      .map(requireIssuedAtBuilder)
      .map(requireNotBeforeBuilder)
      .map(expectedIssuerBuilder)
      .map(expectedAudienceBuilder)
      .map(_.build().processToClaims(jwt))
  }

  /**
   * Maps the `jws` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
   *
   * @return The builder pipeline.
   */
  private def jwsBuilder: BuilderPipeline = {
    case builder =>
      jwsConfiguration match {
        case JwsHmacConfiguration(_, key) =>
          builder.setVerificationKey(key)
        case JwsRsaConfiguration(_, publicKey, _) =>
          builder.setVerificationKey(publicKey)
        case JwsEcConfiguration(_, publicKey, _) =>
          builder.setVerificationKey(publicKey)
        case JwsRsaPssConfiguration(_, publicKey, _) =>
          builder.setVerificationKey(publicKey)
      }
  }

  /**
   * Maps the `requireSubject` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
   *
   * @return The builder pipeline.
   */
  private def requireSubjectBuilder: BuilderPipeline = {
    case builder if requireSubject => builder.setRequireSubject()
    case builder                   => builder
  }

  /**
   * Maps the `requireJwtID` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
   *
   * @return The builder pipeline.
   */
  private def requireJwtIDBuilder: BuilderPipeline = {
    case builder if requireJwtID => builder.setRequireJwtId()
    case builder                 => builder
  }

  /**
   * Maps the `requireExpirationTime` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
   *
   * @return The builder pipeline.
   */
  private def requireExpirationTimeBuilder: BuilderPipeline = {
    case builder if requireExpirationTime => builder.setRequireExpirationTime()
    case builder                          => builder
  }

  /**
   * Maps the `requireIssuedAt` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
   *
   * @return The builder pipeline.
   */
  private def requireIssuedAtBuilder: BuilderPipeline = {
    case builder if requireIssuedAt => builder.setRequireIssuedAt()
    case builder                    => builder
  }

  /**
   * Maps the `requireNotBefore` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
   *
   * @return The builder pipeline.
   */
  private def requireNotBeforeBuilder: BuilderPipeline = {
    case builder if requireNotBefore => builder.setRequireNotBefore()
    case builder                     => builder
  }

  /**
   * Maps the `expectedIssuer` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
   *
   * @return The builder pipeline.
   */
  private def expectedIssuerBuilder: BuilderPipeline = {
    case builder =>
      expectedIssuer.map(issuer => builder.setExpectedIssuer(issuer)).getOrElse(builder)
  }

  /**
   * Maps the `expectedAudience` configuration to the [[org.jose4j.jwt.consumer.JwtConsumerBuilder]].
   *
   * @return The builder pipeline.
   */
  private def expectedAudienceBuilder: BuilderPipeline = {
    case builder =>
      expectedAudience.map(audience => builder.setExpectedAudience(audience: _*)).getOrElse(builder)
  }
}
