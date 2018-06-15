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

import silhouette.authenticator.format.SatReads._
import silhouette.authenticator.{ Authenticator, AuthenticatorException, StatefulReads }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A reads which transforms a SAT (simple authentication token) into an authenticator.
 *
 * A simple authentication token represents a string that cannot store authenticator related data in it. Instead
 * it needs a mapping between this string and the authenticator related data, which is commonly handled through a
 * backing store.
 *
 * @param reader The reader to retrieve the [[Authenticator]] for the given token from persistence layer.
 * @param ex     The execution context.
 */
final case class SatReads(reader: String => Future[Option[Authenticator]])(
  implicit
  ex: ExecutionContext
) extends StatefulReads {

  /**
   * Transforms a simple authentication token into an [[Authenticator]].
   *
   * @param token The simple authentication token to transform.
   * @return An authenticator on success, an error on failure.
   */
  override def read(token: String): Future[Authenticator] = reader(token).map(_.getOrElse(
    throw new AuthenticatorException(MissingAuthenticator.format(token))
  ))
}

/**
 * The companion object.
 */
object SatReads {
  val MissingAuthenticator: String = "Cannot get authenticator for token `%s` from given reader"
}
