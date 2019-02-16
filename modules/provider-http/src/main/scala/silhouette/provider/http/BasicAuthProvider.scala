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
package silhouette.provider.http

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import silhouette._
import silhouette.http.transport.RetrieveBasicCredentialsFromHeader
import silhouette.http.{ BasicCredentials, RequestPipeline }
import silhouette.password.PasswordHasherRegistry
import silhouette.provider.RequestProvider
import silhouette.provider.http.BasicAuthProvider._
import silhouette.provider.password.PasswordProvider

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A request provider implementation which supports HTTP basic authentication.
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
 * @param identityReader         The reader to retrieve the [[Identity]] for the [[LoginInfo]].
 * @param passwordHasherRegistry The password hashers used by the application.
 * @param ec                     The execution context to handle the asynchronous operations.
 * @tparam R The type of the request.
 * @tparam I The type of the identity.
 */
class BasicAuthProvider[R, I <: Identity] @Inject() (
  protected val authInfoReader: PasswordProvider#AuthInfoReader,
  protected val authInfoWriter: PasswordProvider#AuthInfoWriter,
  protected val identityReader: LoginInfo => Future[Option[I]],
  protected val passwordHasherRegistry: PasswordHasherRegistry
)(
  implicit
  val ec: ExecutionContext
) extends RequestProvider[R, I, BasicCredentials]
  with PasswordProvider
  with ExecutionContextProvider
  with LazyLogging {

  /**
   * Gets the provider ID.
   *
   * @return The provider ID.
   */
  override def id: String = ID

  /**
   * Authenticates an identity based on credentials sent in a request.
   *
   * @param request The request pipeline.
   * @return Some login info on successful authentication or None if the authentication was unsuccessful.
   */
  override def authenticate(request: RequestPipeline[R]): Future[AuthState[I, BasicCredentials]] = {
    RetrieveBasicCredentialsFromHeader().read(request) match {
      case Some(credentials) =>
        val loginInfo = LoginInfo(id, credentials.username)
        authenticate(loginInfo, credentials.password).flatMap {
          case Successful =>
            identityReader(loginInfo).map {
              case Some(identity) => Authenticated(identity, credentials, loginInfo)
              case None           => MissingIdentity(credentials, loginInfo)
            }
          case InvalidPassword(error) =>
            logger.debug(error)
            Future.successful(InvalidCredentials(credentials))
          case UnsupportedHasher(error) =>
            Future.failed(new ConfigurationException(error))
          case NotFound(error) =>
            logger.debug(error)
            Future.successful(InvalidCredentials(credentials))
        }
      case None =>
        Future.successful(MissingCredentials())
    }
  }
}

/**
 * The companion object.
 */
object BasicAuthProvider {

  /**
   * The provider constants.
   */
  val ID = "basic-auth"
}
