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
package silhouette.provider.credentials

import org.specs2.concurrent.ExecutionEnv
import silhouette.password.PasswordInfo
import silhouette.provider.IdentityNotFoundException
import silhouette.provider.password.PasswordProvider._
import silhouette.provider.password.{ InvalidPasswordException, PasswordProviderSpec }
import silhouette.specs2.WaitPatience
import silhouette.{ ConfigurationException, Credentials, Done, LoginInfo }

import scala.concurrent.Future

/**
 * Test case for the [[CredentialsProvider]] class.
 *
 * @param ev The execution environment.
 */
class CredentialsProviderSpec(implicit ev: ExecutionEnv) extends PasswordProviderSpec with WaitPatience {

  "The `authenticate` method" should {
    "throw IdentityNotFoundException if no auth info could be found for the given credentials" in new Context {
      val loginInfo = LoginInfo(provider.id, credentials.identifier)

      authInfoReader.apply(loginInfo) returns Future.successful(None)

      provider.authenticate(credentials) must throwA[IdentityNotFoundException].like {
        case e => e.getMessage must beEqualTo(PasswordInfoNotFound.format(provider.id, loginInfo))
      }.awaitWithPatience
    }

    "throw InvalidPasswordException if password does not match" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.identifier)

      fooHasher.matches(passwordInfo, credentials.password) returns false
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))

      provider.authenticate(credentials) must throwA[InvalidPasswordException].like {
        case e => e.getMessage must beEqualTo(PasswordDoesNotMatch.format(provider.id))
      }.awaitWithPatience
    }

    "throw ConfigurationException if unsupported hasher is stored" in new Context {
      val passwordInfo = PasswordInfo("unknown", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.identifier)

      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))

      provider.authenticate(credentials) must throwA[ConfigurationException].like {
        case e => e.getMessage must beEqualTo(HasherIsNotRegistered.format(provider.id, "unknown", "foo, bar"))
      }.awaitWithPatience
    }

    "return login info if passwords does match" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.identifier)

      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))

      provider.authenticate(credentials) must beEqualTo(loginInfo).awaitWithPatience
    }

    "re-hash password with new hasher if hasher is deprecated" in new Context {
      val passwordInfo = PasswordInfo("bar", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.identifier)

      fooHasher.hash(credentials.password) returns passwordInfo
      barHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))
      authInfoWriter.apply(loginInfo, passwordInfo) returns Future.successful(Done)

      provider.authenticate(credentials) must beEqualTo(loginInfo).awaitWithPatience
      there was one(authInfoWriter).apply(loginInfo, passwordInfo)
    }

    "re-hash password with new hasher if hasher is deprecated" in new Context {
      val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
      val loginInfo = LoginInfo(provider.id, credentials.identifier)

      fooHasher.isDeprecated(passwordInfo) returns Some(true)
      fooHasher.hash(credentials.password) returns passwordInfo
      fooHasher.matches(passwordInfo, credentials.password) returns true
      authInfoReader.apply(loginInfo) returns Future.successful(Some(passwordInfo))
      authInfoWriter.apply(loginInfo, passwordInfo) returns Future.successful(Done)

      provider.authenticate(credentials) must beEqualTo(loginInfo).awaitWithPatience
      there was one(authInfoWriter).apply(loginInfo, passwordInfo)
    }
  }

  /**
   * The context.
   */
  trait Context extends BaseContext {

    /**
     * The test credentials.
     */
    lazy val credentials = Credentials("apollonia.vanova@minutemen.group", "s3cr3t")

    /**
     * The provider to test.
     */
    lazy val provider = new CredentialsProvider(authInfoReader, authInfoWriter, passwordHasherRegistry)
  }
}
