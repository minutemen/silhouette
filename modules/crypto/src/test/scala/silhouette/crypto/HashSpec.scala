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

import org.specs2.mutable.Specification
import silhouette.crypto.Hash._

/**
 * Test case for the [[Hash]] object.
 */
class HashSpec extends Specification {

  "The `sha1` method" should {
    "create a SHA-1 hash of a string with UTF-8 decoding" in {
      sha1("SÄÜ%&/($§QW@\\'Ä_:;>|§`´*~") must be equalTo "a87babacb5ef14f1f811527c2028706a55c56be5"
    }
  }

  "The `sha256` method" should {
    "create a SHA-256 hash of a string" in {
      sha256("SÄÜ%&/($§QW@\\'Ä_:;>|§`´*~") must be equalTo
        "162b62e492f8f4d979d5f96c1fb96c7bf5c0621f48f0613bf16fa527e41c54e5"
    }
  }

  "The `sha384` method" should {
    "create a SHA-384 hash of a string" in {
      sha384("SÄÜ%&/($§QW@\\'Ä_:;>|§`´*~") must be equalTo
        "d0f9cc557e46731b3473548d4ccb228ca73d327c8b460e48bf0c465563cde632a7e07774ddce0998e488e38e4ea6aec7"
    }
  }

  "The `sha512` method" should {
    "create a SHA-512 hash of a string" in {
      sha512("SÄÜ%&/($§QW@\\'Ä_:;>|§`´*~") must be equalTo
        "dfa7f66ff599f21a91c8aedb6bfafb12b029e3f4856866d3f7a51d5701e06d101e4a5dd55d9009e96fbfad3f1ec972dd7634431" +
        "5ca311766f4d50b94a0f32edb"
    }
  }
}
