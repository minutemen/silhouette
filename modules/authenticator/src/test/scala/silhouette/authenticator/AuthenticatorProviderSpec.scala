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

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.authenticator.Validator.{ Invalid, Valid }
import silhouette.http.{ Cookie, Fake, SilhouetteRequest, SilhouetteResponse }
import silhouette.specs2.WaitPatience
import silhouette.{ Reads => _, Writes => _, _ }

import scala.concurrent.Future

/**
 * Test case for the [[AuthenticatorProvider]] class.
 *
 * @param ev The execution environment.
 */
class AuthenticatorProviderSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `authenticate` method" should {
    "return the `MissingCredentials` state if no token was found in request" in new Context {
      val request = Fake.request
      val response = Fake.response

      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns Future.successful(response)

      provider.authenticate(request)(authStateHandler) must beEqualTo(response).awaitWithPatience

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(MissingCredentials())
    }

    "return the `AuthFailure` state if the token couldn't be transformed into an authenticator" in new Context {
      val exception = new AuthenticatorException("Parse error")
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReads.read(token) returns Future.failed(exception)
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns Future.successful(response)

      provider.authenticate(request)(authStateHandler) must beEqualTo(response).awaitWithPatience

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `InvalidCredentials` state if the authenticator is invalid" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response
      val errors = Seq("Invalid authenticator")

      authenticatorReads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.successful(Invalid(errors))
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns Future.successful(response)

      provider.authenticate(request)(authStateHandler) must beEqualTo(response).awaitWithPatience

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(InvalidCredentials(authenticator, errors))
    }

    "return the `AuthFailure` state if the validator throws an exception" in new Context {
      val exception = new AuthenticatorException("Validation error")
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.failed(exception)
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns Future.successful(response)

      provider.authenticate(request)(authStateHandler) must beEqualTo(response).awaitWithPatience

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `MissingIdentity` state if the identity couldn't be found for the login info" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.successful(Valid)
      identityReader.apply(loginInfo) returns Future.successful(None)
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns Future.successful(response)

      provider.authenticate(request)(authStateHandler) must beEqualTo(response).awaitWithPatience

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(MissingIdentity(authenticator, loginInfo))
    }

    "return the `AuthFailure` state if the identity reader throws an exception" in new Context {
      val exception = new AuthenticatorException("Retrieval error")
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.successful(Valid)
      identityReader.apply(loginInfo) returns Future.failed(exception)
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns Future.successful(response)

      provider.authenticate(request)(authStateHandler) must beEqualTo(response).awaitWithPatience

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `Authenticated` state if the authentication process was successful" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))
      val response = Fake.response

      authenticatorReads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.successful(Valid)
      identityReader.apply(loginInfo) returns Future.successful(Some(user))
      authStateHandler.apply(any[AuthState[User, Authenticator]]()) returns Future.successful(response)

      provider.authenticate(request)(authStateHandler) must beEqualTo(response).awaitWithPatience

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
     * The reads which transforms a string into an authenticator.
     */
    val authenticatorReads = mock[Reads[Future, String]]

    /**
     * A writes that transforms the [[Authenticator]] into a serialized form of the [[Authenticator]].
     */
    val authenticatorWrites = {
      val m = mock[Writes[Future, String]]
      m.write(authenticator) returns Future.successful(authenticator.toString)
      m
    }

    /**
     * The auth state handler.
     */
    val authStateHandler = mock[AuthState[User, Authenticator] => Future[Fake.Response]]

    /**
     * An argument captor for the auth state handler.
     */
    val authStateCaptor = capture[AuthState[User, Authenticator]]

    /**
     * The reader to retrieve the [[Identity]] for the [[LoginInfo]] stored in the
     * [[silhouette.authenticator.Authenticator]] from the persistence layer.
     */
    val identityReader = mock[LoginInfo => Future[Option[User]]].smart

    /**
     * A [[Validator]] to apply to the [[silhouette.authenticator.Authenticator]].
     */
    val validator = mock[Validator].smart

    /**
     * The provider to test.
     */
    val provider = new AuthenticatorProvider[SilhouetteRequest, SilhouetteResponse, User](
      AuthenticationPipeline(
        _ => Future.successful(Some(Authenticator("test", LoginInfo("", "")))),
        identityReader, Set(validator)
      ),
      TargetPipeline(_ => _ => Future.successful(Fake.response))
    )
  }
}
