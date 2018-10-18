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
import silhouette.http.AuthScheme._

/**
 * Test case for the [[AuthScheme]] class.
 */
class AuthSchemeSpec extends Specification {

  "The `toString` method" should {
    "return the name of the scheme" in {
      Basic.toString must be equalTo "Basic"
      Digest.toString must be equalTo "Digest"
      Bearer.toString must be equalTo "Bearer"
    }
  }

  "The `apply` method" should {
    "return the auth scheme in the form '[NAME] [VALUE]'" in {
      Basic("some.value") must be equalTo "Basic some.value"
      Digest("some.value") must be equalTo "Digest some.value"
      Bearer("some.value") must be equalTo "Bearer some.value"
    }
  }

  "The `unapply` method" should {
    "extract the value of the scheme" in {
      Basic.unapply("Basic some.value") must beSome("some.value")
      Digest.unapply("Digest some.value") must beSome("some.value")
      Bearer.unapply("Bearer some.value") must beSome("some.value")
    }

    "return None if the scheme doesn't match" in {
      Basic.unapply("Test some.value") must beNone
      Digest.unapply("Test some.value") must beNone
      Bearer.unapply("Test some.value") must beNone
    }

    "return None if there is no space between the name and the value" in {
      Basic.unapply("Basicsome.value") must beNone
      Digest.unapply("Digestsome.value") must beNone
      Bearer.unapply("Bearersome.value") must beNone
    }
  }
}
