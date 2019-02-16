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

import com.typesafe.scalalogging.LazyLogging
import silhouette.Fitting._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{ Failure, Success, Try }

/**
 * A fitting is a helper that can connect different formats to create a format pipeline.
 *
 * It's a type that can connect different container types like `Option`, `Try` and `Future`. These types share all
 * the same behaviour, because they represent two kind of states. The first one is a successful state represented
 * by a value and the second is a failure state represented by an error type.
 *
 * A fitting provides methods to convert between theses values with the help of [[Reads]] and [[Writes]].
 *
 * @tparam A The type of the value the fitting does handle.
 */
sealed trait Fitting[A] {

  /**
   * Converts a fitting to an [[scala.Option]].
   *
   * @return The resulting [[scala.Option]].
   */
  def toOption: Option[A]

  /**
   * Converts a fitting to a [[scala.util.Try]].
   *
   * @return The resulting [[scala.util.Try]].
   */
  def toTry: Try[A]

  /**
   * Converts a fitting to a [[scala.concurrent.Future]].
   *
   * @return The resulting [[scala.concurrent.Future]].
   */
  def toFuture: Future[A]

  /**
   * Takes a [[Reads]] that converts the fitting value `A` into an `Option[B]`.
   *
   * @param reads The [[Reads]] to convert the value.
   * @tparam B The type to convert to.
   * @return A converted [[Fitting]].
   */
  def andThenOption[B](reads: Reads[A, Option[B]]): Fitting[B]

  /**
   * Takes a [[Writes]] that converts the fitting value `A` into an `Option[B]`.
   *
   * @param writes The [[Writes]] to convert the value.
   * @tparam B The type to convert to.
   * @return A converted [[Fitting]].
   */
  def andThenOption[B](writes: Writes[A, Option[B]]): Fitting[B]

  /**
   * Takes a [[Reads]] that converts the fitting value `A` into an `Try[B]`.
   *
   * @param reads The [[Reads]] to convert the value.
   * @tparam B The type to convert to.
   * @return A converted [[Fitting]].
   */
  def andThenTry[B](reads: Reads[A, Try[B]]): Fitting[B]

  /**
   * Takes a [[Writes]] that converts the fitting value `A` into an `Try[B]`.
   *
   * @param writes The [[Writes]] to convert the value.
   * @tparam B The type to convert to.
   * @return A converted [[Fitting]].
   */
  def andThenTry[B](writes: Writes[A, Try[B]]): Fitting[B]

  /**
   * Takes a [[Reads]] that converts the fitting value `A` into an `Future[B]`.
   *
   * @param reads The [[Reads]] to convert the value.
   * @tparam B The type to convert to.
   * @return A converted [[Fitting]].
   */
  def andThenFuture[B](reads: Reads[A, Future[B]]): Future[Fitting[B]]

  /**
   * Takes a [[Writes]] that converts the fitting value `A` into an `Future[B]`.
   *
   * @param writes The [[Writes]] to convert the value.
   * @tparam B The type to convert to.
   * @return A converted [[Fitting]].
   */
  def andThenFuture[B](writes: Writes[A, Future[B]]): Future[Fitting[B]]
}

/**
 * Represents a successful fitting value.
 *
 * @param value The fitting value.
 * @tparam A The type of the value the fitting does handle.
 */
final case class SuccessfulFitting[A](value: A) extends Fitting[A] {
  override def toOption: Option[A] = Some(value)
  override def toTry: Try[A] = Success(value)
  override def toFuture: Future[A] = Future.successful(value)

  override def andThenOption[B](reads: Reads[A, Option[B]]): Fitting[B] = optionToFitting(reads.read(value))
  override def andThenTry[B](reads: Reads[A, Try[B]]): Fitting[B] = tryToFitting(reads.read(value))
  override def andThenFuture[B](reads: Reads[A, Future[B]]): Future[Fitting[B]] =
    futureToFutureFitting(reads.read(value))

  override def andThenOption[B](writes: Writes[A, Option[B]]): Fitting[B] = optionToFitting(writes.write(value))
  override def andThenTry[B](writes: Writes[A, Try[B]]): Fitting[B] = tryToFitting(writes.write(value))
  override def andThenFuture[B](writes: Writes[A, Future[B]]): Future[Fitting[B]] =
    futureToFutureFitting(writes.write(value))
}

/**
 * Represents a faulty fitting.
 *
 * @param error The error type.
 * @tparam A The type of the value the fitting does handle.
 */
