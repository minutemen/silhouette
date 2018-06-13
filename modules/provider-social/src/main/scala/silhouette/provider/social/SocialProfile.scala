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

import silhouette.LoginInfo

/**
 * The social profile contains all the data returned from the social providers after authentication.
 */
trait SocialProfile {

  /**
   * Gets the linked login info.
   *
   * @return The linked login info.
   */
  def loginInfo: LoginInfo
}

/**
 * A social profile implementation with the most common data.
 *
 * Not every provider returns all the data defined in this class. This is also the representation of the
 * most common profile information provided by the social providers. The data can be used to create a new
 * identity for the first authentication(which is also the registration) or to update an existing identity
 * on every subsequent authentication.
 *
 * @param loginInfo The linked login info.
 * @param firstName Maybe the first name of the authenticated user.
 * @param lastName Maybe the last name of the authenticated user.
 * @param fullName Maybe the full name of the authenticated user.
 * @param email Maybe the email of the authenticated provider.
 * @param avatarURL Maybe the avatar URL of the authenticated provider.
 */
case class CommonSocialProfile(
  loginInfo: LoginInfo,
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  fullName: Option[String] = None,
  email: Option[String] = None,
  avatarURL: Option[String] = None
) extends SocialProfile
