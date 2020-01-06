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
package silhouette

import cats.Monad
import cats.data.EitherT

import scala.language.implicitConversions
import scala.util.Try

/**
 * The type [[Maybe]] generalizes the types [[cats.Monad]], [[scala.util.Try]], [[scala.util.Either]] or [[Option]] so
 * that they can be composed.
 *
 * The great thing about functions is that they compose. A function `A => B` can be composed with another function
 * `B => C` to build a function `A => C`. To compose functions that return a monadic value we can use
 * [[cats.data.Kleisli]] from Cats. The problem with [[cats.data.Kleisli]] is, that it only composes functions of the
 * same monadic type. It cannot compose a function `A => Option[B]` with a function `B => Either[Error, C]`.
 *
 * Silhouette has a lot of functions that return a [[cats.Monad]], [[scala.util.Try]], [[scala.util.Either]] or
 * [[Option]]. All of this types can represent a success and a failed state. So the idea was to generalize this types
 * into the type [[Maybe]] so that they can be composed with [[cats.data.Kleisli]].
 *
 * The type [[Maybe]] is represented through the type `F[Either[Throwable, A]]` because it combines all the
 * functionality we need for our functions. It's async and it can either be an error or a successful value. We provide
 * implicit converters for the types [[cats.Monad]], [[Try]], [[Either]] and [[Option]] that transform these types to
 * [[Maybe]]. There is also a low priority converter that transforms any type to [[Maybe]]. This will work with types
 * like [[Int]], [[String]] or other types but may not work with other monadic types. In such case an implicit converter
 * should be created.
 */
object Maybe {

  /**
   * Option handles a missing case with [[None]] and not really an error case. This [[Throwable]] represents the
   * [[None]] case for the [[Maybe]] type.
   */
  case class NonException()
    extends Throwable("None value detected! This indicates that a value was not found in an Fitting pipeline")

  /**
   * The type description of the type [[Maybe]].
   *
   * We use the monad transformer [[EitherT]] from the Cats library, because it allows us to easily compose [[Either]]
   * and `F` together without writing a lot of boilerplate.
   */
  type Maybe[F[_], A] = EitherT[F, Throwable, A]

  /**
   * A transformation function that transform a type `A` to [[Maybe]].
   */
  type MaybeWriter[F[_], A, B] = A => Maybe[F, B]

  trait LowPriorityImplicits {

    /**
     * A low priority transformation function that transform any type to [[Maybe]].
     *
     * @tparam F The IO monad.
     * @tparam A The type to convert.
     * @return The [[Maybe]] representation for type `A`.
     */
    implicit def toMaybeWrites[F[_]: Monad, A]: MaybeWriter[F, A, A] = (value: A) =>
      EitherT.pure[F, Throwable](value)
  }

  object Implicits extends LowPriorityImplicits {

    /**
     * A transformation function that transforms an [[Option]] to [[Maybe]].
     *
     * @tparam F The IO monad.
     * @tparam A The type to convert.
     * @return The [[Maybe]] representation for the [[Option]] type.
     */
    implicit def optionToMaybeWriter[F[_]: Monad, A]: MaybeWriter[F, Option[A], A] = (value: Option[A]) =>
      EitherT.fromEither[F](value.toRight(NonException()))

    /**
     * A transformation function that transforms a [[scala.util.Try]] to [[Maybe]].
     *
     * @tparam F The IO monad.
     * @tparam A The type to convert.
     * @return The [[Maybe]] representation for the [[scala.util.Try]] type.
     */
    implicit def tryToMaybeWriter[F[_]: Monad, A]: MaybeWriter[F, Try[A], A] = (value: Try[A]) =>
      EitherT.fromEither[F](value.toEither)

    /**
     * A transformation function that transforms an `Either[Throwable, A]` to [[Maybe]].
     *
     * @tparam F The IO monad.
     * @tparam A The type to convert.
     * @return The [[Maybe]] representation for the `Either[Throwable, A]` type.
     */
    implicit def eitherToMaybeWriter[F[_]: Monad, A]: MaybeWriter[F, Either[Throwable, A], A] =
      (value: Either[Throwable, A]) => EitherT.fromEither[F](value)

    /**
     * A transformation function that transforms a functional effect to [[Maybe]].
     *
     * @tparam F The IO monad.
     * @tparam A The type to convert.
     * @return The [[Maybe]] representation for the `Either[Throwable, A]` type.
     */
    implicit def effectToMaybeWriter[F[_]: Monad, A, B](
      implicit
      writer: MaybeWriter[F, A, B]
    ): MaybeWriter[F, F[A], B] = (value: F[A]) =>
      for {
        r <- EitherT.right(value)
        v <- writer(r)
      } yield v

    /**
     * Converts a type with the help of a transformation function into a [[Maybe]] type.
     *
     * @param value  The value to convert.
     * @param writer The transformation function that transforms from `A` to [[Maybe]].
     * @tparam F The IO monad.
     * @tparam A The source type.
     * @tparam B The type of the success case [[Maybe]] handles.
     * @return The [[Maybe]] representation of type `A`.
     */
    implicit def toMaybe[F[_]: Monad, A, B](value: A)(
      implicit
      writer: MaybeWriter[F, A, B]
    ): Maybe[F, B] = writer(value)
  }
}
