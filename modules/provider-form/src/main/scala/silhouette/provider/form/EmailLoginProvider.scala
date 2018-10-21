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
package silhouette.provider.form

import javax.inject.Inject
import silhouette.LoginInfo
import silhouette.provider.Provider
import silhouette.provider.form.EmailLoginProvider._

import scala.concurrent.Future

/**
 * A provider for authenticating with an email address.
 */
class EmailLoginProvider @Inject() () extends Provider {

  /**
   * Gets the provider ID.
   *
   * @return The provider ID.
   */
  override def id: String = ID

  /**
   * Authenticates a user with an email address.
   *
   * @param email The email to authenticate with.
   * @return The login info if the authentication was successful, otherwise a failure.
   */
  def authenticate(email: String): Future[LoginInfo] = ???
}

/**
 * The companion object.
 */
object EmailLoginProvider {

  /**
   * The provider ID.
   */
  val ID = "email-login"
}
