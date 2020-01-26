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
package silhouette.authorization

import cats.effect.SyncIO._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.authorization.Authorization._
import silhouette.{ Identity, LoginInfo }

/**
 * Test case for the [[Authorization]] class.
 */
class AuthorizationSpec extends Specification with Mockito {

  "The `Authorized` authorization" should {
    "return true" in new Context {
      Authorized().isAuthorized(user, TestContext()).unsafeRunSync() must beEqualTo(true)
    }
  }

  "The `Unauthorized` authorization" should {
    "return true" in new Context {
      Unauthorized().isAuthorized(user, TestContext()).unsafeRunSync() must beEqualTo(false)
    }
  }

  "The `RichAuthorization` class" should {
    "allow to negate an authorization" in new Context {
      !Authorized().isAuthorized(user, TestContext()).unsafeRunSync() must beEqualTo(false)
      !Unauthorized().isAuthorized(user, TestContext()).unsafeRunSync() must beEqualTo(true)
    }

    "allow to perform a logical AND operation" in new Context {
      (Authorized() && Unauthorized()).isAuthorized(user, TestContext()).unsafeRunSync() must
        beEqualTo(false)

      (Authorized() && Authorized()).isAuthorized(user, TestContext()).unsafeRunSync() must
        beEqualTo(true)

      (Unauthorized() && Unauthorized()).isAuthorized(user, TestContext()).unsafeRunSync() must
        beEqualTo(false)

      (Unauthorized() && Authorized()).isAuthorized(user, TestContext()).unsafeRunSync() must
        beEqualTo(false)
    }

    "allow to perform a logical OR operation" in new Context {
      (Authorized() || Unauthorized()).isAuthorized(user, TestContext()).unsafeRunSync() must
        beEqualTo(true)

      (Authorized() || Authorized()).isAuthorized(user, TestContext()).unsafeRunSync() must
        beEqualTo(true)

      (Unauthorized() || Unauthorized()).isAuthorized(user, TestContext()).unsafeRunSync() must
        beEqualTo(false)

      (Unauthorized() || Authorized()).isAuthorized(user, TestContext()).unsafeRunSync() must
        beEqualTo(true)
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
     * A test context.
     */
    case class TestContext()

    /**
     * The login info.
     */
    val loginInfo = LoginInfo("credentials", "john@doe.com")

    /**
     * The identity implementation.
     */
    val user = User(loginInfo)
  }
}
