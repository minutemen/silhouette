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

import cats.Parallel
import cats.effect.Sync
import silhouette._
import silhouette.authenticator.Validator.{ Invalid, Valid }

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
 *
 * @tparam F The type of the IO monad.
 * @tparam S The type of the source.
 * @tparam I The type of the identity.
 */
case class AuthenticationPipeline[F[_]: Sync: Parallel, S, I <: Identity](
  pipeline: S => F[Option[Authenticator]],
  identityReader: LoginInfo => F[Option[I]],
  validators: Set[Validator[F]] = Set.empty[Validator[F]]
) extends silhouette.Reads[S, F[AuthState[I, Authenticator]]] with Pipeline { self =>

  /**
   * Monkey patches a `F[Authenticator]` to add a `toState` method which allows to transforms an [[Authenticator]]
   * into an [[AuthState]].
   *
   * @param value The instance to patch.
   */
  final implicit class AuthenticatorFuture(value: F[Authenticator]) {
    def toState: F[AuthState[I, Authenticator]] = self.toState[Authenticator](value, self.toState)
  }

  /**
   * Monkey patches a `Future[Option[Authenticator]]` to add a `toState` method which allows to transforms
   * an optional [[Authenticator]] into an [[AuthState]].
   *
   * @param value The instance to patch.
   */
  final implicit class OptionalAuthenticatorF(value: F[Option[Authenticator]]) {
    def toState: F[AuthState[I, Authenticator]] = self.toState[Option[Authenticator]](value, self.toState)
  }

  /**
   * Apply the pipeline.
   *
   * @param requestPipeline The request pipeline to retrieve the authenticator from.
   * @return An authentication state.
   */
  override def read(requestPipeline: S): F[AuthState[I, Authenticator]] = pipeline(requestPipeline).toState

  /**
   * Transforms the given IO monad into an authentication state by applying another transformation on the result
   * of the IO monad.
   *
   * @param io          The IO monad to apply the transformer to.
   * @param transformer The transformer to apply to the IO monad value.
   * @tparam T The type of the IO monad value.
   * @return The [[AuthState]].
   */
  final protected def toState[T](
    io: F[T],
    transformer: T => F[AuthState[I, Authenticator]]
  ): F[AuthState[I, Authenticator]] = {
    Sync[F].recover(Sync[F].flatMap(io)(transformer)) { case e: Exception => AuthFailure(e) }
  }

  /**
   * Transforms the given optional [[Authenticator]] into an [[AuthState]].
   *
   * @param maybeAuthenticator Maybe some authenticator or None if the authenticator couldn't be found.
   * @return The [[AuthState]].
   */
  final protected def toState(maybeAuthenticator: Option[Authenticator]): F[AuthState[I, Authenticator]] = {
    maybeAuthenticator match {
      case Some(authenticator) => toState(authenticator)
      case None                => Sync[F].pure(MissingCredentials())
    }
  }

  /**
   * Transforms the given [[Authenticator]] into an [[AuthState]].
   *
   * @param authenticator The authenticator.
   * @return The [[AuthState]].
   */
  final protected def toState(authenticator: Authenticator): F[AuthState[I, Authenticator]] = {
    Sync[F].flatMap(authenticator.isValid(validators)) {
      case Invalid(errors) => Sync[F].pure(InvalidCredentials(authenticator, errors))
      case Valid => Sync[F].map(identityReader(authenticator.loginInfo)) {
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
 * @tparam F The type of the IO monad.
 * @tparam T The type of the target.
 */
final case class TargetPipeline[F[_], T](pipeline: Authenticator => T => F[T])
  extends silhouette.Writes[(Authenticator, T), F[T]] {

  /**
   * Writes an [[Authenticator]] to a target.
   *
   * @param in A tuple consisting of the [[Authenticator]] and the target into which it should be written.
   * @return The target with the [[Authenticator]].
   */
  override def write(in: (Authenticator, T)): F[T] = pipeline(in._1)(in._2)
}
