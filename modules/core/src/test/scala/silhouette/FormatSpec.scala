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
package silhouette

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

/**
 * Test case for the [[Reads]] and [[Writes]] traits.
 */
class FormatSpec extends Specification {

  "A `Reads` instance" should {
    "be composable" in new Context {
      testFormat.asReads andThen testFormat.asReads read "test" must be equalTo "test"
    }
  }

  "A `Writes` instance" should {
    "be composable" in new Context {
      testFormat.asWrites andThen testFormat.asWrites write "test" must be equalTo "test"
    }
  }

  "The `mapReads` method" should {
    "transform the result of a reads into another reads" in new Context {
      testFormat.mapReads(_ + "1") read "test" must be equalTo "test1"
    }
  }

  "The `mapWrites` method" should {
    "transform the result of a writes into another writes" in new Context {
      testFormat.mapWrites(_ + "1") write "test" must be equalTo "test1"
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {
    class TestFormat extends Reads[String, String] with Writes[String, String] {
      override def read(in: String): String = in
      override def write(in: String): String = in
    }

    val testFormat = new TestFormat
  }
}
