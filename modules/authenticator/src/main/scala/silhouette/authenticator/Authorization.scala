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
package silhouette.authenticator

import silhouette.Identity

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A trait to define Authorization objects that let you hook an authorization implementation into an
 * authentication pipeline.
 *
 * @tparam I The type of the identity.
 */
trait Authorization[I <: Identity] {

  /**
   * Checks whether the user is authorized or not.
   *
   * @param identity      The current identity instance.
   * @param authenticator The current authenticator instance.
   * @return True if the user is authorized, false otherwise.
   */
  def isAuthorized(identity: I, authenticator: Authenticator): Future[Boolean]
}

/**
 * An authorization that returns always true.
 *
 * @tparam I The type of the identity.
 */
final case class Authorized[I <: Identity]() extends Authorization[I] {

  /**
   * Checks whether the user is authorized or not.
   *
   * @param identity      The current identity instance.
   * @param authenticator The current authenticator instance.
   * @return True if the user is authorized, false otherwise.
   */
  override def isAuthorized(identity: I, authenticator: Authenticator): Future[Boolean] = Future.successful(true)
}

/**
 * An authorization that returns always false.
 *
 * @tparam I The type of the identity.
 */
final case class Unauthorized[I <: Identity]() extends Authorization[I] {

  /**
   * Checks whether the user is authorized or not.
   *
   * @param identity      The current identity instance.
   * @param authenticator The current authenticator instance.
   * @return True if the user is authorized, false otherwise.
   */
  override def isAuthorized(identity: I, authenticator: Authenticator): Future[Boolean] = Future.successful(false)
}

/**
 * The companion object.
 */
object Authorization {

  /**
   * Defines additional methods on an `Authorization` instance.
   *
   * @param self The `Authorization` instance on which the additional methods should be defined.
   * @param ec   The execution context to handle the asynchronous operations.
   */
  implicit final class RichAuthorization[I <: Identity](self: Authorization[I])(
    implicit
    ec: ExecutionContext
  ) {

    /**
     * Performs a logical negation on an `Authorization` result.
     *
     * @return An `Authorization` which performs a logical negation on an `Authorization` result.
     */
    def unary_! : Authorization[I] = (identity: I, authenticator: Authenticator) => {
      self.isAuthorized(identity, authenticator).map(x => !x)
    }

    /**
     * Performs a logical AND operation with two `Authorization` instances.
     *
     * @param authorization The right hand operand.
     * @return An authorization which performs a logical AND operation with two `Authorization` instances.
     */
    def &&(authorization: Authorization[I]): Authorization[I] = (identity: I, authenticator: Authenticator) => {
      val leftF = self.isAuthorized(identity, authenticator)
      val rightF = authorization.isAuthorized(identity, authenticator)
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
    def ||(authorization: Authorization[I]): Authorization[I] = (identity: I, authenticator: Authenticator) => {
      val leftF = self.isAuthorized(identity, authenticator)
      val rightF = authorization.isAuthorized(identity, authenticator)
      for {
        left <- leftF
        right <- rightF
      } yield left || right
    }
  }
}
