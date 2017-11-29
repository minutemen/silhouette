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

import java.time.{ Clock, Instant, ZoneId }

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.specs2.WaitPatience
import silhouette.{ Authenticator, LoginInfo }

import scala.concurrent.duration._

/**
 * Test case for the [[SlidingWindowValidator]] class.
 *
 * @param ev The execution environment.
 */
class SlidingWindowValidatorSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `isValid` method" should {
    "return always true if the `touched` property isn't set" in new Context {
      SlidingWindowValidator(1.minute, clock)
        .isValid(authenticator) must beTrue.awaitWithPatience
    }

    "return true if the authenticator is not timed out" in new Context {
      SlidingWindowValidator(1.minute, Clock.fixed(instant, UTC))
        .isValid(authenticator.touch(clock)) must beTrue.awaitWithPatience
    }

    "return true if the authenticator was idle for exactly one minute" in new Context {
      SlidingWindowValidator(1.minute, Clock.fixed(instant.plusSeconds(60), UTC))
        .isValid(authenticator.touch(clock)) must beTrue.awaitWithPatience
    }

    "return false if the authenticator was idle for exactly one minute and 1 second " in new Context {
      SlidingWindowValidator(1.minute, Clock.fixed(instant.plusSeconds(61), UTC))
        .isValid(authenticator.touch(clock)) must beFalse.awaitWithPatience
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
