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

import cats.data.Validated._
import cats.data.{ NonEmptyList => NEL }
import cats.effect.IO
import cats.effect.IO._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.authenticator.pipeline.Dsl._
import silhouette.http.transport.{ CookieTransportConfig, EmbedIntoCookie, RetrieveFromCookie }
import silhouette.http.{ Cookie, Fake }
import silhouette.{ AuthFailure, AuthState, Authenticated, Identity, InvalidCredentials, LoginInfo, MissingCredentials, MissingIdentity }

/**
 * Test case for the [[AuthenticatorProvider]] class.
 */
class AuthenticatorProviderSpec extends Specification with Mockito {

  "The `authenticate` method" should {
    "return the `MissingCredentials` state if no token was found in request" in new Context {
      val request = Fake.request
      val response = Fake.response

      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(MissingCredentials())
    }

    "return the `AuthFailure` state if the token couldn't be transformed into an authenticator" in new Context {
      val exception = new AuthenticatorException("Parse error")
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReader(token) returns IO.raiseError(exception)
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `InvalidCredentials` state if the authenticator is invalid" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response
      val errors = NEL.of("Invalid authenticator")

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.pure(invalidNel(errors.head))
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(InvalidCredentials(authenticator, errors))
    }

    "return the `AuthFailure` state if the validator throws an exception" in new Context {
      val exception = new AuthenticatorException("Validation error")
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.raiseError(exception)
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `MissingIdentity` state if the identity couldn't be found for the login info" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.pure(validNel(()))
      identityReader.apply(loginInfo) returns IO.pure(None)
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(MissingIdentity(authenticator, loginInfo))
    }

    "return the `AuthFailure` state if the identity reader throws an exception" in new Context {
      val exception = new AuthenticatorException("Retrieval error")
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.pure(validNel(()))
      identityReader.apply(loginInfo) returns IO.raiseError(exception)
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `Authenticated` state if the authentication process was successful" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReader(token) returns IO.pure(authenticator)
      validator.isValid(authenticator) returns IO.pure(validNel(()))
      identityReader.apply(loginInfo) returns IO.pure(Some(user))
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(
        EmbedIntoCookie(CookieTransportConfig("test"))(response)(authenticator.toString)
      )

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(Authenticated(user, authenticator, loginInfo))
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
    val authenticatorReader: AuthenticatorReader[IO, String] = mock[AuthenticatorReader[IO, String]]

    /**
     * A writer function that transforms the [[Authenticator]] into a serialized form of the [[Authenticator]].
     */
    val authenticatorWriter = {
      val m = mock[AuthenticatorWriter[IO, String]]
      m.apply(authenticator) returns IO.pure(authenticator.toString)
      m
    }

    /**
     * The auth state handler.
     */
    val authStateHandler = mock[AuthState[User, Authenticator] => IO[Fake.ResponsePipeline]]

    /**
     * An argument captor for the auth state handler.
     */
    val authStateCaptor = capture[AuthState[User, Authenticator]]

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
     * The provider to test.
     */
    val provider = new AuthenticatorProvider[IO, Fake.Request, Fake.Response, User](
      AuthenticationPipeline[IO, Fake.RequestPipeline, User](
        ~RetrieveFromCookie("test") >> authenticatorReader,
        identityReader,
        Set(validator)
      ),
      TargetPipeline[IO, Fake.ResponsePipeline](target =>
        ~authenticatorWriter >> EmbedIntoCookie(CookieTransportConfig("test"))(target)
      )
    )
  }
}
