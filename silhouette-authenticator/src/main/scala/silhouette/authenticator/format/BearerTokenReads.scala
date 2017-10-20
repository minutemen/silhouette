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

import silhouette.authenticator.Reads
import silhouette.authenticator.format.BearerTokenReads._
import silhouette.{ Authenticator, AuthenticatorException }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A reads which transforms a bearer token into an authenticator.
 *
 * A bearer token represents a string that cannot store authenticator related data in it. Instead it needs
 * a mapping between this string and the authenticator related data, which is commonly handled through a
 * persistence layer like a database or a cache.
 *
 * @param reader The reader to retrieve the authenticator.
 * @param ex The execution context.
 */
final case class BearerTokenReads(reader: String => Future[Option[Authenticator]])(
  implicit
  ex: ExecutionContext
) extends Reads {

  /**
   * Transforms a bearer token into an [[Authenticator]].
   *
   * @param token The bearer token to transform.
   * @return An authenticator on success, an error on failure.
   */
  override def read(token: String): Future[Authenticator] = reader(token).map(_.getOrElse(
    throw new AuthenticatorException(MissingAuthenticator.format(token))
  ))
}

/**
 * The companion object.
 */
object BearerTokenReads {
  val MissingAuthenticator: String = "[Silhouette][BearerTokenReads] Cannot get authenticator for " +
    "id `%s` from given reader"
}
