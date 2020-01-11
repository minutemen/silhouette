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
import cats.effect.SyncIO._
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.LoginInfo
import silhouette.authenticator.pipeline.Dsl._
import silhouette.authenticator.{ Authenticator, AuthenticatorWriter, TargetPipeline }
import silhouette.http.transport.{ CookieTransportConfig, DiscardCookie, EmbedIntoHeader }
import silhouette.http.{ Fake, Header }

/**
 * Test case for the [[TargetPipeline]] class.
 */
class TargetPipelineSpec extends Specification with Mockito {

  "The pipeline" should {
    "write the authenticator with the `statefulWriter`" in new Context {
      embedPipeline(authenticator, responsePipeline).unsafeRunSync()

      there was one(ioStep).apply(authenticator)
    }

    "embed the authenticator into the response" in new Context {
      embedPipeline(authenticator, responsePipeline).unsafeRunSync() must
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
     * An `IO` step implementation.
     */
    val ioStep: Step[Authenticator, SyncIO[Authenticator]] = {
      val m = mock[Step[Authenticator, SyncIO[Authenticator]]]
      m.apply(authenticator) returns SyncIO.pure(authenticator)
      m
    }

    /**
     * An pure step implementation.
     */
    val pureStep: Step[Authenticator, Authenticator] = {
      val m = mock[Step[Authenticator, Authenticator]]
      m.apply(authenticator) returns authenticator
      m
    }

    /**
     * A writes that transforms the [[Authenticator]] into a serialized form of the [[Authenticator]].
     */
    val authenticatorWriter: AuthenticatorWriter[SyncIO, String] = {
      val m = mock[AuthenticatorWriter[SyncIO, String]]
      m.apply(authenticator) returns SyncIO.pure(authenticator.toString)
      m
    }

    /**
     * The embed pipeline to test.
     */
    val embedPipeline = TargetPipeline[SyncIO, Fake.ResponsePipeline](target =>
      ~pureStep >> pureStep >> ioStep >> pureStep >> authenticatorWriter >> EmbedIntoHeader("test")(target)
    )

    /**
     * The discard pipeline to test.
     */
    val discardPipeline = TargetPipeline[SyncIO, Fake.ResponsePipeline](target =>
      xx >> DiscardCookie(CookieTransportConfig("test"))(target)
    )

    /**
     * The discard pipeline to test.
     */
    val discardPipeline1 = TargetPipeline[SyncIO, Fake.ResponsePipeline](target =>
      ~ioStep xx DiscardCookie(CookieTransportConfig("test"))(target)
    )
  }
}
