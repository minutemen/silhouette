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
import silhouette.{ Credentials, TransformException }
import silhouette.crypto.Base64
import silhouette.http.transport.format.BasicAuthHeaderFormat._

/**
 * Test case for the [[BasicAuthHeaderFormat]] class.
 */
class BasicAuthorizationHeaderSpec extends Specification {

  "The `read` method" should {
    "return None if the header value doesn't start with 'Basic'" in new Context {
      format.read("test") must beFailedTry.like {
        case e: TransformException => e.getMessage must be equalTo MissingBasicAuthIdentifier
      }
    }

    "return None if the authorization part value doesn't consists of two parts" in new Context {
      format.read(s"Basic ${Base64.encode("test")}") must beFailedTry.like {
        case e: TransformException => e.getMessage must be equalTo InvalidBasicAuthHeader
      }
    }

    "return the Credentials if the header value does consists of an identifier and password part" in new Context {
      format.read(s"Basic ${Base64.encode("user:pass")}") must beSuccessfulTry(Credentials("user", "pass"))
    }

    "return the Credentials if the password part contains a colon" in new Context {
      format.read(s"Basic ${Base64.encode("user:abc:def")}") must beSuccessfulTry(Credentials("user", "abc:def"))
    }
  }

  "The `write` method" should {
    "return a 'Basic' auth header for the given credentials" in new Context {
      format.write(Credentials("user", "pass")) must be equalTo s"Basic ${Base64.encode(s"user:pass")}"
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The format to test.
     */
    val format = new BasicAuthHeaderFormat()
  }
}
