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
package silhouette.persistence.services

import silhouette.{ Identity, LoginInfo }

import scala.concurrent.Future

/**
 * A trait that provides the means to retrieve identities for the Silhouette module.
 */
trait IdentityService[T <: Identity] {

  /**
   * Retrieves an identity that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve an identity.
   * @return The retrieved identity or None if no identity could be retrieved for the given login info.
   */
  def retrieve(loginInfo: LoginInfo): Future[Option[T]]
}
