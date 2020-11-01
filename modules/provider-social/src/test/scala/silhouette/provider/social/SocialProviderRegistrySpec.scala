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

import cats.effect.IO
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

/**
 * Test case for the [[SocialProviderRegistry]] class.
 */
class SocialProviderRegistrySpec extends Specification with Mockito {

  "The `get` method" should {
    "return a provider by its type" in new Context {
      registry.get[GoogleProvider] must beSome(google)
    }

    "return None if no provider for the given type exists" in new Context {
      registry.get[YahooProvider] must beNone
    }

    "return a provider by its ID as SocialProvider" in new Context {
      val provider = registry.get[SocialProvider[IO, _]]("google")

      provider must beSome[SocialProvider[IO, _]].like[MatchResult[SocialProvider[IO, _]]] { case value =>
        value.id must be equalTo google.id
        value must beAnInstanceOf[SocialProvider[IO, _]]
      }
    }

    "return a provider by its ID as OAuth2Provider" in new Context {
      val provider = registry.get[OAuth2Provider]("google")

      provider must beSome[OAuth2Provider].like { case value =>
        value.id must be equalTo google.id
        value must beAnInstanceOf[OAuth2Provider]
      }
    }

    "return None if no provider for the given ID exists" in new Context {
      registry.get[SocialProvider[IO, _]]("yahoo") must beNone
    }
  }

  "The `getSeq` method" should {
    "return a list of providers by it's sub type" in new Context {
      val list = registry.getSeq[OAuth2Provider]
      list.head must be equalTo facebook
      list(1) must be equalTo google
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {
    case class OAuth1Config()
    case class OAuth2Config()

    trait OAuth1Provider extends SocialProvider[IO, OAuth1Config]
    trait OAuth2Provider extends SocialProvider[IO, OAuth2Config]
    trait TwitterProvider extends OAuth1Provider
    trait YahooProvider extends OAuth1Provider
    trait FacebookProvider extends OAuth2Provider
    trait GoogleProvider extends OAuth2Provider

    /**
     * Some social providers.
     */
    val facebook = mock[FacebookProvider]
    facebook.id returns "facebook"
    val google = mock[GoogleProvider]
    google.id returns "google"
    val twitter = mock[TwitterProvider]
    twitter.id returns "twitter"

    /**
     * The registry to test.
     */
    val registry = SocialProviderRegistry(
      Seq(
        facebook,
        google,
        twitter
      )
    )
  }
}
