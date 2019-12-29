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

import cats.effect.Sync
import silhouette.password.{ PasswordHasherRegistry, PasswordInfo }
import silhouette.provider.Provider
import silhouette.provider.password.PasswordProvider._
import silhouette.{ Done, LoginInfo }

/**
 * Base provider that provides shared functionality to handle authentication based on passwords.
 *
 * The provider supports the change of password hashing algorithms on the fly. Sometimes it may be possible to change
 * the hashing algorithm used by the application. But the hashes stored in the backing store can't be converted back
 * into plain text passwords, to hash them again with the new algorithm. So if a user successfully authenticates after
 * the application has changed the hashing algorithm, the provider hashes the entered password again with the new
 * algorithm and stores the auth info in the backing store.
 *
 * @tparam F The type of the IO monad.
 */
abstract class PasswordProvider[F[_]: Sync] extends Provider {
  type AuthInfoReader = LoginInfo => F[Option[PasswordInfo]]
  type AuthInfoWriter = (LoginInfo, PasswordInfo) => F[Done]

  /**
   * The authentication state.
   */
  sealed trait State

  /**
   * Indicates that the authentication was successful.
   */
  case object Successful extends State

  /**
   * Indicates that the entered password doesn't match with the stored one.
   */
  case class InvalidPassword(error: String) extends State

  /**
   * Indicates that the stored password cannot be checked with the registered hashers.
   */
  case class UnsupportedHasher(error: String) extends State

  /**
   * Indicates that no password info was stored for the login info.
   */
  case class NotFound(error: String) extends State

  /**
   * A function that tries to find the [[silhouette.password.PasswordInfo]] for the given [[LoginInfo]].
   */
  protected val authInfoReader: AuthInfoReader

  /**
   * A function that writes the [[silhouette.password.PasswordInfo]] for the given [[LoginInfo]].
   */
  protected val authInfoWriter: AuthInfoWriter

  /**
   * The password hashers used by the application.
   */
  protected val passwordHasherRegistry: PasswordHasherRegistry

  /**
   * Handles authentication based on the given [[LoginInfo]] and the given password.
   *
   * @param loginInfo The login info to search the password info for.
   * @param password  The password to authenticate with.
   * @return The authentication state.
   */
  def authenticate(loginInfo: LoginInfo, password: String): F[State] = {
    Sync[F].flatMap(authInfoReader(loginInfo)) {
      case Some(passwordInfo) => passwordHasherRegistry.find(passwordInfo) match {
        case Some(hasher) if hasher.matches(passwordInfo, password) =>
          if ((passwordHasherRegistry isDeprecated hasher) || (hasher isDeprecated passwordInfo).contains(true)) {
            Sync[F].map(authInfoWriter(loginInfo, passwordHasherRegistry.current.hash(password))) { _ =>
              Successful
            }
          } else {
            Sync[F].pure(Successful)
          }
        case Some(_) => Sync[F].pure(InvalidPassword(PasswordDoesNotMatch.format(id)))
        case None => Sync[F].pure(UnsupportedHasher(HasherIsNotRegistered.format(
          id, passwordInfo.hasher, passwordHasherRegistry.all.map(_.id).mkString(", ")
        )))
      }
      case None => Sync[F].pure(NotFound(PasswordInfoNotFound.format(id, loginInfo)))
    }
  }
}

/**
 * The companion object.
 */
object PasswordProvider {

  /**
   * The error messages.
   */
  val PasswordDoesNotMatch = "[%s] Password does not match"
  val HasherIsNotRegistered = "[%s] Stored hasher ID `%s` isn't registered as supported hasher: %s"
  val PasswordInfoNotFound = "[%s] Could not find password info for given login info: %s"
}
