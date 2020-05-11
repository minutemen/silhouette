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
package silhouette.util

import sttp.model.Uri

/**
 * Service to retrieve avatar URIs from an avatar service such as Gravatar.
 */
trait AvatarService[F[_]] {

  /**
   * Retrieves the URI for an identifier.
   *
   * @param id The identifier for the avatar.
   * @return Maybe an avatar URI or None if no URI could be found for the given identifier.
   */
  def retrieveUri(id: String): F[Option[Uri]]
}
