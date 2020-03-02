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
package silhouette.crypto

import java.security.SecureRandom

import cats.effect.Async
import org.apache.commons.codec.binary.Hex

/**
 * A generator which uses [[java.security.SecureRandom]] to generate cryptographically strong IDs.
 *
 * @param idSizeInBytes The size of the ID length in bytes.
 */
class SecureRandomID[F[_]: Async](idSizeInBytes: Int = 128) extends SecureID[F, String] {

  /**
   * Gets a new secure ID using [[java.security.SecureRandom]].
   *
   * Based on the chosen algorithm, the initial seeding may use /dev/random and it may block as entropy is
   * being gathered. Therefore we return a [[cats.effect.Async]] to handle the seeding in an async way.
   *
   * @return The generated ID.
   */
  override def get: F[String] = {
    import cats.syntax.functor._
    val randomValue = new Array[Byte](idSizeInBytes)
    Async[F].delay(SecureRandomID.random.nextBytes(randomValue)).map { _ =>
      Hex.encodeHexString(randomValue)
    }
  }
}

/**
 * The companion object.
 */
object SecureRandomID {

  /**
   * A cryptographically strong random number generator (RNG).
   *
   * There is a cost of getting a secure random instance for its initial seeding, so it's recommended you use
   * a singleton style so you only create one for all of your usage going forward.
   *
   * Based on the configuration, SecureRandom may use /dev/random on Unix-like systems and it may block as entropy is
   * being gathered. So it's recommended to use a singleton style so you only create one for all of your usage going
   * forward.
   *
   * @see https://tersesystems.com/blog/2015/12/17/the-right-way-to-use-securerandom/
   */
  lazy val random = new SecureRandom()
}
