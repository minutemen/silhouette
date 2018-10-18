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
 * Test case for the [[Status]] class.
 */
class StatusSpec extends Specification {

  "The `toString` method" should {
    "return the string representation of a status" in {
      Status.OK.toString must be equalTo "200 (OK)"
    }
  }

  "The implicit `toInt` method" should {
    "transform a `Status` instance to a status code" in {
      Status.OK must be equalTo 200
    }
  }

  "The implicit `fromInt` method" should {
    "transform a status code into a `Status` instance" in {
      200 must be equalTo Status.OK
    }

    "transform an unofficial status code into a `Status` instance" in {
      600 must be equalTo Status(600, "Unofficial code")
    }
  }
}
