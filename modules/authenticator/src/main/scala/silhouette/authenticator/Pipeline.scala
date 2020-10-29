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

import cats.data.Validated.{ Invalid, Valid }
import cats.effect.Async
import silhouette._
import silhouette.authenticator.pipeline.Dsl.{ KleisliM, NoneError }

/**
 * Pipeline to authenticate an identity by transforming a source into an [[AuthState]].
 *
 * An authenticator can be read from different sources. The most common case would be to read the authenticator
 * from the request. But it would also be possible to read the authenticator from an actor or any other source.
 * If the source holds a serialized representation of an authenticator, we must transform it into an authenticator
 * instance by applying an [[AuthenticatorReader]]. Then we could check the validity of the authenticator by applying
 * one or multiple validators. As next we retrieve an [[Identity]] for our authenticator, either from a backing store
 * or any other service. At last we could apply some authorization rules to our identity. Any of these steps in our
 * pipeline can be represented by a [[AuthState]] that describes the result of each step.
 *
 * @param pipeline       The pipeline which transforms the source to an authenticator.
 * @param identityReader The reader to retrieve the [[Identity]] for the [[silhouette.authenticator.Authenticator]]
 *                       from the persistence layer or from the authenticator itself.
 * @param validators     The list of validators to apply to the [[silhouette.authenticator.Authenticator]].
 *
 * @tparam F The type of the IO monad.
 * @tparam S The type of the source.
 * @tparam I The type of the identity.
 */
final case class AuthenticationPipeline[F[_]: Async, S, I <: Identity](
  pipeline: KleisliM[F, S, Authenticator],
  identityReader: Authenticator => F[Option[I]],
  validators: Set[Validator[F]] = Set.empty[Validator[F]]
) extends (S => F[AuthState[I, Authenticator]]) {

  /**
   * Apply the pipeline.
   *
   * @param source The source to retrieve the authenticator from.
   * @return An authentication state.
   */
  override def apply(source: S): F[AuthState[I, Authenticator]] =
    Async[F].recover[AuthState[I, Authenticator]] {
      Async[F].flatMap(pipeline.run(source).value) {
        case Left(e: NoneError[I]) => Async[F].pure(e.state)
        case Left(e)               => Async[F].pure(AuthFailure(e))
        case Right(authenticator) =>
          Async[F].flatMap(authenticator.isValid(validators)) {
            case Invalid(errors) =>
              Async[F].pure(InvalidCredentials(authenticator, errors))
            case Valid(_) =>
              Async[F].map(identityReader(authenticator)) {
                case Some(identity) =>
                  Authenticated(identity, authenticator, authenticator.loginInfo)
                case None =>
                  MissingIdentity[I, Authenticator](authenticator, authenticator.loginInfo)
              }
          }
      }
    } { case e: Exception => AuthFailure(e) }
}

/**
 * Pipeline which writes an [[Authenticator]] to a target `T`.
 *
 * @param pipeline A pipeline that writes an [[Authenticator]] to the given target.
 * @tparam F The type of the IO monad.
 * @tparam T The type of the target.
 */
final case class TargetPipeline[F[_]: Async, T](pipeline: T => KleisliM[F, Authenticator, T])
  extends ((Authenticator, T) => F[T]) {

  /**
   * Writes an [[Authenticator]] to a target.
   *
   * @param authenticator The [[Authenticator]] to write to the target.
   * @param target The target to write to.
   * @return The target with the [[Authenticator]].
   */
  override def apply(authenticator: Authenticator, target: T): F[T] =
    Async[F].flatMap(pipeline(target).run(authenticator).value) {
      case Left(error)   => Async[F].raiseError(error)
      case Right(target) => Async[F].pure(target)
    }
}
