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
package silhouette.authenticator

import java.time.{ Clock, Instant, ZoneId }

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.LoginInfo
import silhouette.RichInstant._
import silhouette.http.{ Fake, RequestPipeline, SilhouetteRequest }
import silhouette.specs2.WaitPatience

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Test case for the [[Authenticator]] class.
 *
 * @param ev The execution environment.
 */
class AuthenticatorSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `expiresIn` method" should {
    "return the duration the authenticator expires in" in new Context {
      authenticator.copy(expires = Some(instant)).expiresIn(Clock.fixed(instant.minusSeconds(10), UTC)) must
        beSome(10.seconds)
    }

    "return a negative duration if the authenticator is already expired" in new Context {
      authenticator.copy(expires = Some(instant)).expiresIn(Clock.fixed(instant.plusSeconds(10), UTC)) must
        beSome(-10.seconds)
    }
  }

  "The `touchedAt` method" should {
    "return the duration the authenticator was last touched at" in new Context {
      authenticator.copy(touched = Some(instant)).touchedAt(Clock.fixed(instant.plusSeconds(10), UTC)) must
        beSome(10.seconds)
    }

    "return a negative duration if the authenticator wasn't used" in new Context {
      authenticator.copy(touched = Some(instant)).touchedAt(Clock.fixed(instant.minusSeconds(10), UTC)) must
        beSome(-10.seconds)
    }
  }

  "The `touch` method" should {
    "touch an authenticator" in new Context {
      authenticator.touch(clock).touched must beSome(instant)
    }
  }

  "The `withExpiry` method" should {
    "set an authenticator expiry" in new Context {
      authenticator.withExpiry(10.hours, clock).expires must beSome(clock.instant() + 10.hours)
    }
  }

  "The `withFingerPrint` method" should {
    "set a default fingerprint" in new Context {
      implicit val request: RequestPipeline[SilhouetteRequest] = Fake.request

      authenticator.withFingerPrint().fingerprint must beSome(request.fingerprint)
    }

    "set a custom fingerprint" in new Context {
      val fingerPrintGenerator = (_: RequestPipeline[SilhouetteRequest]) => "test.fingerprint"
      implicit val request: RequestPipeline[SilhouetteRequest] = Fake.request

      authenticator.withFingerPrint(fingerPrintGenerator).fingerprint must
        beSome(request.fingerprint(fingerPrintGenerator))
    }
  }

  "The `withTags` method" should {
    "add new tags" in new Context {
      authenticator.withTags("test1", "test2").tags should be equalTo Seq("test1", "test2")
    }
  }

  "The `isTouched` method" should {
    "return true if the authenticator was touched" in new Context {
      authenticator.touch(clock).isTouched should beTrue
    }

    "return false if the authenticator was not touched" in new Context {
      authenticator.isTouched should beFalse
    }
  }

  "The `isTaggedWith` method" should {
    "return true if the authenticator was tagged with the given tag" in new Context {
      authenticator.withTags("test").isTaggedWith("test") should beTrue
    }

    "return true if the authenticator was tagged with all the given tags" in new Context {
      authenticator.withTags("test1", "test2", "test3").isTaggedWith("test1", "test2") should beTrue
    }

    "return false if the authenticator was not tagged with the given tag" in new Context {
      authenticator.withTags("test").isTaggedWith("test1") should beFalse
    }

    "return false if the authenticator was not tagged with any of the given tags" in new Context {
      authenticator.withTags("test1", "test2").isTaggedWith("test1", "test3") should beFalse
    }
  }

  "The `isValid` method" should {
    "return false if at least one validator fails" in new Context {
      val validator1 = mock[Validator].smart
      val validator2 = mock[Validator].smart
      validator1.isValid(authenticator) returns Future.successful(true)
      validator2.isValid(authenticator) returns Future.successful(false)
      val validators = Set(validator1, validator2)

      authenticator.isValid(validators) must beFalse.awaitWithPatience
    }

    "return true if all validators are successful" in new Context {
      val validator1 = mock[Validator].smart
      val validator2 = mock[Validator].smart
      validator1.isValid(authenticator) returns Future.successful(true)
      validator2.isValid(authenticator) returns Future.successful(true)
      val validators = Set(validator1, validator2)

      authenticator.isValid(validators) must beTrue.awaitWithPatience
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The UTC time zone.
     */
    val UTC = ZoneId.of("UTC")

    /**
     * An instant of time.
     */
    val instant = Instant.parse("2017-10-22T20:50:45.0Z")

    /**
     * A clock instance.
     */
    val clock: Clock = Clock.fixed(instant, UTC)

    /**
     * The authenticator instance to test.
     */
    val authenticator = Authenticator(
      id = "test-id",
      loginInfo = LoginInfo("credentials", "john@doe.com")
    )
  }
}
