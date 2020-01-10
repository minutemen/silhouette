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
import cats.effect.SyncIO
import cats.effect.SyncIO._
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.authenticator._
import silhouette.authenticator.pipeline.Dsl._
import silhouette.http.transport.RetrieveFromCookie
import silhouette.http.{ Cookie, Fake }
import silhouette.{ AuthFailure, AuthState, Authenticated, Identity, InvalidCredentials, LoginInfo, MissingCredentials, MissingIdentity }

/**
 * Test case for the [[AuthenticationPipeline]] class.
 */
class AuthenticationPipelineSpec extends Specification with Mockito {

  "The `read` method" should {
    "return the `MissingCredentials` state if no token was found in request" in new Context {
      val request = Fake.request

      pipeline(request).unsafeRunSync() must beEqualTo(MissingCredentials())
    }

    "return the `AuthFailure` state if the token couldn't be transformed into an authenticator" in new Context {
      val exception = new AuthenticatorException("Parse error")
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns SyncIO.raiseError(exception)

      pipeline(request).unsafeRunSync() must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `InvalidCredentials` state if the authenticator is invalid" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))
      val errors = NEL.of("Invalid authenticator")

      authenticatorReader(token) returns SyncIO.pure(authenticator)
      validator.isValid(authenticator) returns SyncIO.pure(invalidNel(errors.head))

      pipeline(request).unsafeRunSync() must beEqualTo(InvalidCredentials(authenticator, errors))
    }

    "return the `AuthFailure` state if the validator throws an exception" in new Context {
      val exception = new AuthenticatorException("Validation error")
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns SyncIO.pure(authenticator)
      validator.isValid(authenticator) returns SyncIO.raiseError(exception)

      pipeline(request).unsafeRunSync() must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `MissingIdentity` state if the identity couldn't be found for the login info" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns SyncIO.pure(authenticator)
      validator.isValid(authenticator) returns SyncIO.pure(validNel(()))
      identityReader.apply(loginInfo) returns SyncIO.pure(None)

      pipeline(request).unsafeRunSync() must beEqualTo(MissingIdentity(authenticator, loginInfo))
    }

    "return the `AuthFailure` state if the identity reader throws an exception" in new Context {
      val exception = new AuthenticatorException("Retrieval error")
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns SyncIO.pure(authenticator)
      validator.isValid(authenticator) returns SyncIO.pure(validNel(()))
      identityReader.apply(loginInfo) returns SyncIO.raiseError(exception)

      pipeline(request).unsafeRunSync() must beLike[AuthState[User, Authenticator]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo exception.getMessage
      }
    }

    "return the `Authenticated` state if the authentication process was successful" in new Context {
      val request = Fake.request.withCookies(Cookie("test", token))

      authenticatorReader(token) returns SyncIO.pure(authenticator)
      validator.isValid(authenticator) returns SyncIO.pure(validNel(()))
      identityReader.apply(loginInfo) returns SyncIO.pure(Some(user))

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
    val authenticatorReader = mock[AuthenticatorReader[SyncIO, String]]

    /**
     * The reader to retrieve the [[Identity]] for the [[LoginInfo]] stored in the
     * [[silhouette.authenticator.Authenticator]] from the persistence layer.
     */
    val identityReader = mock[LoginInfo => SyncIO[Option[User]]].smart

    /**
     * A [[Validator]] to apply to the [[silhouette.authenticator.Authenticator]].
     */
    val validator = mock[Validator[SyncIO]].smart

    /**
     * The pipeline to test.
     */
    val pipeline = AuthenticationPipeline[SyncIO, Fake.RequestPipeline, User](
      |>(RetrieveFromCookie("test")) >> authenticatorReader,
      identityReader,
      Set(validator)
    )
  }
}
