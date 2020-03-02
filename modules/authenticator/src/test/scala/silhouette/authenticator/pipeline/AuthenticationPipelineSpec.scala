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

import cats.data.Validated._
import cats.data.{ NonEmptyList => NEL }
import cats.effect.IO
import cats.effect.IO._
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette._
import silhouette.authenticator._
import silhouette.authenticator.pipeline.Dsl._
import silhouette.http.transport.RetrieveFromCookie
import silhouette.http.{ Cookie, Fake }

/**
 * Test case for the [[AuthenticationPipeline]] class.
 */
class AuthenticationPipelineSpec extends Specification with Mockito {

  "The pipeline" should {
    "return the `MissingCredentials` state if no token was found in request" in new Context {
      val request = Fake.request

      pipeline(request).unsafeRunSync() must beEqualTo(MissingCredentials())
    }

    "return a custom state if no token was found in request and if a custom NoneError was defined" in new Context {
      val request = Fake.request
      override implicit val noneError: () => NoneError[User] =
        () => NoneError(AuthFailure(new RuntimeException("test")))

      pipeline(request).unsafeRunSync().toString must beEqualTo(noneError().state.toString)
    }

    "return the `AuthFailure` state if the token couldn't be transformed into an authenticator" in new Context {
      val exception = new AuthenticatorException("Parse error")
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns IO.raiseError(exception)

      pipeline(request).unsafeRunSync() must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `InvalidCredentials` state if the authenticator is invalid" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))
      val errors = NEL.of("Invalid authenticator")

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.pure(invalidNel(errors.head))

      pipeline(request).unsafeRunSync() must beEqualTo(InvalidCredentials(authenticator, errors))
    }

    "return the `AuthFailure` state if the validator throws an exception" in new Context {
      val exception = new AuthenticatorException("Validation error")
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.raiseError(exception)

      pipeline(request).unsafeRunSync() must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `MissingIdentity` state if the identity couldn't be found for the login info" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.pure(validNel(()))
      identityReader.apply(loginInfo) returns IO.pure(None)

      pipeline(request).unsafeRunSync() must beEqualTo(MissingIdentity(authenticator, loginInfo))
    }

    "return the `AuthFailure` state if the identity reader throws an exception" in new Context {
      val exception = new AuthenticatorException("Retrieval error")
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.pure(validNel(()))
      identityReader.apply(loginInfo) returns IO.raiseError(exception)

      pipeline(request).unsafeRunSync() must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `Authenticated` state if the authentication process was successful" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.pure(validNel(()))
      identityReader.apply(loginInfo) returns IO.pure(Some(user))

      pipeline(request).unsafeRunSync() must beEqualTo(Authenticated(user, authenticator, loginInfo))
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A test user.
     */
    case class User(loginInfo: LoginInfo) extends Identity

    /**
     * A non error that can be overridden by a test.
     */
    implicit val noneError: () => NoneError[User] = noneToMissingCredentials

    /**
     * Some authentication token.
     */
    val token = "some.authentication.token"

    /**
     * The login info.
     */
    val loginInfo = LoginInfo("credentials", "john@doe.com")

    /**
     * The authenticator representation of the claims object.
     */
    val authenticator = Authenticator(id = "id", loginInfo = loginInfo)

    /**
     * The identity implementation.
     */
    val user = User(loginInfo)

    /**
     * A reader function that transforms a string into an authenticator.
     */
    val authenticatorReader = mock[AuthenticatorReader[IO, String]]

    /**
     * The reader to retrieve the [[Identity]] for the [[LoginInfo]] stored in the
     * [[silhouette.authenticator.Authenticator]] from the persistence layer.
     */
    val identityReader = mock[LoginInfo => IO[Option[User]]].smart

    /**
     * A [[Validator]] to apply to the [[silhouette.authenticator.Authenticator]].
     */
    val validator = mock[Validator[IO]].smart

    /**
     * The pipeline to test.
     */
    lazy val pipeline = AuthenticationPipeline[IO, Fake.RequestPipeline, User](
      ~RetrieveFromCookie("test") >> authenticatorReader,
      identityReader,
      Set(validator)
    )
  }
}
