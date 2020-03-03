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
package silhouette.provider.social.state.handler

import java.time.Clock

import cats.effect.IO
import org.specs2.matcher.JsonMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.crypto.SecureID
import silhouette.http._
import silhouette.http.transport.CookieTransportConfig
import silhouette.jwt.{ JwtClaimReader, JwtClaimWriter }

/**
 * Test case for the [[CsrfStateItemHandler]] class.
 */
class CsrfStateItemHandlerSpec extends Specification with Mockito with JsonMatchers {

  "The `serialize` method" should {
    "return a serialized value of the state item" in new Context {

    }
  }

  "The `unserialize` method" should {
    "unserialize the state item" in new Context {

    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A CSRF token.
     */
    val csrfToken = "csrfToken"

    /**
     * The secure ID implementation.
     */
    val secureID = {
      val m = mock[SecureID[IO, String]].smart
      m.get returns IO.pure(csrfToken)
      m
    }

    /**
     * The cookie transport config.
     */
    val cookieTransportConfig = CookieTransportConfig("test")

    /**
     * The JWT claim reader.
     */
    val claimReader = mock[JwtClaimReader]

    /**
     * The JWT claim writer.
     */
    val claimWriter = mock[JwtClaimWriter]

    /**
     * The clock instance.
     */
    val clock = mock[Clock]

    /**
     * A CSRF state item.
     */
    val csrfStateItem = CsrfStateItem(csrfToken)

    /**
     * An instance of the CSRF state item handler.
     */
    val csrfStateItemHandler = new CsrfStateItemHandler(
      secureID,
      cookieTransportConfig,
      claimReader,
      claimWriter,
      clock
    )

    /**
     * A helper method to create a cookie.
     *
     * @param value The cookie value.
     * @return A cookie instance with the given value.
     */
    def cookie(value: String): Cookie = Cookie(
      name = cookieTransportConfig.name,
      value = value,
      maxAge = cookieTransportConfig.maxAge.map(_.toSeconds.toInt),
      path = Some(cookieTransportConfig.path),
      domain = cookieTransportConfig.domain,
      secure = cookieTransportConfig.secure,
      httpOnly = cookieTransportConfig.httpOnly,
      sameSite = cookieTransportConfig.sameSite
    )
  }
}
