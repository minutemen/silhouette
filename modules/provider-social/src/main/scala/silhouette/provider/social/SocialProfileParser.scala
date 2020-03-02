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

import silhouette.AuthInfo

/**
 * Parses a social profile.
 *
 * A parser transforms the content returned from the provider into a social profile instance. Parsers can
 * be reused by other parsers to avoid duplicating code.
 *
 * @tparam F The type of the IO monad.
 * @tparam C The content type to parse a profile from.
 * @tparam P The type of the profile to parse to.
 * @tparam A The type of the auth info.
 */
trait SocialProfileParser[F[_], C, P <: SocialProfile, A <: AuthInfo] {

  /**
   * Parses the social profile.
   *
   * @param content The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from given result.
   */
  def parse(content: C, authInfo: A): F[P]
}
