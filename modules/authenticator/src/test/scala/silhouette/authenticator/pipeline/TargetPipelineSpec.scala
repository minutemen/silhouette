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

import cats.effect.IO
import cats.effect.IO._
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.LoginInfo
import silhouette.authenticator.pipeline.Dsl._
import silhouette.authenticator.{ Authenticator, AuthenticatorWriter, TargetPipeline }
import silhouette.http.transport.{ CookieTransportConfig, DiscardCookie, EmbedIntoHeader }
import silhouette.http.{ Cookie, Fake }
import sttp.model.Header

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
            response.header("test") must beSome(Header.notValidated("test", authenticator.toString))
        }
    }

    "discard a cookie from the response" in new Context {
      discardPipeline(authenticator, responsePipeline).unsafeRunSync() must
        beLike[Fake.ResponsePipeline] {
          case response =>
            response.cookie("test") must beSome(Cookie(
              name = "test",
              value = "",
              maxAge = Some(-86400),
              path = Some("/"),
              secure = true,
              httpOnly = true
            ))
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
    val ioStep: Step[Authenticator, IO[Authenticator]] = {
      val m = mock[Step[Authenticator, IO[Authenticator]]]
      m.apply(authenticator) returns IO.pure(authenticator)
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
    val authenticatorWriter: AuthenticatorWriter[IO, String] = {
      val m = mock[AuthenticatorWriter[IO, String]]
      m.apply(authenticator) returns IO.pure(authenticator.toString)
      m
    }

    /**
     * The embed pipeline to test.
     */
    val embedPipeline = TargetPipeline[IO, Fake.ResponsePipeline](target =>
      ~pureStep >> ioStep >> authenticatorWriter >> EmbedIntoHeader("test")(target)
    )

    /**
     * The discard pipeline to test.
     */
    val discardPipeline = TargetPipeline[IO, Fake.ResponsePipeline](target =>
      ~ioStep >> xx(target) >> DiscardCookie[Fake.Response](CookieTransportConfig("test"))
    )
  }
}
