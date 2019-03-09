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
package silhouette.persistence.repositories

import org.specs2.concurrent.ExecutionEnv
import org.specs2.control.NoLanguageFeatures
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.password.PasswordInfo
import silhouette.persistence.daos.InMemoryAuthInfoDAO
import silhouette.persistence.repositories.DelegableAuthInfoRepository._
import silhouette.specs2.WaitPatience
import silhouette.{ AuthInfo, ConfigurationException, LoginInfo }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Test case for the [[DelegableAuthInfoRepository]] trait.
 *
 * @param ev The execution environment.
 */
class DelegableAuthInfoRepositorySpec(implicit ev: ExecutionEnv)
  extends Specification with Mockito with NoLanguageFeatures with WaitPatience {

  "The `find` method" should {
    "delegate the PasswordInfo to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      Await.result(passwordInfoDAO.add(loginInfo, passwordInfo), 10 seconds)

      service.find[PasswordInfo](loginInfo) must beSome(passwordInfo).awaitWithPatience
      there was one(passwordInfoDAO).find(loginInfo)
    }

    "delegate the OAuth1Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      Await.result(oauth1InfoDAO.add(loginInfo, oauth1Info), 10 seconds)

      service.find[OAuth1Info](loginInfo) must beSome(oauth1Info).awaitWithPatience
      there was one(oauth1InfoDAO).find(loginInfo)
    }

    "delegate the OAuth2Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      Await.result(oauth2InfoDAO.add(loginInfo, oauth2Info), 10 seconds)

      service.find[OAuth2Info](loginInfo) must beSome(oauth2Info).awaitWithPatience
      there was one(oauth2InfoDAO).find(loginInfo)
    }

    "throw a ConfigurationException if an unsupported type was given" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.find[UnsupportedInfo](loginInfo) must throwA[ConfigurationException].like {
        case e => e.getMessage must startWith(FindError.format(classOf[UnsupportedInfo]))
      }.awaitWithPatience
    }
  }

  "The `add` method" should {
    "delegate the PasswordInfo to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.add(loginInfo, passwordInfo) must beEqualTo(passwordInfo).awaitWithPatience
      there was one(passwordInfoDAO).add(loginInfo, passwordInfo)
    }

    "delegate the OAuth1Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.add(loginInfo, oauth1Info) must beEqualTo(oauth1Info).awaitWithPatience
      there was one(oauth1InfoDAO).add(loginInfo, oauth1Info)
    }

    "delegate the OAuth2Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.add(loginInfo, oauth2Info) must beEqualTo(oauth2Info).awaitWithPatience
      there was one(oauth2InfoDAO).add(loginInfo, oauth2Info)
    }

    "throw a ConfigurationException if an unsupported type was given" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.add(loginInfo, new UnsupportedInfo) must throwA[ConfigurationException].like {
        case e => e.getMessage must startWith(AddError.format(classOf[UnsupportedInfo]))
      }.awaitWithPatience
    }
  }

  "The `update` method" should {
    "delegate the PasswordInfo to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.update(loginInfo, passwordInfo) must beEqualTo(passwordInfo).awaitWithPatience
      there was one(passwordInfoDAO).update(loginInfo, passwordInfo)
    }

    "delegate the OAuth1Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.update(loginInfo, oauth1Info) must beEqualTo(oauth1Info).awaitWithPatience
      there was one(oauth1InfoDAO).update(loginInfo, oauth1Info)
    }

    "delegate the OAuth2Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.update(loginInfo, oauth2Info) must beEqualTo(oauth2Info).awaitWithPatience
      there was one(oauth2InfoDAO).update(loginInfo, oauth2Info)
    }

    "throw a ConfigurationException if an unsupported type was given" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.update(loginInfo, new UnsupportedInfo) must throwA[ConfigurationException].like {
        case e => e.getMessage must startWith(UpdateError.format(classOf[UnsupportedInfo]))
      }.awaitWithPatience
    }
  }

  "The `save` method" should {
    "delegate the PasswordInfo to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.save(loginInfo, passwordInfo) must beEqualTo(passwordInfo).awaitWithPatience
      there was one(passwordInfoDAO).save(loginInfo, passwordInfo)
    }

    "delegate the OAuth1Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.save(loginInfo, oauth1Info) must beEqualTo(oauth1Info).awaitWithPatience
      there was one(oauth1InfoDAO).save(loginInfo, oauth1Info)
    }

    "delegate the OAuth2Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.save(loginInfo, oauth2Info) must beEqualTo(oauth2Info).awaitWithPatience
      there was one(oauth2InfoDAO).save(loginInfo, oauth2Info)
    }

    "throw a ConfigurationException if an unsupported type was given" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.save(loginInfo, new UnsupportedInfo) must throwA[ConfigurationException].like {
        case e => e.getMessage must startWith(SaveError.format(classOf[UnsupportedInfo]))
      }.awaitWithPatience
    }
  }

  "The `remove` method" should {
    "delegate the PasswordInfo to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      Await.result(passwordInfoDAO.add(loginInfo, passwordInfo), 10 seconds)

      service.remove[PasswordInfo](loginInfo) must beEqualTo(()).awaitWithPatience
      there was one(passwordInfoDAO).remove(loginInfo)
    }

    "delegate the OAuth1Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      Await.result(oauth1InfoDAO.add(loginInfo, oauth1Info), 10 seconds)

      service.remove[OAuth1Info](loginInfo) must beEqualTo(()).awaitWithPatience
      there was one(oauth1InfoDAO).remove(loginInfo)
    }

    "delegate the OAuth2Info to the correct DAO" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      Await.result(oauth2InfoDAO.add(loginInfo, oauth2Info), 10 seconds)

      service.remove[OAuth2Info](loginInfo) must beEqualTo(()).awaitWithPatience
      there was one(oauth2InfoDAO).remove(loginInfo)
    }

    "throw a ConfigurationException if an unsupported type was given" in new Context {
      val loginInfo = LoginInfo("credentials", "1")

      service.remove[UnsupportedInfo](loginInfo) must throwA[ConfigurationException].like {
        case e => e.getMessage must startWith(RemoveError.format(classOf[UnsupportedInfo]))
      }.awaitWithPatience
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * Some auth info types.
     */
    class UnsupportedInfo extends AuthInfo
    class OAuth1Info extends AuthInfo
    class OAuth2Info extends AuthInfo

    /**
     * The auth info.
     */
    val passwordInfo = PasswordInfo("test.hasher", "test.password")
    val oauth1Info = new OAuth1Info()
    val oauth2Info = new OAuth2Info()

    /**
     * The DAOs.
     */
    lazy val passwordInfoDAO = spy(new PasswordInfoDAO)
    lazy val oauth1InfoDAO = spy(new OAuth1InfoDAO)
    lazy val oauth2InfoDAO = spy(new OAuth2InfoDAO)

    /**
     * The cache instance to store the different auth information instances.
     */
    val service = new DelegableAuthInfoRepository(passwordInfoDAO, oauth1InfoDAO, oauth2InfoDAO)

    /**
     * The DAO to store the password information.
     */
    class PasswordInfoDAO extends InMemoryAuthInfoDAO[PasswordInfo]

    /**
     * The DAO to store the OAuth1 information.
     */
    class OAuth1InfoDAO extends InMemoryAuthInfoDAO[OAuth1Info]

    /**
     * The DAO to store the OAuth2 information.
     */
    class OAuth2InfoDAO extends InMemoryAuthInfoDAO[OAuth2Info]
  }
}
