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
import silhouette.crypto.Base64
import silhouette.http.AuthScheme.{ Basic, Bearer }

/**
 * Test case for the [[HttpCredentials]] trait.
 */
class HttpCredentialsSpec extends Specification {

  "The `BasicCredentials`" should {
    "have the `AuthScheme` set to `Basic`" in {
      BasicCredentials("user", "name").authScheme must be equalTo Basic
    }

    "transform `BasicCredentials` as base64 encoded auth scheme" in {
      BasicCredentials.apply(BasicCredentials("user", "pass")) must be equalTo Base64.encode("user:pass")
    }

    "transform a base64 encoded auth scheme into `BasicCredentials`" in {
      BasicCredentials.unapply(Base64.encode("user:pass")) must beSome(BasicCredentials("user", "pass"))
    }

    "not transform a non base64 encoded string into `BasicCredentials`" in {
      BasicCredentials.unapply("user:pass") must beNone
    }

    "not transform an invalid base64 encoded string into `BasicCredentials`" in {
      BasicCredentials.unapply(Base64.encode("user")) must beNone
    }
  }

  "The `BearerToken`" should {
    "have the `AuthScheme` set to `Bearer`" in {
      BearerToken("some.token").authScheme must be equalTo Bearer
    }
  }
}
