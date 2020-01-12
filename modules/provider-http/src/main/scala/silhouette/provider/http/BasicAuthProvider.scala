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

import cats.data.{ NonEmptyList => NEL }
import cats.effect.Sync
import javax.inject.Inject
import silhouette._
import silhouette.http.transport.RetrieveBasicCredentialsFromHeader
import silhouette.http.{ BasicCredentials, RequestPipeline, ResponsePipeline }
import silhouette.password.PasswordHasherRegistry
import silhouette.provider.RequestProvider
import silhouette.provider.http.BasicAuthProvider._
import silhouette.provider.password.PasswordProvider

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
 * @tparam F The type of the IO monad.
 * @tparam R The type of the request.
 * @tparam P The type of the response.
 * @tparam I The type of the identity.
 */
class BasicAuthProvider[F[_]: Sync, R, P, I <: Identity] @Inject() (
  protected val authInfoReader: PasswordProvider[F]#AuthInfoReader,
  protected val authInfoWriter: PasswordProvider[F]#AuthInfoWriter,
  protected val identityReader: LoginInfo => F[Option[I]],
  protected val passwordHasherRegistry: PasswordHasherRegistry
) extends PasswordProvider[F] with RequestProvider[F, R, P, I] {

  /**
   * The type of the credentials.
   */
  override type C = BasicCredentials

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
   * @param handler A function that returns a [[http.ResponsePipeline]] for the given [[AuthState]].
   * @return The [[http.ResponsePipeline]].
   */
  override def authenticate(request: RequestPipeline[R])(handler: AuthStateHandler): F[ResponsePipeline[P]] = {
    RetrieveBasicCredentialsFromHeader()(request) match {
      case Some(credentials) =>
        val loginInfo = LoginInfo(id, credentials.username)
        Sync[F].flatMap(authenticate(loginInfo, credentials.password)) {
          case Successful =>
            Sync[F].flatMap(identityReader(loginInfo)) {
              case Some(identity) => handler(Authenticated(identity, credentials, loginInfo))
              case None           => handler(MissingIdentity(credentials, loginInfo))
            }
          case InvalidPassword(error)   => handler(InvalidCredentials(credentials, NEL.of(error)))
          case UnsupportedHasher(error) => handler(AuthFailure(new ConfigurationException(error)))
          case NotFound(error)          => handler(InvalidCredentials(credentials, NEL.of(error)))
        }
      case None => handler(MissingCredentials())
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
