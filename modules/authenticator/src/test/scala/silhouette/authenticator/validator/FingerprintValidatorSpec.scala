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

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.specs2.WaitPatience
import silhouette.LoginInfo
import silhouette.authenticator.Authenticator

/**
 * Test case for the [[FingerprintValidator]] class.
 *
 * @param ev The execution environment.
 */
class FingerprintValidatorSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `isValid` method" should {
    "return always true if no fingerprint is stored in the authenticator" in new Context {
      FingerprintValidator("test")
        .isValid(authenticator) must beTrue.awaitWithPatience
    }

    "return true if fingerprint stored in the authenticator matches the given fingerprint" in new Context {
      FingerprintValidator("test")
        .isValid(authenticator.copy(fingerprint = Some("test"))) must beTrue.awaitWithPatience
    }

    "return false if fingerprint stored in the authenticator doesn't match the given fingerprint" in new Context {
      FingerprintValidator("invalid")
        .isValid(authenticator.copy(fingerprint = Some("test"))) must beFalse.awaitWithPatience
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
