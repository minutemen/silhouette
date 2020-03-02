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

import cats.effect.IO._
import cats.effect.{ ContextShift, IO }
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import silhouette.authenticator.Authenticator
import silhouette.authenticator.pipeline.Dsl._
import silhouette.{ AuthFailure, Identity }

import scala.concurrent.Future
import scala.util.{ Success, Try }

/**
 * Test case for the [[Dsl]].
 *
 * @param ev The execution environment.
 */
class DslSpec(implicit ev: ExecutionEnv) extends Specification {

  "The DSL" should {
    "lift a function that returns a non-effectful value into a KleisliM" in {
      val f: String => String = (v: String) => v

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns an Option[String] into a KleisliM" in {
      val f: String => Option[String] = (v: String) => Some(v)

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns an Either[Throwable, String] into a KleisliM" in {
      val f: String => Either[Throwable, String] = (v: String) => Right(v)

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns a Try[String] into a KleisliM" in {
      val f: String => Try[String] = (v: String) => Success(v)

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns a Future[String] into a KleisliM" in {
      implicit val cs: ContextShift[IO] = IO.contextShift(ev.executionContext)
      val f: String => Future[String] = (v: String) => Future.successful(v)

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns a Future[Option[String]] into a KleisliM" in {
      implicit val cs: ContextShift[IO] = IO.contextShift(ev.executionContext)
      val f: String => Future[Option[String]] = (v: String) => Future.successful(Some(v))

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns a Future[Either[Throwable, String]] into a KleisliM" in {
      implicit val cs: ContextShift[IO] = IO.contextShift(ev.executionContext)
      val f: String => Future[Either[Throwable, String]] = (v: String) => Future.successful(Right(v))

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns a Future[Try[String]] into a KleisliM" in {
      implicit val cs: ContextShift[IO] = IO.contextShift(ev.executionContext)
      val f: String => Future[Try[String]] = (v: String) => Future.successful(Success(v))

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns a IO[String] into a KleisliM" in {
      val f: String => IO[String] = (v: String) => IO.pure(v)

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns a IO[Option[String]] into a KleisliM" in {
      val f: String => IO[Option[String]] = (v: String) => IO.pure(Some(v))

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns a IO[Either[Throwable, String]] into a KleisliM" in {
      val f: String => IO[Either[Throwable, String]] = (v: String) => IO.pure(Right(v))

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a function that returns a IO[Try[String]] into a KleisliM" in {
      val f: String => IO[Try[String]] = (v: String) => IO.pure(Success(v))

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beRight("test")
    }

    "lift a value into a KleisliM" in {
      KleisliM.liftV("test").run(()).value.unsafeRunSync() must beRight("test")
    }

    "Combine multiple KleisliM" in {
      implicit val cs: ContextShift[IO] = IO.contextShift(ev.executionContext)

      val f1: String => Option[String] = (v: String) => Some(v)
      val f2: String => IO[String] = (v: String) => IO.pure(v)
      val f3: String => Future[Either[Throwable, String]] = (v: String) => Future.successful(Right(v))

      (KleisliM.lift(f1) >> f2 >> f3).run("test").value.unsafeRunSync() must beRight("test")
    }

    "Use the unary ~ operator to lift a function into a KleisliM" in {
      implicit val cs: ContextShift[IO] = IO.contextShift(ev.executionContext)

      val f1: String => Option[String] = (v: String) => Some(v)
      val f2: String => IO[String] = (v: String) => IO.pure(v)
      val f3: String => Future[Either[Throwable, String]] = (v: String) => Future.successful(Right(v))

      (~f1 >> f2 >> f3).run("test").value.unsafeRunSync() must beRight("test")
    }

    "Use the unary xx function to lift a value into a KleisliM" in {
      implicit val cs: ContextShift[IO] = IO.contextShift(ev.executionContext)

      val f1: String => Option[String] = (v: String) => Some(v)
      val f2: String => IO[String] = (v: String) => IO.pure(v)
      val f3: String => Future[Either[Throwable, String]] = (v: String) => Future.successful(Right(v))

      (~f1 >> f2 >> f3 >> xx(1)).run("test").value.unsafeRunSync() must beRight(1)
    }

    "Use the default AuthState in case of a NoneError" in {
      val f: String => Option[String] = (_: String) => None

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beLeft(noneToMissingCredentials())
    }

    "Allow to override the NoneError with a user defined AuthState" in {
      case class User() extends Identity
      val authState = AuthFailure[User, Authenticator](new RuntimeException("test"))
      val f: String => Option[String] = (_: String) => None
      implicit val noneError = () => NoneError[User](authState)

      KleisliM.lift(f).run("test").value.unsafeRunSync() must beLeft(NoneError[User](authState))
    }
  }
}
