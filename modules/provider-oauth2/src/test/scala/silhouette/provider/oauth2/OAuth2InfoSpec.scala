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
package silhouette.provider.oauth2

import java.time.{ Clock, Instant }

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

/**
 * Test case for the [[OAuth2Info]] class.
 */
class OAuth2InfoSpec extends Specification with Mockito {

  "The `expired` method" should {
    "return false if no `expiresIn` value is set" in new Context {
      authInfo.expiresIn returns None
      authInfo.createdAt returns Some(Instant.ofEpochSecond(0))

      authInfo.expired(clock) must beFalse
    }

    "return false if no `createdAt` value is set" in new Context {
      authInfo.expiresIn returns Some(10)
      authInfo.createdAt returns None

      authInfo.expired(clock) must beFalse
    }

    "return false if no `expiresIn` and no `createdAt` values are set" in new Context {
      authInfo.expiresIn returns None
      authInfo.createdAt returns None

      authInfo.expired(clock) must beFalse
    }

    "return false if the token isn't expired" in new Context {
      authInfo.expiresIn returns Some(10)
      authInfo.createdAt returns Some(Instant.ofEpochSecond(0))
      clock.instant() returns Instant.ofEpochSecond(10)

      authInfo.expired(clock) must beFalse
    }

    "return true if the token is expired" in new Context {
      authInfo.expiresIn returns Some(10)
      authInfo.createdAt returns Some(Instant.ofEpochSecond(0))
      clock.instant() returns Instant.ofEpochSecond(11)

      authInfo.expired(clock) must beTrue
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A mock of the clock.
     */
    val clock = mock[Clock].smart

    /**
     * The [[OAuth2Info]] mock to test.
     */
    val authInfo = spy(OAuth2Info("some-access-token"))
  }
}
