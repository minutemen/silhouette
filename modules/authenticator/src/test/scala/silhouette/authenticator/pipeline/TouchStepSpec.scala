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
package silhouette.authenticator.pipeline

import java.time.{ Clock, Instant, ZoneId }

import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.LoginInfo
import silhouette.authenticator.Authenticator

/**
 * Test case for the [[TouchStep]] class.
 */
class TouchStepSpec extends Specification with Mockito {

  "The `apply` method" should {
    "touch the authenticator if touching is enabled" in new Context {
      step.apply(authenticator.touch(enableTouchClock)) must beLike[Authenticator] {
        case a =>
          a.touched must beSome(pipelineTouchClock.instant())
      }
    }

    "not touch the authenticator if touching is disabled" in new Context {
      step.apply(authenticator) must beLike[Authenticator] {
        case a =>
          a.touched must beNone
      }
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The clock used to enable the touching.
     */
    val enableTouchClock: Clock = Clock.fixed(Instant.parse("2017-10-22T20:50:00.0Z"), ZoneId.of("UTC"))

    /**
     * The clock used to touch the authenticator with the pipeline.
     */
    val pipelineTouchClock: Clock = Clock.fixed(Instant.parse("2017-10-22T20:55:00.0Z"), ZoneId.of("UTC"))

    /**
     * The login info.
     */
    val loginInfo = LoginInfo("credentials", "john@doe.com")

    /**
     * The authenticator representation of the claims object.
     */
    val authenticator = Authenticator(id = "id", loginInfo = loginInfo)

    /**
     * The step to test.
     */
    val step = TouchStep(pipelineTouchClock)
  }
}
