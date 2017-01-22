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
package silhouette.http.transport.format

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.exceptions.TransformException
import silhouette.http.transport.format.BearerAuthHeaderFormat._

/**
 * Test case for the [[BearerAuthHeaderFormat]] class.
 */
class BearerAuthorizationHeaderSpec extends Specification {

  "The `read` method" should {
    "return None if the header value doesn't start with 'Bearer'" in new Context {
      format.read("test") must beFailedTry.like {
        case e: TransformException => e.getMessage must be equalTo MissingBearerAuthIdentifier
      }
    }

    "return the header value" in new Context {
      format.read(s"Bearer some.long.token") must beSuccessfulTry("some.long.token")
    }
  }

  "The `write` method" should {
    "return a 'Bearer' auth header for the given credentials" in new Context {
      format.write("some.long.token") must be equalTo s"Bearer some.long.token"
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The format to test.
     */
    val format = new BearerAuthHeaderFormat()
  }
}
