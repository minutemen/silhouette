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
package silhouette.authorization

import cats.effect.Async
import cats.syntax.all._
import silhouette.Identity

/**
 * A trait to define an authorization policy that can be applied to an identity to provide access control to resources.
 *
 * @tparam F The type of the IO monad.
 * @tparam I The type of the identity.
 * @tparam C The type of the context.
 */
trait Authorization[F[_], I <: Identity, C] {

  /**
   * Checks whether the user is authorized or not.
   *
   * @param identity The current identity instance.
   * @param context  The current context instance.
   * @return True if the user is authorized, false otherwise.
   */
  def isAuthorized(identity: I, context: C): F[Boolean]
}

/**
 * An authorization policy that always grants access to resources.
 *
 * @tparam F The type of the IO monad.
 */
case class Authorized[F[_]: Async]() extends Authorization[F, Identity, Any] {

  /**
   * Checks whether the user is authorized or not.
   *
   * @param identity The current identity instance.
   * @param context  The current context instance.
   * @return True if the user is authorized, false otherwise.
   */
  override def isAuthorized(identity: Identity, context: Any): F[Boolean] =
    Async[F].pure(true)
}

/**
 * An authorization policy that always denies access to resources.
 *
 * @tparam F The type of the IO monad.
 */
case class Unauthorized[F[_]: Async]() extends Authorization[F, Identity, Any] {

  /**
   * Checks whether the user is authorized or not.
   *
   * @param identity The current identity instance.
   * @param context  The current context instance.
   * @return True if the user is authorized, false otherwise.
   */
  override def isAuthorized(identity: Identity, context: Any): F[Boolean] =
    Async[F].pure(false)
}

/**
 * The companion object.
 */
object Authorization {

  /**
   * Defines additional methods on an `Authorization` instance.
   *
   * @param self The `Authorization` instance on which the additional methods should be defined.
   * @tparam F The type of the IO monad.
   * @tparam I The type of the identity.
   * @tparam C The type of the context.
   */
  implicit final class RichAuthorization[F[_]: Async, I <: Identity, C](self: Authorization[F, I, C]) {

    /**
     * Performs a logical negation on an `Authorization` result.
     *
     * @return An `Authorization` which performs a logical negation on an `Authorization` result.
     */
    def unary_! : Authorization[F, I, C] =
      (identity: I, context: C) => {
        Async[F].map(self.isAuthorized(identity, context))(x => !x)
      }

    /**
     * Performs a logical AND operation with two `Authorization` instances.
     *
     * @param authorization The right hand operand.
     * @return An authorization which performs a logical AND operation with two `Authorization` instances.
     */
    def &&(authorization: Authorization[F, I, C]): Authorization[F, I, C] =
      (identity: I, context: C) => {
        val leftF = self.isAuthorized(identity, context)
        val rightF = authorization.isAuthorized(identity, context)
        for {
          left <- leftF
          right <- rightF
        } yield left && right
      }

    /**
     * Performs a logical OR operation with two `Authorization` instances.
     *
     * @param authorization The right hand operand.
     * @return An authorization which performs a logical OR operation with two `Authorization` instances.
     */
    def ||(authorization: Authorization[F, I, C]): Authorization[F, I, C] =
      (identity: I, context: C) => {
        val leftF = self.isAuthorized(identity, context)
        val rightF = authorization.isAuthorized(identity, context)
        for {
          left <- leftF
          right <- rightF
        } yield left || right
      }
  }
}
