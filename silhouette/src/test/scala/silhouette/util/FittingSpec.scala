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
package silhouette.util

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.specs2.WaitPatience
import silhouette.util.Fitting._

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

/**
 * Test case for the [[Fitting]] trait.
 *
 * @param ev The execution environment.
 */
class FittingSpec(implicit ev: ExecutionEnv) extends Specification with WaitPatience {

  "A fitting" should {

    // *****************
    // Option - Reads
    // *****************

    "connect a `Some[A]` to a `Reads[A, Option[B]]`" in new Context {
      Some("test") andThenOption optionFormat.asReads must beLike {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }
    }

    "connect a `None` to a `Reads[A, Option[B]]`" in new Context {
      None andThenOption optionFormat.asReads must beLike {
        case FaultyFitting(e) =>
          e must be equalTo NoneError
      }
    }

    "connect a `Some[A]` to a `Reads[A, Try[B]]`" in new Context {
      Some("test") andThenTry tryFormat.asReads must beLike {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }
    }

    "connect a `None` to a `Reads[A, Try[B]]`" in new Context {
      None andThenTry tryFormat.asReads must beLike {
        case FaultyFitting(e) =>
          e must be equalTo NoneError
      }
    }

    "connect a `Some[A]` to a `Reads[A, Future[B]]`" in new Context {
      Some("test") andThenFuture futureFormat.asReads must beLike[Fitting[String]] {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }.awaitWithPatience
    }

    "connect a `None` to a `Reads[A, Future[B]]`" in new Context {
      None andThenFuture futureFormat.asReads must beLike[Fitting[String]] {
        case FaultyFitting(e) =>
          e must be equalTo NoneError
      }.awaitWithPatience
    }

    // *****************
    // Try - Reads
    // *****************

    "connect a `Success[A]` to a `Reads[A, Option[B]]`" in new Context {
      Success("test") andThenOption optionFormat.asReads must beLike {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }
    }

    "connect a `Failure` to a `Reads[A, Option[B]]`" in new Context {
      Failure(exception) andThenOption optionFormat.asReads must beLike {
        case FaultyFitting(e) =>
          e must be equalTo ThrowableError(exception)
      }
    }

    "connect a `Success[A]` to a `Reads[A, Try[B]]`" in new Context {
      Success("test") andThenTry tryFormat.asReads must beLike {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }
    }

    "connect a `Failure` to a `Reads[A, Try[B]]`" in new Context {
      Failure(exception) andThenTry tryFormat.asReads must beLike {
        case FaultyFitting(e) =>
          e must be equalTo ThrowableError(exception)
      }
    }

    "connect a `Success[A]` to a `Reads[A, Future[B]]`" in new Context {
      Success("test") andThenFuture futureFormat.asReads must beLike[Fitting[String]] {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }.awaitWithPatience
    }

    "connect a `Failure` to a `Reads[A, Future[B]]`" in new Context {
      Failure(exception) andThenFuture futureFormat.asReads must beLike[Fitting[String]] {
        case FaultyFitting(e) =>
          e must be equalTo ThrowableError(exception)
      }.awaitWithPatience
    }

    // *****************
    // Future - Reads
    // *****************

    "connect a `Future[A]` to a `Reads[A, Future[B]]`" in new Context {
      Future.successful("test") andThenFuture futureFormat.asReads must beLike[Fitting[String]] {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }.awaitWithPatience
    }

    "connect a failed `Future` to a `Reads[A, Future[B]]`" in new Context {
      Future.failed(exception) andThenFuture futureFormat.asReads must beLike[Fitting[_]] {
        case FaultyFitting(e) =>
          e must be equalTo ThrowableError(exception)
      }.awaitWithPatience
    }

    "connect a `Future[SuccessfulFitting[A]]` to a `Reads[A, Future[B]]`" in new Context {
      Future.successful(SuccessfulFitting("test")) andThenFuture futureFormat.asReads must beLike[Fitting[String]] {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }.awaitWithPatience
    }

    "connect a `Future[FaultyFitting[A]]` to a `Reads[A, Future[B]]`" in new Context {
      Future.successful(FaultyFitting[String](ThrowableError(exception)))
        .andThenFuture(futureFormat.asReads) must beLike[Fitting[_]] {
          case FaultyFitting(e) =>
            e must be equalTo ThrowableError(exception)
        }.awaitWithPatience
    }

    // *****************
    // Option - Writes
    // *****************

    "connect a `Some[A]` to a `Writes[A, Option[B]]`" in new Context {
      Some("test") andThenOption optionFormat.asWrites must beLike {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }
    }

    "connect a `None` to a `Writes[A, Option[B]]`" in new Context {
      None andThenOption optionFormat.asWrites must beLike {
        case FaultyFitting(e) =>
          e must be equalTo NoneError
      }
    }

    "connect a `Some[A]` to a `Writes[A, Try[B]]`" in new Context {
      Some("test") andThenTry tryFormat.asWrites must beLike {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }
    }

    "connect a `None` to a `Writes[A, Try[B]]`" in new Context {
      None andThenTry tryFormat.asWrites must beLike {
        case FaultyFitting(e) =>
          e must be equalTo NoneError
      }
    }

    "connect a `Some[A]` to a `Writes[A, Future[B]]`" in new Context {
      Some("test") andThenFuture futureFormat.asWrites must beLike[Fitting[String]] {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }.awaitWithPatience
    }

    "connect a `None` to a `Writes[A, Future[B]]`" in new Context {
      None andThenFuture futureFormat.asWrites must beLike[Fitting[String]] {
        case FaultyFitting(e) =>
          e must be equalTo NoneError
      }.awaitWithPatience
    }

    // *****************
    // Try - Writes
    // *****************

    "connect a `Success[A]` to a `Writes[A, Option[B]]`" in new Context {
      Success("test") andThenOption optionFormat.asWrites must beLike {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }
    }

    "connect a `Failure` to a `Writes[A, Option[B]]`" in new Context {
      Failure(exception) andThenOption optionFormat.asWrites must beLike {
        case FaultyFitting(e) =>
          e must be equalTo ThrowableError(exception)
      }
    }

    "connect a `Success[A]` to a `Writes[A, Try[B]]`" in new Context {
      Success("test") andThenTry tryFormat.asWrites must beLike {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }
    }

    "connect a `Failure` to a `Writes[A, Try[B]]`" in new Context {
      Failure(exception) andThenTry tryFormat.asWrites must beLike {
        case FaultyFitting(e) =>
          e must be equalTo ThrowableError(exception)
      }
    }

    "connect a `Success[A]` to a `Writes[A, Future[B]]`" in new Context {
      Success("test") andThenFuture futureFormat.asWrites must beLike[Fitting[String]] {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }.awaitWithPatience
    }

    "connect a `Failure` to a `Writes[A, Future[B]]`" in new Context {
      Failure(exception) andThenFuture futureFormat.asWrites must beLike[Fitting[String]] {
        case FaultyFitting(e) =>
          e must be equalTo ThrowableError(exception)
      }.awaitWithPatience
    }

    // *****************
    // Future - Writes
    // *****************

    "connect a `Future[A]` to a `Writes[A, Future[B]]`" in new Context {
      Future.successful("test") andThenFuture futureFormat.asWrites must beLike[Fitting[String]] {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }.awaitWithPatience
    }

    "connect a failed `Future` to a `Writes[A, Future[B]]`" in new Context {
      Future.failed(exception) andThenFuture futureFormat.asWrites must beLike[Fitting[_]] {
        case FaultyFitting(e) =>
          e must be equalTo ThrowableError(exception)
      }.awaitWithPatience
    }

    "connect a `Future[SuccessfulFitting[A]]` to a `Writes[A, Future[B]]`" in new Context {
      Future.successful(SuccessfulFitting("test")) andThenFuture futureFormat.asWrites must beLike[Fitting[String]] {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }.awaitWithPatience
    }

    "connect a `Future[FaultyFitting[A]]` to a `Writes[A, Future[B]]`" in new Context {
      Future.successful(FaultyFitting[String](ThrowableError(exception)))
        .andThenFuture(futureFormat.asWrites) must beLike[Fitting[_]] {
          case FaultyFitting(e) =>
            e must be equalTo ThrowableError(exception)
        }.awaitWithPatience
    }

    // *****************
    // Option - Fitting
    // *****************

    "convert a `Some[A]` to an `Option[A]`" in new Context {
      Some("test").toOption should beSome("test")
    }

    "convert a `None` to an `Option[A]`" in new Context {
      None.toOption should beNone
    }

    "convert a `Some[A]` to a `Try[A]`" in new Context {
      Some("test").toTry should beSuccessfulTry("test")
    }

    "convert a `None` to a `Try[A]`" in new Context {
      None.toTry should beFailedTry(NoneError.value)
    }

    "convert a `Some[A]` to a `Future[A]`" in new Context {
      Some("test").toFuture should beEqualTo("test").awaitWithPatience
    }

    "convert a `None` to a `Future[A]`" in new Context {
      None.toFuture should throwA(NoneError.value).awaitWithPatience
    }

    // *****************
    // Try - Fitting
    // *****************

    "convert a `Success[A]` to an `Option[A]`" in new Context {
      Success("test").toOption should beSome("test")
    }

    "convert a `Failure` to an `Option[A]`" in new Context {
      Failure(exception).toOption should beNone
    }

    "convert a `Success[A]` to a `Try[A]`" in new Context {
      Success("test").toTry should beSuccessfulTry("test")
    }

    "convert a `Failure` to a `Try[A]`" in new Context {
      Failure(exception).toTry should beFailedTry(exception)
    }

    "convert a `Success[A]` to a `Future[A]`" in new Context {
      Success("test").toFuture should beEqualTo("test").awaitWithPatience
    }

    "convert a `Failure` to a `Future[A]`" in new Context {
      Failure(exception).toFuture should throwA(exception).awaitWithPatience
    }

    // *****************
    // Future - Fitting
    // *****************

    "convert a `Future[A]` to a `Fitting[A]`" in new Context {
      futureToFutureFitting(Future.successful("test")) must beLike[Fitting[String]] {
        case SuccessfulFitting(v) =>
          v must be equalTo "test"
      }.awaitWithPatience
    }

    "convert a failed `Future` to a `Fitting[A]`" in new Context {
      futureToFutureFitting(Future.failed(exception)) must beLike[Fitting[_]] {
        case FaultyFitting(e) =>
          e must be equalTo ThrowableError(exception)
      }.awaitWithPatience
    }

    // *****************
    // Fitting -> T implicits
    // *****************

    "provide an implicit that converts a `Fitting[A]` into an `Option[A]`" in new Context {
      def apply[A](value: Fitting[A]): Option[A] = value

      apply(SuccessfulFitting("test")) must beSome("test")
    }

    "provide an implicit that converts a `Fitting[A]` into an `Try[A]`" in new Context {
      def apply[A](value: Fitting[A]): Try[A] = value

      apply(SuccessfulFitting("test")) must beSuccessfulTry("test")
    }

    "provide an implicit that converts a `Fitting[A]` into an `Future[A]`" in new Context {
      def apply[A](value: Fitting[A]): Future[A] = value

      apply(SuccessfulFitting("test")) must beEqualTo("test").awaitWithPatience
    }

    "provide an implicit that converts a `Future[SuccessfulFitting[A]]` into an `Future[Option[A]]`" in new Context {
      def apply[A](value: Future[Fitting[A]]): Future[Option[A]] = value

      apply(Future.successful(SuccessfulFitting("test"))) must beSome("test").awaitWithPatience
    }

    "provide an implicit that converts a `Future[FaultyFitting(NoneError)]` into an `Future[Option[A]]`" in
      new Context {
        def apply[A](value: Future[Fitting[A]]): Future[Option[A]] = value

        apply(Future.successful(FaultyFitting[String](NoneError))) must beNone.awaitWithPatience
      }

    "provide an implicit that converts a `Future[FaultyFitting(ThrowableError)]` into an `Future[Option[A]]`" in
      new Context {
        def apply[A](value: Future[Fitting[A]]): Future[Option[A]] = value

        apply(Future.successful(FaultyFitting[String](ThrowableError(exception)))) must throwA(exception)
          .awaitWithPatience
      }

    "provide an implicit that converts a `Future[SuccessfulFitting[A]]` into an `Future[A]`" in new Context {
      def apply[A](value: Future[Fitting[A]]): Future[A] = value

      apply(Future.successful(SuccessfulFitting("test"))) must beEqualTo("test").awaitWithPatience
    }

    "provide an implicit that converts a `Future[FaultyFitting(NoneError)]` into an `Future[A]`" in
      new Context {
        def apply[A](value: Future[Fitting[A]]): Future[A] = value

        apply(Future.successful(FaultyFitting[String](NoneError))) must throwA(NoneError.value)
          .awaitWithPatience
      }

    "provide an implicit that converts a `Future[FaultyFitting(ThrowableError)]` into an `Future[A]`" in
      new Context {
        def apply[A](value: Future[Fitting[A]]): Future[A] = value

        apply(Future.successful(FaultyFitting[String](ThrowableError(exception)))) must throwA(exception)
          .awaitWithPatience
      }
  }

  /**
   * The context.
   */
  trait Context extends Scope {
    class OptionFormat extends Reads[String, Option[String]] with Writes[String, Option[String]] {
      override def read(in: String): Option[String] = Some(in)
      override def write(in: String): Option[String] = Some(in)
    }
    class TryFormat extends Reads[String, Try[String]] with Writes[String, Try[String]] {
      override def read(in: String): Try[String] = Try(in)
      override def write(in: String): Try[String] = Try(in)
    }
    class FutureFormat extends Reads[String, Future[String]] with Writes[String, Future[String]] {
      override def read(in: String): Future[String] = Future.successful(in)
      override def write(in: String): Future[String] = Future.successful(in)
    }

    val optionFormat = new OptionFormat
    val tryFormat = new TryFormat
    val futureFormat = new FutureFormat

    val exception = new RuntimeException("test")
  }
}
