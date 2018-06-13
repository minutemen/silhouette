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
package silhouette.authenticator

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.authenticator.Authorization._
import silhouette.specs2.WaitPatience
import silhouette.{ Identity, LoginInfo }

/**
 * Test case for the [[Authorization]] class.
 *
 * @param ev The execution environment.
 */
class AuthorizationSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `Authorized` authorization" should {
    "return true" in new Context {
      Authorized().isAuthorized(user, authenticator) must beEqualTo(true).awaitWithPatience
    }
  }

  "The `Unauthorized` authorization" should {
    "return true" in new Context {
      Unauthorized().isAuthorized(user, authenticator) must beEqualTo(false).awaitWithPatience
    }
  }

  "The `RichAuthorization` class" should {
    "allow to negate an authorization" in new Context {
      (!Authorized[User]()).isAuthorized(user, authenticator) must beEqualTo(false).awaitWithPatience
      (!Unauthorized[User]()).isAuthorized(user, authenticator) must beEqualTo(true).awaitWithPatience
    }

    "allow to perform a logical AND operation" in new Context {
      (Authorized[User]() && Unauthorized[User]()).isAuthorized(user, authenticator) must
        beEqualTo(false).awaitWithPatience

      (Authorized[User]() && Authorized[User]()).isAuthorized(user, authenticator) must
        beEqualTo(true).awaitWithPatience

      (Unauthorized[User]() && Unauthorized[User]()).isAuthorized(user, authenticator) must
        beEqualTo(false).awaitWithPatience

      (Unauthorized[User]() && Authorized[User]()).isAuthorized(user, authenticator) must
        beEqualTo(false).awaitWithPatience
    }

    "allow to perform a logical OR operation" in new Context {
      (Authorized[User]() || Unauthorized[User]()).isAuthorized(user, authenticator) must
        beEqualTo(true).awaitWithPatience

      (Authorized[User]() || Authorized[User]()).isAuthorized(user, authenticator) must
        beEqualTo(true).awaitWithPatience

      (Unauthorized[User]() || Unauthorized[User]()).isAuthorized(user, authenticator) must
        beEqualTo(false).awaitWithPatience

      (Unauthorized[User]() || Authorized[User]()).isAuthorized(user, authenticator) must
        beEqualTo(true).awaitWithPatience
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A test user.
     */
    case class User(loginInfo: LoginInfo) extends Identity

    /**
     * The login info.
     */
    val loginInfo = LoginInfo("credentials", "john@doe.com")

    /**
     * The identity implementation.
     */
    val user = User(loginInfo)

    /**
     * The authenticator instance to test.
     */
    val authenticator = Authenticator(
      id = "test-id",
      loginInfo = loginInfo
    )
  }
}
