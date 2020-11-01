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
package silhouette.http.auth

import org.specs2.mutable.Specification
import silhouette.TransformException
import silhouette.http.BearerToken
import silhouette.http.auth.BearerAuthSchemeReader._

/**
 * Test case for the [[BearerAuthSchemeReader]] and [[BearerAuthSchemeWriter]] objects.
 */
class BearerAuthSchemeSpec extends Specification {

  "The `read` method" should {
    "return None if the header value doesn't start with 'Bearer'" in {
      BearerAuthSchemeReader("test") must beFailedTry.like { case e: TransformException =>
        e.getMessage must be equalTo MissingBearerAuthIdentifier
      }
    }

    "return the header value" in {
      BearerAuthSchemeReader(s"Bearer some.long.token") must beSuccessfulTry(BearerToken("some.long.token"))
    }
  }

  "The `write` method" should {
    "return a 'Bearer' auth header for the given credentials" in {
      BearerAuthSchemeWriter(BearerToken("some.long.token")) must be equalTo s"Bearer some.long.token"
    }
  }
}
