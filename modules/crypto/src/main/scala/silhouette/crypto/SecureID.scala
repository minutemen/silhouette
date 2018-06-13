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
package silhouette.crypto

import scala.concurrent.Future

/**
 * Represents a secure (in a cryptographically sense) ID.
 */
trait SecureID[T] {

  /**
   * Gets a secure ID.
   *
   * @return The secure ID.
   */
  def get: T
}

/**
 * Represents a secure (in a cryptographically sense) ID that will be created in an async way.
 *
 * @tparam T The type of the ID.
 */
trait SecureAsyncID[T] extends SecureID[Future[T]] {

  /**
   * Gets a secure ID in an async way.
   *
   * @return The secure ID.
   */
  override def get: Future[T]
}
