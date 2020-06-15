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
package silhouette.specs2

import java.security.Security

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.specs2.mutable.Specification

import scala.util.{ Failure, Try }

/**
 * Add the Bouncy Castle JCE provider to a test.
 */
trait WithBouncyCastle {
  self: Specification =>

  Try {
    val provider = new BouncyCastleProvider()
    if (Option(Security.getProvider(provider.getName)).isDefined)
      Security.removeProvider(provider.getName)

    Security.addProvider(provider)
  }.recoverWith {
    case e: Exception => Failure(new RuntimeException("Could not initialize bouncy castle encryption", e))
  }
}
