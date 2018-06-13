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
import silhouette.authenticator.{ Authenticator, StatelessWrites }
import silhouette.http.transport.EmbedIntoHeader
import silhouette.http.{ FakeResponse, FakeResponsePipeline, ResponsePipeline }
import silhouette.specs2.WaitPatience
import silhouette.LoginInfo

import scala.concurrent.Future

/**
 * Test case for the [[EmbedStatelessPipeline]] class.
 *
 * @param ev The execution environment.
 */
class EmbedStatelessPipelineSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `write` method" should {
    "embed the authenticator into the response" in new Context {
      pipeline.write(authenticator -> responsePipeline) must beLike[ResponsePipeline[FakeResponse]] {
        case response =>
          response.header("test") must be equalTo Seq(authenticator.toString)
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
    val responsePipeline = FakeResponsePipeline()

    /**
     * A writes that transforms the [[Authenticator]] into a serialized form of the [[Authenticator]].
     */
    val statelessWrites = {
      val m = mock[StatelessWrites]
      m.write(authenticator) returns Future.successful(authenticator.toString)
      m
    }

    /**
     * The pipeline to test.
     */
    val pipeline = EmbedStatelessPipeline[FakeResponse](statelessWrites, EmbedIntoHeader("test"))
  }
}
