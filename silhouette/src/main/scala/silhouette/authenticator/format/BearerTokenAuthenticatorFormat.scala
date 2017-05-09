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
package silhouette.authenticator.format

import javax.inject.Inject

import silhouette.Authenticator
import silhouette.authenticator.AuthenticatorFormat
import silhouette.authenticator.format.BearerTokenAuthenticatorFormat._
import silhouette.exceptions.AuthenticatorException
import silhouette.repositories.AuthenticatorRepository

import scala.concurrent.Future

/**
 * A format which transforms an authenticator into a bearer token and vice versa.
 *
 * We do not persist the authenticator in the write method, because the format isn't the right place to manage
 * the persistence of the token. This means writing a token should not always persist a token. In contrast,
 * the read method must always utilize the persistence layer.
 *
 * @param repository The repository to retrieve the authenticator from.
 */
final case class BearerTokenAuthenticatorFormat @Inject() (repository: AuthenticatorRepository)
  extends AuthenticatorFormat {

  /**
   * Transforms a bearer token into an [[Authenticator]].
   *
   * @param token The bearer token to transform.
   * @return An authenticator on success, an error on failure.
   */
  override def read(token: String): Future[Authenticator] = repository.find(token).map(_.getOrElse(
    throw new AuthenticatorException(MissingAuthenticator.format(token))
  ))

  /**
   * Transforms an [[Authenticator]] into a bearer token.
   *
   * @param authenticator The authenticator to transform.
   * @return A bearer token on success, an error on failure.
   */
  override def write(authenticator: Authenticator): Future[String] = Future.successful(authenticator.id)
}

/**
 * The companion object.
 */
object BearerTokenAuthenticatorFormat {
  val MissingAuthenticator = "[Silhouette][BearerTokenAuthenticatorFormat] Cannot get authenticator for id `%s` " +
    "from repository"
}