final case class FaultyFitting[A](error: FittingError) extends Fitting[A] with LazyLogging {
  override def toOption: Option[A] = error match {
    case ThrowableError(e) =>
      logger.debug("ThrowableError to None conversion", e)
      None
    case NoneError => None
  }
  override def toTry: Try[A] = Failure(error.value)
  override def toFuture: Future[A] = Future.failed(error.value)

  override def andThenOption[B](reads: Reads[A, Option[B]]): Fitting[B] = FaultyFitting(error)
  override def andThenTry[B](reads: Reads[A, Try[B]]): Fitting[B] = FaultyFitting(error)
  override def andThenFuture[B](reads: Reads[A, Future[B]]): Future[Fitting[B]] =
    Future.successful(FaultyFitting(error))

  override def andThenOption[B](writes: Writes[A, Option[B]]): Fitting[B] = FaultyFitting(error)
  override def andThenTry[B](writes: Writes[A, Try[B]]): Fitting[B] = FaultyFitting(error)
  override def andThenFuture[B](writes: Writes[A, Future[B]]): Future[Fitting[B]] =
    Future.successful(FaultyFitting(error))
}

/**
 * Represents the error type of a fitting.
 */
sealed trait FittingError {

  /**
   * The error value.
   */
  val value: Throwable
}

/**
 * Represents an throwable error that was caught during an operation.
 *
 * The `Try` and `Future` types user throwable errors for their error case. This is their counterpart handled
 * by a fitting type.
 *
 * @param value The error value.
 */
final case class ThrowableError(value: Throwable) extends FittingError

/**
 * Represents the `None` type of an option.
 */
case object NoneError extends FittingError {
  override val value: Throwable = new IllegalStateException("None value detected")
}

/**
 * Some implicit fitting converters.
 */
object Fitting {

  /**
   * Converts a `Reads[A, Option[B]])` into a function that accepts `A` and returns a `Fitting[B]`.
   *
   * @param reads The reads to convert.
   * @tparam A The source type.
   * @tparam B The target type.
   * @return A function that accepts `A` and returns a `Fitting[B]`.
   */
  implicit def optionReadsToFitting[A, B](reads: Reads[A, Option[B]]): A => Fitting[B] = (in: A) => reads.read(in)

  /**
   * Converts a `Reads[A, Try[B]])` into a function that accepts `A` and returns a `Fitting[B]`.
   *
   * @param reads The reads to convert.
   * @tparam A The source type.
   * @tparam B The target type.
   * @return A function that accepts `A` and returns a `Fitting[B]`.
   */
  implicit def tryReadsToFitting[A, B](reads: Reads[A, Try[B]]): A => Fitting[B] = (in: A) => reads.read(in)

  /**
   * Converts a `Reads[A, Future[B]])` into a function that accepts `A` and returns a `Future[Fitting[B]]`.
   *
   * @param reads The reads to convert.
   * @tparam A The source type.
   * @tparam B The target type.
   * @return A function that accepts `A` and returns a `Future[Fitting[B]]`.
   */
  implicit def futureReadsToFitting[A, B](reads: Reads[A, Future[B]]): A => Future[Fitting[B]] =
    (in: A) => reads.read(in)

  /**
   * Converts a `Writes[A, Option[B]])` into a function that accepts `A` and returns a `Fitting[B]`.
   *
   * @param writes The writes to convert.
   * @tparam A The source type.
   * @tparam B The target type.
   * @return A function that accepts `A` and returns a `Fitting[B]`.
   */
  implicit def optionWritesToFitting[A, B](writes: Writes[A, Option[B]]): A => Fitting[B] = (in: A) => writes.write(in)

  /**
   * Converts a `Writes[A, Try[B]])` into a function that accepts `A` and returns a `Fitting[B]`.
   *
   * @param writes The writes to convert.
   * @tparam A The source type.
   * @tparam B The target type.
   * @return A function that accepts `A` and returns a `Fitting[B]`.
   */
  implicit def tryWritesToFitting[A, B](writes: Writes[A, Try[B]]): A => Fitting[B] = (in: A) => writes.write(in)

  /**
   * Converts a `Writes[A, Future[B]])` into a function that accepts `A` and returns a `Future[Fitting[B]]`.
   *
   * @param writes The writes to convert.
   * @tparam A The source type.
   * @tparam B The target type.
   * @return A function that accepts `A` and returns a `Future[Fitting[B]]`.
   */
  implicit def futureWritesToFitting[A, B](writes: Writes[A, Future[B]]): A => Future[Fitting[B]] =
    (in: A) => writes.write(in)

  /**
   * Converts an `Option[T]` to a `Fitting[T]`.
   *
   * @param value The [[scala.Option]] to convert.
   * @tparam T The type of the [[scala.Option]] value.
   * @return The resulting [[Fitting]].
   */
  implicit def optionToFitting[T](value: Option[T]): Fitting[T] = value match {
    case Some(v) => SuccessfulFitting(v)
    case None    => FaultyFitting(NoneError)
  }

