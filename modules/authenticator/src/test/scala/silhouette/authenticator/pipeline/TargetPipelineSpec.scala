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

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.LoginInfo
import silhouette.authenticator.{ Authenticator, TargetPipeline, Writes }
import silhouette.http.{ Fake, Header, ResponsePipeline, SilhouetteResponse }
import silhouette.specs2.WaitPatience

import scala.concurrent.Future

/**
 * Test case for the [[TargetPipeline]] class.
 *
 * @param ev The execution environment.
 */
class TargetPipelineSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `write` method" should {
    "write the authenticator with the `statefulWriter`" in new Context {
      pipeline.write(authenticator -> responsePipeline)

      there was one(asyncStep).apply(authenticator)
    }

    "embed the authenticator into the response" in new Context {
      pipeline.write(authenticator -> responsePipeline) must beLike[ResponsePipeline[SilhouetteResponse]] {
        case response =>
          response.header("test") must beSome(Header("test", authenticator.toString))
      }.awaitWithPatience
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
      val m = mock[AsyncStep]
      m.apply(authenticator) returns Future.successful(authenticator)
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
      val m = mock[Writes[Future, String]]
      m.write(authenticator) returns Future.successful(authenticator.toString)
      m
    }

    /**
     * The pipeline to test.
     */
    val pipeline = TargetPipeline[ResponsePipeline[SilhouetteResponse]](_ => _ => Future.successful(Fake.response))
  }
}
