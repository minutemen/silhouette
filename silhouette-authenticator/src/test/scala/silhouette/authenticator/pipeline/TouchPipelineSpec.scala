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

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.specs2.WaitPatience
import silhouette.{ Authenticator, LoginInfo }

/**
 * Test case for the [[TouchPipeline]] class.
 *
 * @param ev The execution environment.
 */
class TouchPipelineSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `write` method" should {
    "touch the authenticator is touching is enabled" in new Context {
      pipeline.write(authenticator.touch(enableTouchClock)) must beLike[Authenticator] {
        case a =>
          a.touched must beSome(pipelineTouchClock.instant())
      }.awaitWithPatience
    }

    "not touch the authenticator is touching is disabled" in new Context {
      pipeline.write(authenticator) must beLike[Authenticator] {
        case a =>
          a.touched must beNone
      }.awaitWithPatience
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
     * The pipeline to test.
     */
    val pipeline = TouchPipeline(pipelineTouchClock)
  }
}
