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
package silhouette

import java.time.{ Clock, Instant, ZoneId }

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.authenticator.Validator
import silhouette.specs2.WaitPatience

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Test case for the [[Authenticator]] class.
 *
 * @param ev The execution environment.
 */
class AuthenticatorSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `expiresIn` method" should {
    "return the duration the authenticator expires in" in new Context {
      authenticator.copy(expires = Some(instant)).expiresIn(Clock.fixed(instant.minusSeconds(10), UTC)) must
        beSome(10.seconds)
    }

    "return a negative duration if the authenticator is already expired" in new Context {
      authenticator.copy(expires = Some(instant)).expiresIn(Clock.fixed(instant.plusSeconds(10), UTC)) must
        beSome(-10.seconds)
    }
  }

  "The `lastTouchedAt` method" should {
    "return the duration the authenticator was last touched at" in new Context {
      authenticator.copy(lastTouched = Some(instant)).lastTouchedAt(Clock.fixed(instant.plusSeconds(10), UTC)) must
        beSome(10.seconds)
    }

    "return a negative duration if the authenticator wasn't used" in new Context {
      authenticator.copy(lastTouched = Some(instant)).lastTouchedAt(Clock.fixed(instant.minusSeconds(10), UTC)) must
        beSome(-10.seconds)
    }
  }

  "The `isValid` method" should {
    "return false if at least one validator fails" in new Context {
      val validator1 = mock[Validator].smart
      val validator2 = mock[Validator].smart
      validator1.isValid(authenticator) returns Future.successful(true)
      validator2.isValid(authenticator) returns Future.successful(false)
      val validators = Set(validator1, validator2)

      authenticator.isValid(validators) must beFalse.awaitWithPatience
    }

    "return true if all validators are successful" in new Context {
      val validator1 = mock[Validator].smart
      val validator2 = mock[Validator].smart
      validator1.isValid(authenticator) returns Future.successful(true)
      validator2.isValid(authenticator) returns Future.successful(true)
      val validators = Set(validator1, validator2)

      authenticator.isValid(validators) must beTrue.awaitWithPatience
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The UTC time zone.
     */
    val UTC = ZoneId.of("UTC")

    /**
     * An instant of time.
     */
    val instant = Instant.parse("2017-10-22T20:50:45.0Z")

    /**
     * A clock instance.
     */
    val clock: Clock = Clock.fixed(instant, UTC)

    /**
     * The authenticator instance to test.
     */
    val authenticator = Authenticator(
      id = "test-id",
      loginInfo = LoginInfo("test", "test")
    )
  }
}
