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
package silhouette.http

import org.specs2.mutable.Specification
import sttp.model.Header

/**
 * Test case for the [[silhouette.http.AuthorizationHeader]] class.
 */
class AuthorizationHeaderSpec extends Specification {

  "The `BasicAuthorizationHeader` object" should {
    "create a 'Basic' `Authorization` header from a value" in {
      BasicAuthorizationHeader("some.value") must be equalTo Header.authorization("Basic", "some.value")
    }

    "create a 'Basic' `Authorization` header from `BasicCredentials`" in {
      val credentials = BasicCredentials("user", "pass")

      BasicAuthorizationHeader(credentials) must be equalTo
        Header.authorization("Basic", BasicCredentials(credentials))
    }

    "extract `BasicCredentials` from an 'Basic' `Authorization` header" in {
      val credentials = BasicCredentials("user", "pass")
      val header = Header.authorization("Basic", BasicCredentials(credentials))

      BasicAuthorizationHeader.unapply(header) must beSome(credentials)
    }

    "not extract `BasicCredentials` for an unexpected auth scheme" in {
      val credentials = BasicCredentials("user", "pass")
      val header = Header.authorization("Unexpected", BasicCredentials(credentials))

      BasicAuthorizationHeader.unapply(header) must beNone
    }

    "not extract `BasicCredentials` for invalid encoded credentials" in {
      val header = Header.authorization("Basic", "user:pass")

      BasicAuthorizationHeader.unapply(header) must beNone
    }
  }

  "The `BearerAuthorizationHeader` object" should {
    "create a 'Bearer' `Authorization` header from a value" in {
      BearerAuthorizationHeader("some.value") must be equalTo Header.authorization("Bearer", "some.value")
    }

    "create a 'Bearer' `Authorization` header from a `BearerToken`" in {
      val token = BearerToken("some.token")

      BearerAuthorizationHeader(token) must be equalTo
        Header.authorization("Bearer", token.value)
    }

    "extract a `BearerToken` from an 'Bearer' `Authorization` header" in {
      val token = BearerToken("some.token")
      val header = Header.authorization("Bearer", token.value)

      BearerAuthorizationHeader.unapply(header) must beSome(token)
    }

    "not extract a `BearerToken` for an unexpected auth scheme" in {
      val token = BearerToken("some.token")
      val header = Header.authorization("Unexpected", token.value)

      BearerAuthorizationHeader.unapply(header) must beNone
    }
  }
}
