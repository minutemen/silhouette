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

/**
 * Test case for the [[Header]] class.
 */
class HeaderSpec extends Specification {

  "The `value` method" should {
    "concatenate the header values with a comma" in {
      Header(Header.Name.Status, "value1", "value2").value must be equalTo "value1,value2"
    }
  }

  "The implicit `toString` method" should {
    "transform a `Header.Name` instance to a string" in {
      Header.Name.Status must be equalTo "Status"
    }
  }

  "The implicit `fromString` method" should {
    "transform a string into a `Header.Name` instance" in {
      "Status" must be equalTo Header.Name.Status
    }
  }

  "The `BasicAuthorizationHeader` object" should {
    "create a 'Basic' `Authorization` header from a value" in {
      BasicAuthorizationHeader("some.value") must be equalTo Header(Header.Name.Authorization, "Basic some.value")
    }

    "create a 'Basic' `Authorization` header from `BasicCredentials`" in {
      val credentials = BasicCredentials("user", "pass")

      BasicAuthorizationHeader(credentials) must be equalTo
        Header(Header.Name.Authorization, s"Basic ${BasicCredentials(credentials)}")
    }

    "extract `BasicCredentials` from an 'Basic' `Authorization` header" in {
      val credentials = BasicCredentials("user", "pass")
      val header = Header(Header.Name.Authorization, s"Basic ${BasicCredentials(credentials)}")

      BasicAuthorizationHeader.unapply(header) must beSome(credentials)
    }

    "not extract `BasicCredentials` for an unexpected auth scheme" in {
      val credentials = BasicCredentials("user", "pass")
      val header = Header(Header.Name.Authorization, s"Unexpected ${BasicCredentials(credentials)}")

      BasicAuthorizationHeader.unapply(header) must beNone
    }

    "not extract `BasicCredentials` for invalid encoded credentials" in {
      val header = Header(Header.Name.Authorization, s"Basic user:pass")

      BasicAuthorizationHeader.unapply(header) must beNone
    }
  }

  "The `BearerAuthorizationHeader` object" should {
    "create a 'Bearer' `Authorization` header from a value" in {
      BearerAuthorizationHeader("some.value") must be equalTo Header(Header.Name.Authorization, "Bearer some.value")
    }

    "create a 'Bearer' `Authorization` header from a `BearerToken`" in {
      val token = BearerToken("some.token")

      BearerAuthorizationHeader(token) must be equalTo
        Header(Header.Name.Authorization, s"Bearer ${token.value}")
    }

    "extract a `BearerToken` from an 'Bearer' `Authorization` header" in {
      val token = BearerToken("some.token")
      val header = Header(Header.Name.Authorization, s"Bearer ${token.value}")

      BearerAuthorizationHeader.unapply(header) must beSome(token)
    }

    "not extract a `BearerToken` for an unexpected auth scheme" in {
      val token = BearerToken("some.token")
      val header = Header(Header.Name.Authorization, s"Unexpected ${token.value}")

      BearerAuthorizationHeader.unapply(header) must beNone
    }
  }
}
