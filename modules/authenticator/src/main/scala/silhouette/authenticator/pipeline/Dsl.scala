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
package silhouette.authenticator.pipeline

import cats.data.{ EitherT, Kleisli }
import cats.effect.Sync
import silhouette.authenticator.Authenticator
import silhouette.authenticator.pipeline.Dsl.NoneError
import silhouette.{ AuthState, Identity, MissingCredentials }

import scala.language.implicitConversions
import scala.util.Try

/**
 * A simple DSL to model authenticator pipelines.
 */
// TODO: Add doc examples
object Dsl extends DslLowPriorityImplicits {

  /**
   * Maybe is an IO monad that represents a success and an error case. It's represented by the monad transformer
   * [[EitherT]] from Cats, because it allows us to easily compose [[Either]] and `F[_]` together without writing
   * a lot of boilerplate and it makes it easy to compose it with [[Kleisli]].
   */
  type Maybe[F[_], A] = EitherT[F, Throwable, A]

  /**
   * A transformation function that transform a type `A` to [[Maybe]].
   */
  type MaybeWriter[F[_], A, B] = A => Maybe[F, B]

  /**
   * Kleisli that works with [[Maybe]].
   *
   * Represents a function `A => Maybe[F, B]`.
   */
  type KleisliM[F[_], A, B] = Kleisli[({ type L[T] = Maybe[F, T] })#L, A, B]

  /**
   * Option handles a missing case with [[None]] which cannot be easily translated into a throwable. Therefore this
   * [[Throwable]] represents the [[None]] case for the [[Maybe]] type which acts as an error that can directly be
   * translated to an auth state.
   */
  final case class NoneError[I <: Identity](state: AuthState[I, Authenticator]) extends Throwable()

  /**
   * The companion object for [[KleisliM]].
   */
  object KleisliM {

    /**
     * Constructs a [[KleisliM]] from a function `A` => `B`.
     *
     * @param f      The function to convert.
     * @param writes The writes that transforms the value of the function `A` => `B` into a [[KleisliM]].
     * @tparam F The type of the IO monad.
     * @tparam A The type of the function parameter.
     * @tparam B The type the function returns.
     * @tparam C The type that will be stored in [[Maybe]].
     * @return A [[KleisliM]] for a function `A` => `B`.
     */
    def apply[F[_], A, B, C](f: A => B)(
      implicit
      writes: MaybeWriter[F, B, C]
    ): KleisliM[F, A, C] = Kleisli((a: A) => writes(f(a)))

    /**
     * Constructs a [[KleisliM]] from a function [[Any]] => [[Unit]].
     *
     * This can be used to discard a value in a pipeline and set it to [[Unit]].
     *
     * @tparam F The type of the IO monad.
     * @return A [[KleisliM]] for a function [[Any]] => [[Unit]].
     */
    def discard[F[_]: Sync]: KleisliM[F, Any, Unit] = Kleisli((_: Any) => EitherT.pure[F, Throwable](()))
  }

  /**
   * Monkey patches a function `A` => `B`.
   *
   * @param f      The function to patch.
   * @param writes The writes that transforms the value of the function `A` => `B` into a [[KleisliM]].
   * @tparam F The type of the IO monad.
   * @tparam A The type of the function parameter.
   * @tparam B The type the function returns.
   * @tparam C The type that will be stored in [[Maybe]].
   */
  implicit class Function1Ops[F[_]: Sync, A, B, C](f: A => B)(implicit writes: MaybeWriter[F, B, C]) {

    /**
     * Start a pipeline by providing the unary operator `~` for a function `A` => `B`.
     *
     * @return A [[KleisliM]] for the function `A` => `B`.
     */
    def unary_~ : KleisliM[F, A, C] = KleisliM(f)
  }

  /**
   * Monkey patches a [[KleisliM]] instance.
   *
   * @param kleisliM The instance to patch.
   * @tparam F The type of the IO monad.
   * @tparam A The type of the function parameter.
   * @tparam B The type the function returns.
   */
  implicit class KleisliMOps[F[_]: Sync, A, B](kleisliM: KleisliM[F, A, B]) {

    /**
     * Composes two functions.
     *
     * An alias for the [[Kleisli.andThen]] function that allows to pass a [[KleisliM]] instance.
     *
     * @param k The function to compose.
     * @tparam C The type the function returns after the composition.
     * @return A new [[KleisliM]] from `A` to `C`.
     */
    def >>[C](k: KleisliM[F, B, C]): KleisliM[F, A, C] = kleisliM.andThen(k)

    /**
     * A function that discards the function parameter [[B]] by changing it to [[Unit]] before composing it with
     * the given [[KleisliM]].
     *
     * @param k The function to compose.
     * @tparam C The type the function returns after the composition.
     * @return A new [[KleisliM]] from `A` to `C`.
     */
    def xx[C](k: KleisliM[F, Unit, C]): KleisliM[F, A, C] = KleisliM.discard >> k
  }

  /**
   * A transformation function that transforms an [[Option]] to [[Maybe]].
   *
   * In case of [[None]], the function uses the implicit [[NoneError]], which can be translated directly to
   * an [[AuthState]]. There is automatically a low-priority implicit in scope, which translates to the
   * [[MissingCredentials]] state. This can be overridden by defining a custom implicit.
   *
   * @param noneError The error which should be used in the none case.
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @tparam I The type of the identity.
   * @return The [[Maybe]] representation for the [[Option]] type.
   */
  implicit def optionToMaybeWriter[F[_]: Sync, A, I <: Identity](
    implicit
    noneError: => NoneError[I]
  ): MaybeWriter[F, Option[A], A] = (value: Option[A]) =>
    EitherT.fromEither[F](value.toRight(noneError))

  /**
   * A transformation function that transforms a [[scala.util.Try]] to [[Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Maybe]] representation for the [[scala.util.Try]] type.
   */
  implicit def tryToMaybeWriter[F[_]: Sync, A]: MaybeWriter[F, Try[A], A] = (value: Try[A]) =>
    EitherT.fromEither[F](value.toEither)

  /**
   * A transformation function that transforms an `Either[Throwable, A]` to [[Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Maybe]] representation for the `Either[Throwable, A]` type.
   */
  implicit def eitherToMaybeWriter[F[_]: Sync, A]: MaybeWriter[F, Either[Throwable, A], A] =
    (value: Either[Throwable, A]) => EitherT.fromEither[F](value)

  /**
   * A transformation function that transforms a functional effect to [[Dsl.Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Dsl.Maybe]] representation for the `Either[Throwable, A]` type.
   */
  implicit def effectToMaybeWriter[F[_]: Sync, A, B](
    implicit
    writer: Dsl.MaybeWriter[F, A, B]
  ): Dsl.MaybeWriter[F, F[A], B] = (value: F[A]) =>
    for {
      r <- EitherT.right(value)
      v <- writer(r)
    } yield v

  /**
   * Provides an implicit conversion from a function `A` => `B` to [[KleisliM]] .
   *
   * @param f      The function to convert.
   * @param writes The writes that transforms the value of the function `A` => `B` into a [[KleisliM]].
   * @tparam F The type of the IO monad.
   * @tparam A The type of the function parameter.
   * @tparam B The type the function returns.
   * @tparam C The type that will be stored in [[Maybe]].
   * @return A [[KleisliM]] from a function `A` => `B`.
   */
  implicit def toKleisliM[F[_], A, B, C](f: A => B)(
    implicit
    writes: Dsl.MaybeWriter[F, B, C]
  ): Dsl.KleisliM[F, A, C] = Dsl.KleisliM(f)

  /**
   * An alias for the [[KleisliM.discard]] function.
   *
   * Constructs a [[KleisliM]] from a function [[Any]] => [[Unit]].
   *
   * This can be used to discard a value in a pipeline and set it to [[Unit]].
   *
   * @tparam F The type of the IO monad.
   * @return A [[KleisliM]] for a function [[Any]] => [[Unit]].
   */
  def xx[F[_]: Sync]: KleisliM[F, Any, Unit] = KleisliM.discard
}

/**
 * Low-priority implicits for the Dsl.
 */
trait DslLowPriorityImplicits {

  /**
   * A low priority transformation function that transforms any type to [[Dsl.Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Dsl.Maybe]] representation for type `A`.
   */
  implicit def toMaybeWrites[F[_]: Sync, A]: Dsl.MaybeWriter[F, A, A] = (value: A) =>
    EitherT.pure[F, Throwable](value)

  /**
   * A low priority transformation that returns a [[NoneError]] that can be translated to a [[MissingCredentials]]
   * state.
   *
   * @tparam I The type of the identity.
   * @return A [[NoneError]] that can be translated to a [[MissingCredentials]] state.
   */
  implicit def noneToMissingCredentials[I <: Identity]: Dsl.NoneError[I] =
    NoneError(MissingCredentials())
}
