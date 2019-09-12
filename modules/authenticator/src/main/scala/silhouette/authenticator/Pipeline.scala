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
import silhouette.authenticator.Validator.{ Invalid, Valid }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * An authenticator pipeline represents a composition of multiple single steps in the authentication process.
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
 * @param pipeline       The pipeline which transforms the source into an authenticator.
 * @param identityReader The reader to retrieve the [[Identity]] for the [[LoginInfo]] stored in the
 *                       [[silhouette.authenticator.Authenticator]] from the persistence layer.
 * @param validators     The list of validators to apply to the [[silhouette.authenticator.Authenticator]].
 * @param ec             The execution context.
 *
 * @tparam S The type of the source.
 * @tparam I The type of the identity.
 */
case class AuthenticationPipeline[S, I <: Identity](
  pipeline: S => Future[Option[Authenticator]],
  identityReader: LoginInfo => Future[Option[I]],
  validators: Set[Validator] = Set()
)(
  implicit
  ec: ExecutionContext
) extends silhouette.Reads[S, Future[AuthState[I, Authenticator]]] with Pipeline { self =>

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
   * Apply the pipeline.
   *
   * @param requestPipeline The request pipeline to retrieve the authenticator from.
   * @return An authentication state.
   */
  override def read(requestPipeline: S): Future[AuthState[I, Authenticator]] = pipeline(requestPipeline).toState

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
  ): Future[AuthState[I, Authenticator]] = {
    future.flatMap(transformer).recover { case e: Exception => AuthFailure(e) }
  }

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
      case Invalid(errors) => Future.successful(InvalidCredentials(authenticator, errors))
      case Valid => identityReader(authenticator.loginInfo).map {
        case Some(identity) => Authenticated[I, Authenticator](identity, authenticator, authenticator.loginInfo)
        case None           => MissingIdentity(authenticator, authenticator.loginInfo)
      }
    }
  }
}

/**
 * Pipeline which writes an [[Authenticator]] to a target `T`.
 *
 * @param pipeline A pipeline that writes an [[Authenticator]] to the given target.
 * @tparam T The type of the target.
 */
final case class TargetPipeline[T](pipeline: Authenticator => T => Future[T])
  extends silhouette.Writes[(Authenticator, T), Future[T]] {

  /**
   * Writes an [[Authenticator]] to a target.
   *
   * @param in A tuple consisting of the [[Authenticator]] and the target into which it should be written.
   * @return The target with the [[Authenticator]].
   */
  override def write(in: (Authenticator, T)): Future[T] = pipeline(in._1)(in._2)
}
