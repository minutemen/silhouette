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
import silhouette.Fitting._
import silhouette.http.transport.HeaderTransport
import silhouette.http.transport.format.BasicAuthHeaderFormat
import silhouette.http.{ Header, RequestPipeline }
import silhouette.password.PasswordHasherRegistry
import silhouette.provider.RequestProvider
import silhouette.provider.http.BasicAuthProvider._
import silhouette.provider.password.PasswordProvider
import silhouette.{ ConfigurationException, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

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
 * @param passwordHasherRegistry The password hashers used by the application.
 * @param ec                     The execution context to handle the asynchronous operations.
 */
class BasicAuthProvider @Inject() (
  protected val authInfoReader: PasswordProvider#AuthInfoReader,
  protected val authInfoWriter: PasswordProvider#AuthInfoWriter,
  protected val passwordHasherRegistry: PasswordHasherRegistry
)(
  implicit
  val ec: ExecutionContext
) extends RequestProvider with PasswordProvider with LazyLogging {

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
   * @tparam R The type of the request.
   * @return Some login info on successful authentication or None if the authentication was unsuccessful.
   */
  override def authenticate[R](request: RequestPipeline[R]): Future[Option[LoginInfo]] = {
    HeaderTransport(Header.Name.Authorization).retrieve(request).andThenTry(BasicAuthHeaderFormat()).toTry match {
      case Success(credentials) =>
        val loginInfo = LoginInfo(id, credentials.identifier)
        authenticate(loginInfo, credentials.password).map {
          case Authenticated => Some(loginInfo)
          case InvalidPassword(error) =>
            logger.debug(error)
            None
          case UnsupportedHasher(error) => throw new ConfigurationException(error)
          case NotFound(error) =>
            logger.debug(error)
            None
        }
      case Failure(e) =>
        logger.debug(e.getMessage, e)
        Future.successful(None)
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
