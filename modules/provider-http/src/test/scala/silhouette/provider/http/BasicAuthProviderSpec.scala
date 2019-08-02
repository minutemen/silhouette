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

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import silhouette._
import silhouette.crypto.Base64
import silhouette.http._
import silhouette.password.PasswordInfo
import silhouette.provider.password.PasswordProvider._
import silhouette.provider.password.PasswordProviderSpec
import silhouette.specs2.WaitPatience

import scala.concurrent.Future

/**
 * Test case for the [[BasicAuthProvider]] class.
 *
 * @param ev The execution environment.
 */
class BasicAuthProviderSpec(implicit ev: ExecutionEnv) extends PasswordProviderSpec with Mockito with WaitPatience {

  "The `authenticate` method" should {
    "return the `AuthFailure` state if a unsupported hasher was stored" in new Context {
      val passwordInfo = PasswordInfo("unknown", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))

      provider.authenticate(request) must beLike[AuthState[User, BasicCredentials]] {
        case AuthFailure(e) =>
          e.getMessage must be equalTo HasherIsNotRegistered.format(provider.id, "unknown", "foo, bar")
      }.awaitWithPatience
    }

    "return the `InvalidCredentials` state if no auth info could be found for the given credentials" in new Context {
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      authInfoReader.apply(loginInfo) returns Future.successful(None)

      provider.authenticate(request) must beEqualTo(
        InvalidCredentials(credentials, Seq(PasswordInfoNotFound.format(provider.id, loginInfo)))
      ).awaitWithPatience
    }

    "return the `InvalidCredentials` state if password does not match" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      fooHasher.matches(passwordInfo, credentials.password) returns false
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))

      provider.authenticate(request) must beEqualTo(
        InvalidCredentials(credentials, Seq(PasswordDoesNotMatch.format(provider.id)))
      ).awaitWithPatience
    }

    "return the `MissingCredentials` state if provider isn't responsible" in new Context {
      provider.authenticate(Fake.request) must beEqualTo(MissingCredentials).awaitWithPatience
    }

    "return the `MissingCredentials` state for wrong encoded credentials" in new Context {
      val request = Fake.request.withHeaders(Header(Header.Name.Authorization, "wrong"))

      provider.authenticate(request) must beEqualTo(MissingCredentials).awaitWithPatience
    }

    "return the `MissingIdentity` state if no identity could be found" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))
      identityReader.apply(loginInfo) returns Future.successful(None)

      provider.authenticate(request) must beEqualTo(MissingIdentity(credentials, loginInfo)).awaitWithPatience
    }

    "return the `Authenticated` state if passwords does match" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))
      identityReader.apply(loginInfo) returns Future.successful(Some(user))

      provider.authenticate(request) must beEqualTo(Authenticated(user, credentials, loginInfo)).awaitWithPatience
    }

    "handle a colon in a password" in new Context {
      val credentialsWithColon = BasicCredentials(credentials.username, "s3c:r3t")
      val passwordInfo = PasswordInfo("foo", "hashed(s3c:r3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentialsWithColon))

      fooHasher.matches(passwordInfo, credentialsWithColon.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))
      identityReader.apply(loginInfo) returns Future.successful(Some(user))

      provider.authenticate(request) must beEqualTo(
        Authenticated(user, credentialsWithColon, loginInfo)
      ).awaitWithPatience
    }

    "re-hash password with new hasher if hasher is deprecated" in new Context {
      val passwordInfo = PasswordInfo("bar", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      fooHasher.hash(credentials.password) returns passwordInfo
      barHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))
      authInfoWriter.apply(loginInfo, passwordInfo) returns Future.successful(Done)
      identityReader.apply(loginInfo) returns Future.successful(Some(user))

      provider.authenticate(request) must beEqualTo(Authenticated(user, credentials, loginInfo)).awaitWithPatience
      there was one(authInfoWriter).apply(loginInfo, passwordInfo)
    }

    "re-hash password with new hasher if password info is deprecated" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      fooHasher.isDeprecated(passwordInfo) returns Some(true)
      fooHasher.hash(credentials.password) returns passwordInfo
      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))
      authInfoWriter.apply(loginInfo, passwordInfo) returns Future.successful(Done)
      identityReader.apply(loginInfo) returns Future.successful(Some(user))

      provider.authenticate(request) must beEqualTo(Authenticated(user, credentials, loginInfo)).awaitWithPatience
      there was one(authInfoWriter).apply(loginInfo, passwordInfo)
    }

    "return the `MissingCredentials` state if Authorization method is not Basic and Base64 decoded header has ':'" in
      new Context {
        val request = Fake.request.withHeaders(
          Header(Header.Name.Authorization, Base64.encode("NotBasic foo:bar"))
        )

        provider.authenticate(request) must beEqualTo(MissingCredentials).awaitWithPatience
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
    val identityReader = mock[LoginInfo => Future[Option[User]]].smart

    /**
     * The provider to test.
     */
    val provider = new BasicAuthProvider[SilhouetteRequest, User](
      authInfoReader, authInfoWriter, identityReader, passwordHasherRegistry
    )
  }
}
