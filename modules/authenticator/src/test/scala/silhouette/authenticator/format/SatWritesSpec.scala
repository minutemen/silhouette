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
package silhouette.authenticator.format

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.authenticator.{ Authenticator, StatefulWrites }
import silhouette.specs2.WaitPatience
import silhouette.LoginInfo

/**
 * Test case for the [[SatWrites]] class.
 *
 * @param ev The execution environment.
 */
class SatWritesSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The instance" should {
    "be a StatefulWrites" in new Context {
      satWrites must beAnInstanceOf[StatefulWrites]
    }
  }

  "The `write` method" should {
    "return the authenticator ID as token" in new Context {
      satWrites.write(authenticator) must beEqualTo("id").awaitWithPatience
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The authenticator representation of the claims object.
     */
    val authenticator = Authenticator("id", LoginInfo("credentials", "john@doe.com"))

    /**
     * The SAT authenticator writes.
     */
    val satWrites = SatWrites()
  }
}
