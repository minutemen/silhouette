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

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.authenticator.Validator.{ Invalid, Valid }
import silhouette.authenticator._
import silhouette.specs2.WaitPatience
import silhouette.{ Reads => _, _ }

import scala.concurrent.Future

/**
 * Test case for the [[ReadsAuthenticationPipeline]] class.
 *
 * @param ev The execution environment.
 */
class ReadsAuthenticationPipelineSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `read` method" should {
    "return the `MissingCredentials` state if no token was given" in new Context {
      pipeline.read(None) must beEqualTo(MissingCredentials()).awaitWithPatience
    }

    "throws an `AuthenticatorException` if the token couldn't be transformed into an authenticator" in new Context {
      val exception = new AuthenticatorException("Parse error")

      reads.read(token) returns Future.failed(exception)

      pipeline.read(Some(token)) must throwA[AuthenticatorException].like {
        case e => e.getMessage must beEqualTo(exception.getMessage)
      }.awaitWithPatience
    }

    "return the `InvalidCredentials` state if the authenticator is invalid" in new Context {
      val errors = Seq("Authenticator is invalid")

      reads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.successful(Invalid(errors))

      pipeline.read(Some(token)) must beEqualTo(InvalidCredentials(authenticator, errors)).awaitWithPatience
    }

    "throws an `AuthenticatorException` if the validator throws an exception" in new Context {
      val exception = new AuthenticatorException("Validation error")

      reads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.failed(exception)

      pipeline.read(Some(token)) must throwA[AuthenticatorException].like {
        case e => e.getMessage must beEqualTo(exception.getMessage)
      }.awaitWithPatience
    }

    "return the `MissingIdentity` state if the identity couldn't be found for the login info" in new Context {
      reads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.successful(Valid)
      identityReader.apply(loginInfo) returns Future.successful(None)

      pipeline.read(Some(token)) must beEqualTo(MissingIdentity(authenticator, loginInfo)).awaitWithPatience
    }

    "throws an `AuthenticatorException` if the identity reader throws an exception" in new Context {
      val exception = new AuthenticatorException("Retrieval error")

      reads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.successful(Valid)
      identityReader.apply(loginInfo) returns Future.failed(exception)

      pipeline.read(Some(token)) must throwA[AuthenticatorException].like {
        case e => e.getMessage must beEqualTo(exception.getMessage)
      }.awaitWithPatience
    }

    "return the `Authenticated` state if the authentication process was successful" in new Context {
      reads.read(token) returns Future.successful(authenticator)
      validator.isValid(authenticator) returns Future.successful(Valid)
      identityReader.apply(loginInfo) returns Future.successful(Some(user))

      pipeline.read(Some(token)) must beEqualTo(Authenticated(user, authenticator, loginInfo)).awaitWithPatience
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
    val reads = mock[Reads[String]]

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
     * The pipeline to test.
     */
    val pipeline = ReadsAuthenticationPipeline(reads, identityReader, Set(validator))
  }
}
