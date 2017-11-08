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

import java.security.interfaces.{ ECPrivateKey, ECPublicKey, RSAPrivateKey, RSAPublicKey }
import javax.crypto.SecretKey

/**
 * Base trait for a JWA(JSON Web Algorithm).
 *
 * A JWA algorithm can be used with the JSON Web Signature ([JWS](https://tools.ietf.org/html/rfc7515)),
 * JSON Web Encryption ([JWE](https://tools.ietf.org/html/rfc7516)), and JSON Web Key
 * ([JWK](https://tools.ietf.org/html/rfc7517)) specifications.
 *
 * The JWS-NONE algorithm isn't supported by Silhouette because it doesn't provide any security.
 *
 * @see https://tools.ietf.org/html/rfc7518
 * @tparam T The type of the algorithm representation.
 */
sealed trait JwaAlgorithm[T] {

  /**
   * Gets the algorithm in the specified representation.
   *
   * @return The algorithm in the specified representation.
   */
  def get: T
}

/**
 * A JWS algorithm which uses asymmetric cryptography like RSA or EC.
 *
 * @tparam T The type of the algorithm representation.
 */
sealed trait JwsAsymmetricAlgorithm[T] extends JwaAlgorithm[T]

/**
 * A JWS algorithm which is based on a HMAC(Keyed-Hash Message Authentication Code).
 *
 * @see https://tools.ietf.org/html/rfc7518#section-3.2
 * @tparam T The type of the algorithm representation.
 */
trait JwsHmacAlgorithm[T] extends JwaAlgorithm[T]

/**
 * A JWS algorithm which is based on asymmetric RSA cryptography.
 *
 * @see https://tools.ietf.org/html/rfc7518#section-3.3
 * @tparam T The type of the algorithm representation.
 */
trait JwsRsaAlgorithm[T] extends JwsAsymmetricAlgorithm[T]

/**
 * A JWS algorithm which is based on asymmetric EC(elliptic-curve) cryptography.
 *
 * @see https://tools.ietf.org/html/rfc7518#section-3.4
 * @tparam T The type of the algorithm representation.
 */
trait JwsEcAlgorithm[T] extends JwsAsymmetricAlgorithm[T]

/**
 * A JWS algorithm which is based on asymmetric RSA cryptography and the Probabilistic Signature Scheme.
 *
 * @see https://tools.ietf.org/html/rfc7518#section-3.5
 * @tparam T The type of the algorithm representation.
 */
trait JwsRsaPssAlgorithm[T] extends JwsAsymmetricAlgorithm[T]

/**
 * Base trait for a JWS configuration.
 *
 * @tparam T The type of the algorithm representation.
 */
sealed trait JwsConfiguration[T] {

  /**
   * Gets the JWA algorithm used with the JWS configuration.
   *
   * @return The JWA algorithm used with the JWS configuration.
   */
  def algorithm: JwaAlgorithm[T]
}

/**
 * The JWS configuration for HMAC based algorithms.
 *
 * @param algorithm Any of the available HMAC based algorithms.
 * @param key       The secret key.
 * @tparam T The type of the algorithm representation.
 */
case class JwsHmacConfiguration[T](algorithm: JwsHmacAlgorithm[T], key: SecretKey)
  extends JwsConfiguration[T]

/**
 * The JWS configuration for asymmetric RSA cryptography based algorithms.
 *
 * @param algorithm  Any of the available RSA based algorithms.
 * @param publicKey  The public key.
 * @param privateKey The private key.
 * @tparam T The type of the algorithm representation.
 */
case class JwsRsaConfiguration[T](algorithm: JwsRsaAlgorithm[T], publicKey: RSAPublicKey, privateKey: RSAPrivateKey)
  extends JwsConfiguration[T]

/**
 * The JWS configuration for asymmetric EC(elliptic-curve) cryptography based algorithms.
 *
 * @param algorithm  Any of the available EC based algorithms.
 * @param publicKey  The public key.
 * @param privateKey The private key.
 * @tparam T The type of the algorithm representation.
 */
case class JwsEcConfiguration[T](algorithm: JwsEcAlgorithm[T], publicKey: ECPublicKey, privateKey: ECPrivateKey)
  extends JwsConfiguration[T]

/**
 * The JWS configuration for asymmetric RSA-PSS cryptography based algorithms.
 *
 * @param algorithm  Any of the available RSA-PSS based algorithms.
 * @param publicKey  The public key.
 * @param privateKey The private key.
 * @tparam T The type of the algorithm representation.
 */
case class JwsRsaPssConfiguration[T](
  algorithm: JwsRsaPssAlgorithm[T],
  publicKey: RSAPublicKey,
  privateKey: RSAPrivateKey
) extends JwsConfiguration[T]
