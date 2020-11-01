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
package silhouette.authenticator.transformer

import cats.effect.IO
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.LoginInfo
import silhouette.authenticator.transformer.SatReader.MissingAuthenticator
import silhouette.authenticator.{ Authenticator, AuthenticatorException }

/**
 * Test case for the [[SatReader]] class.
 */
class SatReaderSpec extends Specification with Mockito {

  "The `read` method" should {
    "throw an `AuthenticatorException` if no authenticator couldn't be found for the given token" in new Context {
      override val satReader = SatReader[IO](_ => IO.pure(None))

      satReader("some.token").unsafeRunSync() must throwA[AuthenticatorException].like { case e =>
        e.getMessage must be equalTo MissingAuthenticator.format("some.token")
      }
    }

    "return the found authenticator" in new Context {
      satReader("some.token").unsafeRunSync() must be equalTo authenticator
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
     * The SAT authenticator reader.
     */
    val satReader = SatReader[IO](_ => IO.pure(Some(authenticator)))
  }
}
