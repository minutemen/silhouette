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
package silhouette.provider.password

import cats.effect.SyncIO
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.password.{ PasswordHasher, PasswordHasherRegistry, PasswordInfo }

/**
 * Abstract test case for the [[silhouette.provider.password.PasswordProvider]] trait.
 */
// TODO: Add tests
trait PasswordProviderSpec extends Specification with Mockito {

  /**
   * The context.
   */
  trait BaseContext extends Scope {
    type Provider = PasswordProvider[SyncIO]

    /**
     * The default password hasher.
     */
    lazy val fooHasher = hasher("foo")

    /**
     * A deprecated password hasher.
     */
    lazy val barHasher = hasher("bar")

    /**
     * A function that tries to find the [[PasswordInfo]] for the given [[silhouette.LoginInfo]].
     */
    lazy val authInfoReader: Provider#AuthInfoReader = mock[Provider#AuthInfoReader].smart

    /**
     * A function that writes the [[PasswordInfo]] for the given [[silhouette.LoginInfo]].
     */
    lazy val authInfoWriter: Provider#AuthInfoWriter = mock[Provider#AuthInfoWriter].smart

    /**
     * The password hasher registry.
     */
    lazy val passwordHasherRegistry = PasswordHasherRegistry(fooHasher, List(barHasher))

    /**
     * Helper method to create a hasher mock.
     *
     * @param id The ID of the hasher.
     * @return A hasher mock.
     */
    private def hasher(id: String) = {
      val h = mock[PasswordHasher]
      h.id returns id
      h.isSuitable(any[PasswordInfo]()) answers { p: Any =>
        p.asInstanceOf[PasswordInfo].hasher == h.id
      }
      h.isDeprecated(any[PasswordInfo]()) returns Some(false)
      h
    }
  }
}
