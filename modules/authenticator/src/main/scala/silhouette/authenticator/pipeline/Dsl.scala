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
import cats.effect.{ Async, ContextShift, IO }
import silhouette.authenticator.Authenticator
import silhouette.authenticator.pipeline.Dsl.NoneError
import silhouette.{ AuthState, Identity, MissingCredentials }

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Try

/**
 * A simple DSL to model authenticator pipelines.
 */
object Dsl extends DslLowPriorityImplicits {

  /**
   * Maybe is an IO monad that represents a success and an error case. It's represented by the monad transformer
   * [[cats.data.EitherT]] from Cats, because it allows us to easily compose [[scala.util.Either]] and `F[_]` together
   * without writing a lot of boilerplate and it makes it easy to compose it with [[cats.data.Kleisli]].
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
   * Option handles a missing case with [[scala.None]] which cannot be easily translated into a throwable. Therefore
   * this [[scala.Throwable]] represents the [[scala.None]] case for the [[Maybe]] type which acts as an error that
   * can directly be translated to an auth state.
   */
  final case class NoneError[I <: Identity](state: AuthState[I, Authenticator]) extends Throwable()

  /**
   * The companion object for [[KleisliM]].
   */
  object KleisliM {

    /**
     * Lifts a function `A` => `B` with the help of a [[MaybeWriter]] into a [[KleisliM]].
     *
     * {{{
     * import silhouette.http.transport.RetrieveFromCookie
     * import silhouette.authenticator.pipeline.Dsl._
     * import silhouette.authenticator.transformer.SatReader
     *
     * KleisliM.lift(RetrieveFromCookie("test")) >> SatReader(...)
     * }}}
     *
     * @param f      The function to convert.
     * @param writes The writes that transforms the value of the function `A` => `B` into a [[KleisliM]].
     * @tparam F The type of the IO monad.
     * @tparam A The type of the function parameter.
     * @tparam B The type the function returns.
     * @tparam C The type that will be stored in [[Maybe]].
     * @return A [[KleisliM]] for a function `A` => `B`.
     */
    def lift[F[_], A, B, C](f: A => B)(
      implicit
      writes: MaybeWriter[F, B, C]
    ): KleisliM[F, A, C] = Kleisli((a: A) => writes(f(a)))

    /**
     * Lifts a new value into a [[KleisliM]].
     *
     * {{{
     * import silhouette.http.transport.{ DiscardCookie, CookieTransportConfig }
     * import silhouette.authenticator.pipeline.Dsl._
     *
     * KleisliM.liftV(target) >> DiscardCookie[Fake.Response](CookieTransportConfig("test"))
     * }}}
     *
     * The resulting [[KleisliM]] discards the incoming value and uses the lifted value as return value.
     *
     * @tparam F The type of the IO monad.
     * @return A [[KleisliM]] for a value [[scala.Any]] => `A`.
     */
    def liftV[F[_]: Async, A](a: A): KleisliM[F, Any, A] = Kleisli((_: Any) => EitherT.pure[F, Throwable](a))
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
  implicit class Function1Ops[F[_]: Async, A, B, C](f: A => B)(implicit writes: MaybeWriter[F, B, C]) {

    /**
     * Lifts a function `A` => `B` into a [[KleisliM]].
     *
     * {{{
     * import silhouette.http.transport.RetrieveFromCookie
     * import silhouette.authenticator.pipeline.Dsl._
     * import silhouette.authenticator.transformer.SatReader
     *
     * ~RetrieveFromCookie("authenticator") >> SatReader(...)
     * }}}
     *
     * @param writes The writes that transforms the value of the function `A` => `B` into a [[KleisliM]].
     * @return A [[KleisliM]] for the function `A` => `B`.
     */
    def unary_~(
      implicit
      writes: MaybeWriter[F, B, C]
    ): KleisliM[F, A, C] = KleisliM.lift(f)
  }

