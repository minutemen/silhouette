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

import org.jose4j.jws.AlgorithmIdentifiers._

/**
 * JWT implementation based on the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The library supports the JWS/JWE compact serializations with the complete suite of JOSE algorithms.
 */
package object jose4j {

  /**
   * HMAC based algorithms.
   */
  case object HS256 extends JwsHmacAlgorithm[String] { def get: String = HMAC_SHA256 }
  case object HS384 extends JwsHmacAlgorithm[String] { def get: String = HMAC_SHA384 }
  case object HS512 extends JwsHmacAlgorithm[String] { def get: String = HMAC_SHA512 }

  /**
   * Asymmetric RSA cryptography based algorithms.
   */
  case object RS256 extends JwsRsaAlgorithm[String] { def get: String = RSA_USING_SHA256 }
  case object RS384 extends JwsRsaAlgorithm[String] { def get: String = RSA_USING_SHA384 }
  case object RS512 extends JwsRsaAlgorithm[String] { def get: String = RSA_USING_SHA512 }

  /**
   * Asymmetric EC(elliptic-curve) cryptography based algorithms.
   */
  case object ES256 extends JwsEcAlgorithm[String] { def get: String = ECDSA_USING_P256_CURVE_AND_SHA256 }
  case object ES384 extends JwsEcAlgorithm[String] { def get: String = ECDSA_USING_P384_CURVE_AND_SHA384 }
  case object ES512 extends JwsEcAlgorithm[String] { def get: String = ECDSA_USING_P521_CURVE_AND_SHA512 }

  /**
   * Asymmetric RSA-PSS cryptography based algorithms.
   *
   * This algorithms requires the Bouncy Castle JCA provider (or another provider which supports RSASSA-PSS).
   */
  case object PS256 extends JwsRsaPssAlgorithm[String] { def get: String = RSA_PSS_USING_SHA256 }
  case object PS384 extends JwsRsaPssAlgorithm[String] { def get: String = RSA_PSS_USING_SHA384 }
  case object PS512 extends JwsRsaPssAlgorithm[String] { def get: String = RSA_PSS_USING_SHA512 }
}
