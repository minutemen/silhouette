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
package silhouette.persistence.repositories

import java.time.Clock

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.persistence.CacheLayer
import silhouette.specs2.WaitPatience
import silhouette.{ Authenticator, LoginInfo }

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.classTag

/**
 * Test case for the [[CacheAuthenticatorRepository]] class.
 *
 * @param ev The execution environment.
 */
class CacheAuthenticatorRepositorySpec(implicit ev: ExecutionEnv)
  extends Specification with Mockito with WaitPatience {

  "The `find` method" should {
    "return value from cache" in new Context {
      implicit val ct = classTag[Authenticator]
      cacheLayer.find[Authenticator]("test-id") returns Future.successful(Some(authenticator))

      repository.find("test-id") must beSome(authenticator).awaitWithPatience
      there was one(cacheLayer).find[Authenticator]("test-id")
    }

    "return None if value couldn't be found in cache" in new Context {
      implicit val ct = classTag[Authenticator]
      cacheLayer.find("test-id") returns Future.successful(None)

      repository.find("test-id") must beNone.awaitWithPatience
      there was one(cacheLayer).find("test-id")
    }
  }

  "The `add` method" should {
    "add value in cache" in new Context {
      cacheLayer.save("test-id", authenticator, Duration.Inf) returns Future.successful(authenticator)

      repository.add(authenticator) must beEqualTo(authenticator).awaitWithPatience
      there was one(cacheLayer).save("test-id", authenticator, Duration.Inf)
    }
  }

  "The `update` method" should {
    "update value in cache" in new Context {
      cacheLayer.save("test-id", authenticator, Duration.Inf) returns Future.successful(authenticator)

      repository.update(authenticator) must beEqualTo(authenticator).awaitWithPatience
      there was one(cacheLayer).save("test-id", authenticator, Duration.Inf)
    }
  }

  "The `remove` method" should {
    "remove value from cache" in new Context {
      cacheLayer.remove("test-id") returns Future.successful(())

      repository.remove("test-id") must beEqualTo(()).awaitWithPatience
      there was one(cacheLayer).remove("test-id")
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A storable authenticator.
     */
    lazy val authenticator = Authenticator(
      "test-id",
      LoginInfo("", ""),
      Clock.systemUTC().instant(),
      Clock.systemUTC().instant()
    )

    /**
     * The cache layer implementation.
     */
    lazy val cacheLayer = mock[CacheLayer].smart

    /**
     * The repository to test.
     */
    lazy val repository = new CacheAuthenticatorRepository(cacheLayer)
  }
}