  /**
   * Monkey patches a [[KleisliM]] instance.
   *
   * @param kleisliM The instance to patch.
   * @tparam F The type of the IO monad.
   * @tparam A The type of the function parameter.
   * @tparam B The type the function returns.
   */
  implicit class KleisliMOps[F[_]: Async, A, B](kleisliM: KleisliM[F, A, B]) {

    /**
     * Composes two functions.
     *
     * An alias for the `cats.data.Kleisli.andThen` function that allows to pass a [[KleisliM]] instance.
     *
     * {{{
     * import silhouette.http.transport.RetrieveFromCookie
     * import silhouette.authenticator.pipeline.Dsl._
     * import silhouette.authenticator.transformer.SatReader
     *
     * ~RetrieveFromCookie("authenticator") >> SatReader(...)
     * }}}
     *
     * @param k The function to compose.
     * @tparam C The type the function returns after the composition.
     * @return A new [[KleisliM]] from `A` to `C`.
     */
    def >>[C](k: KleisliM[F, B, C]): KleisliM[F, A, C] = kleisliM.andThen(k)
  }

  /**
   * A transformation function that transforms an [[scala.Option]] to [[Maybe]].
   *
   * In case of [[scala.None]], the function uses the implicit [[NoneError]], which can be translated directly to
   * an [[AuthState]]. There is automatically a low-priority implicit in scope, which translates to the
   * `MissingCredentials` state. This can be overridden by defining a custom implicit.
   *
   * @param noneError The error which should be used in the none case.
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @tparam I The type of the identity.
   * @return The [[Maybe]] representation for the [[scala.Option]] type.
   */
  implicit def optionToMaybeWriter[F[_]: Async, A, I <: Identity](
    implicit
    noneError: () => NoneError[I]
  ): MaybeWriter[F, Option[A], A] = (value: Option[A]) =>
    EitherT.fromEither[F](value.toRight(noneError()))

  /**
   * A transformation function that transforms a [[scala.util.Try]] to [[Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Maybe]] representation for the [[scala.util.Try]] type.
   */
  implicit def tryToMaybeWriter[F[_]: Async, A]: MaybeWriter[F, Try[A], A] = (value: Try[A]) =>
    EitherT.fromEither[F](value.toEither)

  /**
   * A transformation function that transforms an `Either[Throwable, A]` to [[Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Maybe]] representation for the `Either[Throwable, A]` type.
   */
  implicit def eitherToMaybeWriter[F[_]: Async, A]: MaybeWriter[F, Either[Throwable, A], A] =
    (value: Either[Throwable, A]) => EitherT.fromEither[F](value)

  /**
   * A transformation function that transforms a [[scala.concurrent.Future]] effect to [[Dsl.Maybe]].
   *
   * @tparam A The type to convert.
   * @return The [[Dsl.Maybe]] representation for the `Either[Throwable, A]` type.
   */
  implicit def futureToMaybeWriter[A, B](
    implicit
    writer: Dsl.MaybeWriter[IO, A, B],
    contextShift: ContextShift[IO]
  ): Dsl.MaybeWriter[IO, Future[A], B] = (value: Future[A]) =>
    for {
      r <- EitherT.right(IO.fromFuture(IO(value)))
      v <- writer(r)
    } yield v

  /**
   * A transformation function that transforms a functional effect to [[Dsl.Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Dsl.Maybe]] representation for the `Either[Throwable, A]` type.
   */
  implicit def effectToMaybeWriter[F[_]: Async, A, B](
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
  ): Dsl.KleisliM[F, A, C] = Dsl.KleisliM.lift(f)

  /**
   * An alias for the [[KleisliM.liftV]] function.
   *
   * {{{
   * import silhouette.http.transport.{ DiscardCookie, CookieTransportConfig }
   * import silhouette.authenticator.pipeline.Dsl._
   *
   * xx(target) >> DiscardCookie[Fake.Response](CookieTransportConfig("test"))
   * }}}
   *
   * @tparam F The type of the IO monad.
   * @return A [[KleisliM]] for a value [[scala.Any]] => `A`.
   */
  def xx[F[_]: Async, C](a: C): KleisliM[F, Any, C] = KleisliM.liftV(a)
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
  implicit def toMaybeWrites[F[_]: Async, A]: Dsl.MaybeWriter[F, A, A] = (value: A) =>
    EitherT.pure[F, Throwable](value)

  /**
   * A low priority transformation that returns a [[Dsl.NoneError]] that can be translated to a `MissingCredentials` s
   * tate.
   *
   * @tparam I The type of the identity.
   * @return A [[Dsl.NoneError]] that can be translated to a `MissingCredentials` state.
   */
  implicit def noneToMissingCredentials[I <: Identity]: () => Dsl.NoneError[I] =
    () => NoneError(MissingCredentials())
}
