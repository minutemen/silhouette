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
package silhouette.specs2

import java.security.Security

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.specs2.mutable.Specification

/**
 * Test case for the [[WithBouncyCastle]] trait.
 */
class WithBouncyCastleSpec extends Specification {

  "The trait" should {
    "add the `BouncyCastleProvider` if it's not registered" in {
      new Specification with WithBouncyCastle

      val provider = new BouncyCastleProvider()
      Option(Security.getProvider(provider.getName)) must beSome
    }

    "add the `BouncyCastleProvider` if it's already registered" in {
      val provider = new BouncyCastleProvider()
      Security.addProvider(provider)

      new Specification with WithBouncyCastle

      Option(Security.getProvider(provider.getName)) must beSome
    }
  }
}
