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
package silhouette.provider.social

import io.circe.Json

/**
 * Builds the social profile.
 *
 * @tparam F The type of the IO monad.
 */
trait SocialProfileBuilder[F[_]] {
  self: SocialProvider[F, _] =>

  /**
   * The content type to parse a profile from.
   *
   * The content type is mostly JSON. A concrete provider is able to override it if needed.
   */
  type Content = Json

  /**
   * The type of the profile a profile builder is responsible for.
   */
  type Profile <: SocialProfile

  /**
   * Subclasses need to implement this method to populate the profile information from the service provider.
   *
   * @param authInfo The auth info received from the provider.
   * @return Either an error on the left or the build social profile on the right.
   */
  protected def buildProfile(authInfo: A): F[Profile]

  /**
   * Returns the profile parser implementation.
   *
   * @return The profile parser implementation.
   */
  protected def profileParser: SocialProfileParser[F, Content, Profile, A]
}

/**
 * The profile builder for the common social profile.
 *
 * @tparam F The type of the IO monad.
 */
trait CommonProfileBuilder[F[_]] {
  self: SocialProfileBuilder[F] =>

  /**
   * The type of the profile a profile builder is responsible for.
   */
  type Profile = CommonSocialProfile
}
