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

import cats.effect.SyncIO
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.LoginInfo
import silhouette.authenticator.{ Authenticator, TargetPipeline, Writes }
import silhouette.http.{ Fake, Header }

/**
 * Test case for the [[TargetPipeline]] class.
 */
class TargetPipelineSpec extends Specification with Mockito {
  // TODO: Fix tests
  args(skipAll = true)
  "The `write` method" should {
    "write the authenticator with the `statefulWriter`" in new Context {
      pipeline.write(authenticator -> responsePipeline).unsafeRunSync()

      there was one(asyncStep).apply(authenticator)
    }

    "embed the authenticator into the response" in new Context {
      pipeline.write(authenticator -> responsePipeline).unsafeRunSync() must
        beLike[Fake.ResponsePipeline] {
          case response =>
            response.header("test") must beSome(Header("test", authenticator.toString))
        }
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The login info.
     */
    val loginInfo = LoginInfo("credentials", "john@doe.com")

    /**
     * The authenticator representation of the claims object.
     */
    val authenticator = Authenticator(id = "id", loginInfo = loginInfo)

    /**
     * The response pipeline.
     */
    val responsePipeline = Fake.response

    /**
     * An `AsyncStep` implementation.
     */
    val asyncStep = {
      val m = mock[AsyncStep[SyncIO]]
      m.apply(authenticator) returns SyncIO.pure(authenticator)
      m
    }

    /**
     * An `ModifyStep` implementation.
     */
    val modifyStep = {
      val m = mock[ModifyStep]
      m.apply(authenticator) returns authenticator
      m
    }

    /**
     * A writes that transforms the [[Authenticator]] into a serialized form of the [[Authenticator]].
     */
    val authenticatorWrites = {
      val m = mock[Writes[SyncIO, String]]
      m.write(authenticator) returns SyncIO.pure(authenticator.toString)
      m
    }

    /**
     * The pipeline to test.
     */
    val pipeline = TargetPipeline[SyncIO, Fake.ResponsePipeline](_ => _ => SyncIO.pure(Fake.response))
  }
}
