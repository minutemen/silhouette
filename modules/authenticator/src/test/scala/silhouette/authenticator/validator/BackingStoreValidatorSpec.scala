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
package silhouette.authenticator.validator

import cats.effect.SyncIO
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.LoginInfo
import silhouette.authenticator.Authenticator
import silhouette.authenticator.Validator.{ Invalid, Valid }
import silhouette.authenticator.validator.BackingStoreValidator._

/**
 * Test case for the [[BackingStoreValidator]] class.
 */
class BackingStoreValidatorSpec extends Specification with Mockito {

  "The `isValid` method" should {
    "return Valid if the authenticator is valid" in new Context {
      BackingStoreValidator[SyncIO](_ => SyncIO.pure(true))
        .isValid(authenticator)
        .unsafeRunSync() must beEqualTo(Valid)
    }

    "return Invalid if the authenticator is invalid" in new Context {
      BackingStoreValidator[SyncIO](_ => SyncIO.pure(false))
        .isValid(authenticator)
        .unsafeRunSync() must beEqualTo(Invalid(Seq(Error)))
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The authenticator instance to test.
     */
    val authenticator = Authenticator(
      id = "test-id",
      loginInfo = LoginInfo("test", "test")
    )
  }
}
