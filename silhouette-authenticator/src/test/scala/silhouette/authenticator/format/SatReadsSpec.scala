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
package silhouette.authenticator.format

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.authenticator.StatefulReads
import silhouette.authenticator.exceptions.AuthenticatorException
import silhouette.authenticator.format.SatReads.MissingAuthenticator
import silhouette.specs2.WaitPatience
import silhouette.{ Authenticator, LoginInfo }

import scala.concurrent.Future

/**
 * Test case for the [[SatReads]] class.
 *
 * @param ev The execution environment.
 */
class SatReadsSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The instance" should {
    "be a StatefulReads" in new Context {
      satReads must beAnInstanceOf[StatefulReads]
    }
  }

  "The `read` method" should {
    "throw an `AuthenticatorException` if no authenticator couldn't be found for the given token" in new Context {
      override val satReads = SatReads(_ => Future.successful(None))

      satReads.read("some.token") must throwA[AuthenticatorException].like {
        case e =>
          e.getMessage must be equalTo MissingAuthenticator.format("some.token")
      }.awaitWithPatience
    }

    "return the found authenticator" in new Context {
      satReads.read("some.token") must beEqualTo(authenticator).awaitWithPatience
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The authenticator representation of the claims object.
     */
    val authenticator = Authenticator("id", LoginInfo("credentials", "john@doe.com"))

    /**
     * The SAT authenticator reads.
     */
    val satReads = SatReads(_ => Future.successful(Some(authenticator)))
  }
}