  /**
   * Converts a `Try[T]` to a `Fitting[T]`.
   *
   * @param value The [[scala.util.Try]] to convert.
   * @tparam T The type of the [[scala.util.Try]] value.
   * @return The resulting [[Fitting]].
   */
  implicit def tryToFitting[T](value: Try[T]): Fitting[T] = value match {
    case Success(v) => SuccessfulFitting(v)
    case Failure(e) => FaultyFitting(ThrowableError(e))
  }

  /**
   * Converts a `Future[T]` to a `Future[Fitting[T]]`.
   *
   * @param value The [[scala.concurrent.Future]] to convert.
   * @tparam T The type of the [[scala.concurrent.Future]] value.
   * @return The resulting [[Fitting]].
   */
  implicit def futureToFutureFitting[T](value: Future[T]): Future[Fitting[T]] = value.map { v =>
    SuccessfulFitting(v)
  }.recover {
    case e => FaultyFitting(ThrowableError(e))
  }

  /**
   * Converts a fitting to an [[scala.Option]].
   *
   * @param fitting The fitting to convert.
   * @tparam T The type of the [[scala.Option]] value.
   * @return The resulting [[scala.Option]].
   */
  implicit def fittingToOption[T](fitting: Fitting[T]): Option[T] = fitting.toOption

  /**
   * Converts a fitting to a [[scala.util.Try]].
   *
   * @param fitting The fitting to convert.
   * @tparam T The type of the [[scala.util.Try]] value.
   * @return The resulting [[scala.util.Try]].
   */
  implicit def fittingToTry[T](fitting: Fitting[T]): Try[T] = fitting.toTry

  /**
   * Converts a fitting to a [[scala.concurrent.Future]].
   *
   * @param fitting The fitting to convert.
   * @tparam T The type of the [[scala.concurrent.Future]] value.
   * @return The resulting [[scala.concurrent.Future]].
   */
  implicit def fittingToFuture[T](fitting: Fitting[T]): Future[T] = fitting.toFuture

  /**
   * Converts a `Future[Fitting[T]]` to a `Future[Option[T]]`.
   *
   * @param futureFitting The fitting to convert.
   * @tparam T The type of the [[Fitting]] value.
   * @return The resulting `Future[Option[T]]`.
   */
  implicit def futureFittingToFutureOption[T](futureFitting: Future[Fitting[T]]): Future[Option[T]] =
    futureFitting.flatMap {
      case SuccessfulFitting(v)             => Future.successful(Some(v))
      case FaultyFitting(NoneError)         => Future.successful(None)
      case FaultyFitting(e: ThrowableError) => Future.failed(e.value)
    }

  /**
   * Converts a `Future[Fitting[T]]` to a `Future[T]`.
   *
   * @param futureFitting The fitting to convert.
   * @tparam T The type of the [[Fitting]] value.
   * @return The resulting `Future[T]`.
   */
  implicit def futureFittingToFuture[T](futureFitting: Future[Fitting[T]]): Future[T] =
    futureFitting.flatMap {
      case SuccessfulFitting(v) => Future.successful(v)
      case FaultyFitting(error) => Future.failed(error.value)
    }

  /**
   * A future fitting is a special case which cannot be handled by a pure fitting because of the async handling
   * the `Future` types provides. So we add a `andThenFuture` method on `Future[Fitting[A]]` types to allow to
   * connect types of `Future[Fitting[A]]` to types of `Future[Fitting[B]]`.
   *
   * @param value The type to monkey patch.
   * @tparam A The type of the [[scala.concurrent.Future]].
   */
  implicit class FutureFitting[A](value: Future[Fitting[A]]) {
    def andThenFuture[B](reads: Reads[A, Future[B]]): Future[Fitting[B]] = value.flatMap {
      case SuccessfulFitting(v) => futureToFutureFitting(reads.read(v))
      case FaultyFitting(e)     => Future.successful(FaultyFitting[B](e))
    }.recover {
      case e => FaultyFitting[B](ThrowableError(e))
    }
    def andThenFuture[B](writes: Writes[A, Future[B]]): Future[Fitting[B]] = value.flatMap {
      case SuccessfulFitting(v) => futureToFutureFitting(writes.write(v))
      case FaultyFitting(e)     => Future.successful(FaultyFitting[B](e))
    }.recover {
      case e => FaultyFitting[B](ThrowableError(e))
    }
  }

  /**
   * Helper to connect a `Reads[A, Future[B]]` directly to a [[scala.concurrent.Future]].
   *
   * We already have an implicit converter that converts a `Future[A]` to a `Future[Fitting[A]]` and an implicit
   * class which adds a `andThenFuture` method to a `Future[Fitting[A]]`. But this cannot be applied to a `Future[A]`
   * alone, because Scala cannot chain implicits. So we do this manually here.
   *
   * @param value The type to monkey patch.
   * @tparam A The type of the [[scala.concurrent.Future]].
   */
  implicit class RichFuture[A](value: Future[A]) extends FutureFitting(futureToFutureFitting(value))
}
