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

import cats.effect.Async
import javax.inject.Inject
import silhouette.password.PasswordHasherRegistry
import silhouette.provider.IdentityNotFoundException
import silhouette.provider.form.PasswordLoginProvider._
import silhouette.provider.password.{ InvalidPasswordException, PasswordProvider }
import silhouette.{ ConfigurationException, Credentials, LoginInfo }

/**
 * Credentials to authenticate with an identifier (pseudonym, email address, ...) and a password.
 *
 * @param identifier The unique identifier (pseudonym, email address, ...) to authenticate with.
 * @param password   The password to authenticate with.
 */
case class PasswordCredentials(identifier: String, password: String) extends Credentials

/**
 * A provider for authenticating with [[PasswordCredentials]].
 *
 * The provider supports the change of password hashing algorithms on the fly. Sometimes it may be possible to change
 * the hashing algorithm used by the application. But the hashes stored in the backing store can't be converted back
 * into plain text passwords, to hash them again with the new algorithm. So if a user successfully authenticates after
 * the application has changed the hashing algorithm, the provider hashes the entered password again with the new
 * algorithm and stores the auth info in the backing store.
 *
 * @param authInfoReader         A function that tries to find the [[silhouette.password.PasswordInfo]] for the given
 *                               [[LoginInfo]].
 * @param authInfoWriter         A function that writes the [[silhouette.password.PasswordInfo]] for the given
 *                               [[LoginInfo]].
 * @param passwordHasherRegistry The password hashers used by the application.
 * @tparam F The type of the IO monad.
 */
class PasswordLoginProvider[F[_]: Async] @Inject() (
  protected val authInfoReader: PasswordProvider[F]#AuthInfoReader,
  protected val authInfoWriter: PasswordProvider[F]#AuthInfoWriter,
  protected val passwordHasherRegistry: PasswordHasherRegistry
) extends PasswordProvider[F] {

  /**
   * Gets the provider ID.
   *
   * @return The provider ID.
   */
  override def id: String = ID

  /**
   * Authenticates a user with its credentials.
   *
   * @param credentials The credentials to authenticate with.
   * @return The login info if the authentication was successful, otherwise a failure.
   */
  def authenticate(credentials: PasswordCredentials): F[LoginInfo] = {
    Async[F].flatMap(loginInfo(credentials)) { loginInfo =>
      Async[F].flatMap(authenticate(loginInfo, credentials.password)) {
        case Successful               => Async[F].pure(loginInfo)
        case InvalidPassword(error)   => Async[F].raiseError(new InvalidPasswordException(error))
        case UnsupportedHasher(error) => Async[F].raiseError(new ConfigurationException(error))
        case NotFound(error)          => Async[F].raiseError(new IdentityNotFoundException(error))
      }
    }
  }

  /**
   * Gets the login info for the given credentials.
   *
   * Override this method to manipulate the creation of the login info from the credentials.
   *
   * By default the credentials provider creates the login info with the identifier entered
   * in the form. For some cases this may not be enough. It could also be possible that a login
   * form allows a user to log in with either a username or an email address. In this case
   * this method should be overridden to provide a unique binding, like the user ID, for the
   * entered form values.
   *
   * @param credentials The credentials to authenticate with.
   * @return The login info created from the credentials.
   */
  def loginInfo(credentials: PasswordCredentials): F[LoginInfo] =
    Async[F].pure(LoginInfo(id, credentials.identifier))
}

/**
 * The companion object.
 */
object PasswordLoginProvider {

  /**
   * The provider ID.
   */
  val ID = "password-login"
}
