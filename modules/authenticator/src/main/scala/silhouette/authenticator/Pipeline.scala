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

import silhouette.Fitting.futureFittingToFutureOption
import silhouette._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * An authenticator pipeline represents a step in the authentication process which is composed of multiple single steps.
 */
sealed trait Pipeline

/**
 * Pipeline to authenticate an identity by transforming a source into an [[AuthState]].
 *
 * An authenticator can be read from different sources. The most common case would be to read the authenticator
 * from the request. But it would also be possible to read the authenticator from an actor or any other source.
 * If the source holds a serialized representation of an authenticator, we must transform it into an authenticator
 * instance by applying an authenticator [[Reads]]. Then we could check the validity of the authenticator by applying
 * one or multiple validators. As next we retrieve an [[Identity]] for our authenticator, either from a backing store
 * or any other service. At last we could apply some authorization rules to our identity. Any of these steps in our
 * pipeline can be represented by a [[AuthState]] that describes the result of each step.
 *
 * @tparam S The type of the source.
 * @tparam I The type of the identity.
 */
trait AuthenticationPipeline[S, I <: Identity]
  extends silhouette.Reads[S, Future[AuthState[I, Authenticator]]] with Pipeline { self =>

  /**
   * Monkey patches a `Future[Authenticator]` to add a `toState` method which allows to transforms an [[Authenticator]]
   * into an [[AuthState]].
   *
   * @param value The instance to patch.
   */
  final implicit class AuthenticatorFuture(value: Future[Authenticator]) {
    def toState(
      implicit
      ec: ExecutionContext
    ): Future[AuthState[I, Authenticator]] = self.toState[Authenticator](value, self.toState)
  }

  /**
   * Monkey patches a `Future[Option[Authenticator]]` to add a `toState` method which allows to transforms
   * an optional [[Authenticator]] into an [[AuthState]].
   *
   * @param value The instance to patch.
   */
  final implicit class OptionalAuthenticatorFuture(value: Future[Option[Authenticator]]) {
    def toState(
      implicit
      ec: ExecutionContext
    ): Future[AuthState[I, Authenticator]] = self.toState[Option[Authenticator]](value, self.toState)
  }

  /**
   * Monkey patches a `Future[Fitting[Authenticator]]` to add a `toState` method which allows to transforms
   * an `Fitting[Authenticator]` into an [[AuthState]].
   *
   * @param value The instance to patch.
   */
  final implicit class AuthenticatorFitting(value: Future[Fitting[Authenticator]]) {
    def toState(
      implicit
      ec: ExecutionContext
    ): Future[AuthState[I, Authenticator]] = futureFittingToFutureOption(value).toState
  }

  /**
   * The reader to retrieve the [[Identity]] for the [[LoginInfo]] stored in the [[Authenticator]].
   */
  protected val identityReader: LoginInfo => Future[Option[I]]

  /**
   * The list of validators to apply to the [[Authenticator]] from the persistence layer.
   */
  protected val validators: Set[Validator] = Set()

  /**
   * Transforms the given future into an authentication state by applying another transformation on the result
   * of the future.
   *
   * @param future          The future to apply the transformer to.
   * @param transformer     The transformer to apply to the future value.
   * @param ec              The execution context.
   * @tparam T The type of the future value.
   * @return The [[AuthState]].
   */
  final protected def toState[T](future: Future[T], transformer: T => Future[AuthState[I, Authenticator]])(
    implicit
    ec: ExecutionContext
  ): Future[AuthState[I, Authenticator]] = future.flatMap(transformer)

  /**
   * Transforms the given optional [[Authenticator]] into an [[AuthState]].
   *
   * @param maybeAuthenticator Maybe some authenticator or None if the authenticator couldn't be found.
   * @param ec                 The execution context.
   * @return The [[AuthState]].
   */
  final protected def toState(maybeAuthenticator: Option[Authenticator])(
    implicit
    ec: ExecutionContext
  ): Future[AuthState[I, Authenticator]] = {
    maybeAuthenticator match {
      case Some(authenticator) => toState(authenticator)
      case None                => Future.successful(MissingCredentials())
    }
  }

  /**
   * Transforms the given [[Authenticator]] into an [[AuthState]].
   *
   * @param authenticator The authenticator.
   * @param ec            The execution context.
   * @return The [[AuthState]].
   */
  final protected def toState(authenticator: Authenticator)(
    implicit
    ec: ExecutionContext
  ): Future[AuthState[I, Authenticator]] = {
    authenticator.isValid(validators).flatMap {
      case false => Future.successful(InvalidCredentials(authenticator))
      case true => identityReader(authenticator.loginInfo).map {
        case Some(identity) => Authenticated[I, Authenticator](identity, authenticator, authenticator.loginInfo)
        case None           => MissingIdentity(authenticator, authenticator.loginInfo)
      }
    }
  }
}

/**
 * Pipeline which transforms an authenticator into another authenticator.
 */
trait TransformPipeline extends silhouette.Writes[Authenticator, Future[Authenticator]]

/**
 * Pipeline which merges an [[Authenticator]] and a target `T` into a target `T` that contains the given
 * [[Authenticator]].
 *
 * @tparam T The type of the target.
 */
trait WritePipeline[T] extends silhouette.Writes[(Authenticator, T), Future[T]]
