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
package silhouette.authenticator.transformer

import cats.effect.Async
import silhouette.authenticator.transformer.SatReader._
import silhouette.authenticator.{ Authenticator, AuthenticatorException, AuthenticatorReader }

/**
 * A transformation function that transforms a SAT (simple authentication token) into an authenticator.
 *
 * A simple authentication token represents a string that cannot store authenticator related data in it. Instead
 * it needs a mapping between this string and the authenticator related data, which is commonly handled through a
 * backing store.
 *
 * @param reader The reader to retrieve the [[Authenticator]] for the given token from persistence layer.
 * @tparam F The type of the IO monad.
 */
final case class SatReader[F[_]: Async](reader: String => F[Option[Authenticator]])
  extends AuthenticatorReader[F, String] {

  /**
   * Transforms a simple authentication token into an [[Authenticator]].
   *
   * @param token The simple authentication token to transform.
   * @return An authenticator on success, an error on failure.
   */
  override def apply(token: String): F[Authenticator] = Async[F].flatMap(reader(token))(
    _.fold[F[Authenticator]] {
      Async[F].raiseError(new AuthenticatorException(MissingAuthenticator.format(token)))
    } {
      Async[F].pure
    }
  )
}

/**
 * The companion object.
 */
object SatReader {
  val MissingAuthenticator: String = "Cannot get authenticator for token `%s` from given reader"
}
