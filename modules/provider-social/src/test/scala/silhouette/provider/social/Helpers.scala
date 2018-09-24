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
package silhouette.provider.social

import org.specs2.matcher.{ JsonMatchers, MatchResult }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.AuthInfo
import silhouette.http.ResponsePipeline
import silhouette.provider.social.state.StateItem
import silhouette.specs2.Wait

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Base test case for the social providers.
 */
trait SocialProviderSpec[A <: AuthInfo] extends Specification with Mockito with JsonMatchers with Wait {

  /**
   * Applies a matcher on a simple result.
   *
   * @param providerResult The result from the provider.
   * @param b              The matcher block to apply.
   * @tparam P The type of the request.
   * @return A specs2 match result.
   */
  def result[P](providerResult: Future[Either[ResponsePipeline[P], A]])(
    b: Future[ResponsePipeline[P]] => MatchResult[_]
  ): MatchResult[Either[ResponsePipeline[P], A]] = {
    await(providerResult) must beLeft[ResponsePipeline[P]].like {
      case result => b(Future.successful(result))
    }
  }

  /**
   * Applies a matcher on a auth info.
   *
   * @param providerResult The result from the provider.
   * @param b              The matcher block to apply.
   * @tparam P The type of the request.
   * @return A specs2 match result.
   */
  def authInfo[P](providerResult: Future[Either[ResponsePipeline[P], A]])(
    b: A => MatchResult[_]
  ): MatchResult[Either[ResponsePipeline[P], A]] = {
    await(providerResult) must beRight[A].like {
      case authInfo => b(authInfo)
    }
  }

  /**
   * Applies a matcher on a social profile.
   *
   * @param providerResult The result from the provider.
   * @param b              The matcher block to apply.
   * @return A specs2 match result.
   */
  def profile(providerResult: Future[SocialProfile])(b: SocialProfile => MatchResult[_]): MatchResult[SocialProfile] = {
    await(providerResult) must beLike[SocialProfile] {
      case socialProfile => b(socialProfile)
    }
  }

  /**
   * Matches a partial function against a failure message.
   *
   * This method checks if an exception was thrown in a future.
   * @see https://groups.google.com/d/msg/specs2-users/MhJxnvyS1_Q/FgAK-5IIIhUJ
   *
   * @param providerResult The result from the provider.
   * @param f A matcher function.
   * @return A specs2 match result.
   */
  def failed[E <: Throwable: ClassTag](providerResult: Future[_])(
    f: => PartialFunction[Throwable, MatchResult[_]]
  ): MatchResult[Throwable] = {
    implicit class Rethrow(t: Throwable) {
      def rethrow = { throw t; t }
    }

    lazy val result = await(providerResult.failed)

    result must not(throwAn[E])
    result.rethrow must throwAn[E].like(f)
  }
}

/**
 * Base test case for the social state providers.
 */
trait SocialStateProviderSpec[A <: AuthInfo, S <: StateItem] extends SocialProviderSpec[A] {

  /**
   * Applies a matcher on a simple result.
   *
   * @param providerResult The result from the provider.
   * @param b              The matcher block to apply.
   * @tparam P The type of the response.
   * @return A specs2 match result.
   */
  def statefulResult[P](providerResult: Future[Either[ResponsePipeline[P], StatefulAuthInfo[A, S]]])(
    b: Future[ResponsePipeline[P]] => MatchResult[_]
  ): MatchResult[Either[ResponsePipeline[P], StatefulAuthInfo[A, S]]] = {
    await(providerResult) must beLeft[ResponsePipeline[P]].like {
      case result => b(Future.successful(result))
    }
  }

  /**
   * Applies a matcher on a stateful auth info.
   *
   * @param providerResult The result from the provider.
   * @param b              The matcher block to apply.
   * @tparam P The type of the response.
   * @return A specs2 match result.
   */
  def statefulAuthInfo[P](providerResult: Future[Either[ResponsePipeline[P], StatefulAuthInfo[A, S]]])(
    b: StatefulAuthInfo[A, S] => MatchResult[_]
  ): MatchResult[Either[ResponsePipeline[P], StatefulAuthInfo[A, S]]] = {
    await(providerResult) must beRight[StatefulAuthInfo[A, S]].like {
      case info => b(info)
    }
  }
}
