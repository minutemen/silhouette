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
import silhouette.crypto.Base64
import silhouette.http.{ BasicAuthorizationHeader, BasicCredentials, Fake, Header }
import silhouette.password.PasswordInfo
import silhouette.provider.password.PasswordProvider._
import silhouette.provider.password.PasswordProviderSpec
import silhouette.specs2.WaitPatience
import silhouette.{ ConfigurationException, Done, LoginInfo }

import scala.concurrent.Future

/**
 * Test case for the [[BasicAuthProvider]] class.
 *
 * @param ev The execution environment.
 */
class BasicAuthProviderSpec(implicit ev: ExecutionEnv) extends PasswordProviderSpec with WaitPatience {

  "The `authenticate` method" should {
    "throw ConfigurationException if unsupported hasher is stored" in new Context {
      val passwordInfo = PasswordInfo("unknown", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.username)
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))

      provider.authenticate(request) must throwA[ConfigurationException].like {
        case e => e.getMessage must beEqualTo(HasherIsNotRegistered.format(provider.id, "unknown", "foo, bar"))
      }.awaitWithPatience
    }

    "return None if no auth info could be found for the given credentials" in new Context {
      val loginInfo = new LoginInfo(provider.id, credentials.username)
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      authInfoReader.apply(loginInfo) returns Future.successful(None)

      provider.authenticate(request) must beNone.awaitWithPatience
    }

    "return None if password does not match" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.username)
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      fooHasher.matches(passwordInfo, credentials.password) returns false
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))

      provider.authenticate(request) must beNone.awaitWithPatience
    }

    "return None if provider isn't responsible" in new Context {
      provider.authenticate(Fake.request) must beNone.awaitWithPatience
    }

    "return None for wrong encoded credentials" in new Context {
      val request = Fake.request.withHeaders(Header(Header.Name.Authorization, "wrong"))

      provider.authenticate(request) must beNone.awaitWithPatience
    }

    "return login info if passwords does match" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.username)
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))

      provider.authenticate(request) must beSome(loginInfo).awaitWithPatience
    }

    "handle a colon in a password" in new Context {
      val credentialsWithColon = BasicCredentials("apollonia.vanova@minutemen.group", "s3c:r3t")
      val passwordInfo = PasswordInfo("foo", "hashed(s3c:r3t)")
      val loginInfo = LoginInfo(provider.id, credentialsWithColon.username)
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentialsWithColon))

      fooHasher.matches(passwordInfo, credentialsWithColon.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))

      provider.authenticate(request) must beSome(loginInfo).awaitWithPatience
    }

    "re-hash password with new hasher if hasher is deprecated" in new Context {
      val passwordInfo = PasswordInfo("bar", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.username)
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      fooHasher.hash(credentials.password) returns passwordInfo
      barHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))
      authInfoWriter.apply(loginInfo, passwordInfo) returns Future.successful(Done)

      provider.authenticate(request) must beSome(loginInfo).awaitWithPatience
      there was one(authInfoWriter).apply(loginInfo, passwordInfo)
    }

    "re-hash password with new hasher if password info is deprecated" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.username)
      val request = Fake.request.withHeaders(BasicAuthorizationHeader(credentials))

      fooHasher.isDeprecated(passwordInfo) returns Some(true)
      fooHasher.hash(credentials.password) returns passwordInfo
      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))
      authInfoWriter.apply(loginInfo, passwordInfo) returns Future.successful(Done)

      provider.authenticate(request) must beSome(loginInfo).awaitWithPatience
      there was one(authInfoWriter).apply(loginInfo, passwordInfo)
    }

    "return None if Authorization method is not Basic and Base64 decoded header has ':'" in new Context {
      val request = Fake.request.withHeaders(
        Header(Header.Name.Authorization, Base64.encode("NotBasic foo:bar"))
      )

      provider.authenticate(request) must beNone.awaitWithPatience
    }
  }

  /**
   * The context.
   */
  trait Context extends BaseContext {

    /**
     * The test credentials.
     */
    lazy val credentials = BasicCredentials("apollonia.vanova@minutemen.group", "s3cr3t")

    /**
     * The provider to test.
     */
    lazy val provider = new BasicAuthProvider(authInfoReader, authInfoWriter, passwordHasherRegistry)
  }
}
