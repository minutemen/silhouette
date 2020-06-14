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
package silhouette.provider.http

import cats.data.{ NonEmptyList => NEL }
import cats.effect.IO
import org.specs2.mock.Mockito
import silhouette._
import silhouette.crypto.Base64
import silhouette.http._
import silhouette.password.PasswordInfo
import silhouette.provider.password.PasswordProvider._
import silhouette.provider.password.PasswordProviderSpec
import sttp.model.Header

/**
 * Test case for the [[BasicAuthProvider]] class.
 */
class BasicAuthProviderSpec extends PasswordProviderSpec with Mockito {

  "The `authenticate` method" should {
    "return the `AuthFailure` state if a unsupported hasher was stored" in new Context {
      val passwordInfo = PasswordInfo("unknown", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))
      val response = Fake.response

      authInfoReader.apply(loginInfo) returns IO.pure(Some(passwordInfo))
      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beLike[AuthState[User, BasicCredentials]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo HasherIsNotRegistered.format(provider.id, "unknown", "foo, bar")
      }
    }

    "return the `InvalidCredentials` state if no auth info could be found for the given credentials" in new Context {
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))
      val response = Fake.response

      authInfoReader.apply(loginInfo) returns IO.pure(None)
      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(
        InvalidCredentials(credentials, NEL.of(PasswordInfoNotFound.format(provider.id, loginInfo)))
      )
    }

    "return the `InvalidCredentials` state if password does not match" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))
      val response = Fake.response

      fooHasher.matches(passwordInfo, credentials.password) returns false
      authInfoReader.apply(loginInfo) returns IO.pure(Some(passwordInfo))
      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(
        InvalidCredentials(credentials, NEL.of(PasswordDoesNotMatch.format(provider.id)))
      )
    }

    "return the `MissingCredentials` state if provider isn't responsible" in new Context {
      val request = Fake.request
      val response = Fake.response
      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(MissingCredentials())
    }

    "return the `MissingCredentials` state for wrong encoded credentials" in new Context {
      val request = Fake.request.withHeaders(Header.authorization("wrong", "wrong"))
      val response = Fake.response

      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(MissingCredentials())
    }

    "return the `MissingIdentity` state if no identity could be found" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))
      val response = Fake.response

      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns IO.pure(Some(passwordInfo))
      identityReader.apply(loginInfo) returns IO.pure(None)
      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(MissingIdentity(credentials, loginInfo))
    }

    "return the `Authenticated` state if passwords does match" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))
      val response = Fake.response

      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns IO.pure(Some(passwordInfo))
      identityReader.apply(loginInfo) returns IO.pure(Some(user))
      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(Authenticated(user, credentials, loginInfo))
    }

    "handle a colon in a password" in new Context {
      val credentialsWithColon = BasicCredentials(credentials.username, "s3c:r3t")
      val passwordInfo = PasswordInfo("foo", "hashed(s3c:r3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentialsWithColon))
      val response = Fake.response

      fooHasher.matches(passwordInfo, credentialsWithColon.password) returns true
      authInfoReader.apply(loginInfo) returns IO.pure(Some(passwordInfo))
      identityReader.apply(loginInfo) returns IO.pure(Some(user))
      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)

      authStateCaptor.value must beEqualTo(
        Authenticated(user, credentialsWithColon, loginInfo)
      )
    }

    "re-hash password with new hasher if hasher is deprecated" in new Context {
      val passwordInfo = PasswordInfo("bar", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))
      val response = Fake.response

      fooHasher.hash(credentials.password) returns passwordInfo
      barHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns IO.pure(Some(passwordInfo))
      authInfoWriter.apply(loginInfo, passwordInfo) returns IO.pure(Done)
      identityReader.apply(loginInfo) returns IO.pure(Some(user))
      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)
      there was one(authInfoWriter).apply(loginInfo, passwordInfo)

      authStateCaptor.value must beEqualTo(Authenticated(user, credentials, loginInfo))
    }

    "re-hash password with new hasher if password info is deprecated" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))
      val response = Fake.response

      fooHasher.isDeprecated(passwordInfo) returns Some(true)
      fooHasher.hash(credentials.password) returns passwordInfo
      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns IO.pure(Some(passwordInfo))
      authInfoWriter.apply(loginInfo, passwordInfo) returns IO.pure(Done)
      identityReader.apply(loginInfo) returns IO.pure(Some(user))
      authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

      provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

      there was one(authStateHandler).apply(authStateCaptor)
      there was one(authInfoWriter).apply(loginInfo, passwordInfo)

      authStateCaptor.value must beEqualTo(Authenticated(user, credentials, loginInfo))
    }

    "return the `MissingCredentials` state if Authorization method is not Basic and Base64 decoded header has ':'" in
      new Context {
        val request = Fake.request.withHeaders(
          Header.authorization("NotBasic", Base64.encode("foo:bar"))
        )
        val response = Fake.response

        authStateHandler.apply(any[AuthState[User, BasicCredentials]]()) returns IO.pure(response)

        provider.authenticate(request)(authStateHandler).unsafeRunSync() must beEqualTo(response)

        there was one(authStateHandler).apply(authStateCaptor)

        authStateCaptor.value must beEqualTo(MissingCredentials())
      }
  }

  /**
   * The context.
   */
  trait Context extends BaseContext {

    /**
     * A test user.
     */
    case class User(loginInfo: LoginInfo) extends Identity

    /**
     * The test credentials.
     */
    val credentials = BasicCredentials("apollonia.vanova@minutemen.group", "s3cr3t")

    /**
     * The login info.
     */
    val loginInfo = LoginInfo(BasicAuthProvider.ID, credentials.username)

    /**
     * The identity implementation.
     */
    val user = User(loginInfo)

    /**
     * The reader to retrieve the [[Identity]] for the [[LoginInfo]] from the persistence layer.
     */
    val identityReader = mock[LoginInfo => IO[Option[User]]].smart

    /**
     * The auth state handler.
     */
    val authStateHandler = mock[AuthState[User, BasicCredentials] => IO[Fake.ResponsePipeline]]

    /**
     * An argument captor for the auth state handler.
     */
    val authStateCaptor = capture[AuthState[User, BasicCredentials]]

    /**
     * The provider to test.
     */
    val provider = new BasicAuthProvider[IO, SilhouetteRequest, SilhouetteResponse, User](
      authInfoReader, authInfoWriter, identityReader, passwordHasherRegistry
    )
  }
}
