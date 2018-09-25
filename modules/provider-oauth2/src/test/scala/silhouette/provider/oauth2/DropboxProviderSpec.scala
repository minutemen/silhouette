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
package silhouette.provider.oauth2

import java.net.URI
import java.nio.file.Paths

import io.circe.Json
import silhouette.LoginInfo
import silhouette.http.client.BodyFormat._
import silhouette.http.client.{ Body, Response }
import silhouette.http.{ Fake, Header, Method, Status }
import silhouette.provider.UnexpectedResponseException
import silhouette.provider.oauth2.DropboxProvider._
import silhouette.provider.oauth2.OAuth2Provider.{ Code, InvalidInfoFormat, UnexpectedResponse }
import silhouette.provider.social.SocialProvider.UnspecifiedProfileError
import silhouette.provider.social.state.StateItemHandler
import silhouette.provider.social.{ CommonSocialProfile, ProfileRetrievalException, StatefulAuthInfo }
import silhouette.specs2.BaseFixture

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Test case for the [[DropboxProvider]] class.
 */
class DropboxProviderSpec extends OAuth2ProviderSpec {

  "The `withSettings` method" should {
    "create a new instance with customized settings" in new Context {
      val s = provider.withSettings { s =>
        s.copy(accessTokenUri = new URI("new-access-token-url"))
      }

      s.settings.accessTokenUri must be equalTo new URI("new-access-token-url")
    }
  }

  "The `authenticate` method" should {
    "fail with UnexpectedResponseException for an unexpected response" in new Context {
      val code = "my.code"
      val httpResponse = mock[Response]
      val request = Fake.request.withQueryParams(Code -> code)

      httpResponse.status returns 401
      httpResponse.body returns Body.from("Unauthorized")

      httpClient.withUri(settings.accessTokenUri) returns httpClient
      httpClient.withHeaders(any()) returns httpClient
      httpClient.withBody[Map[String, Seq[String]]](any())(any()) returns httpClient
      httpClient.withMethod(Method.POST) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(state)
      stateHandler.state returns Future.successful(state)

      failed[UnexpectedResponseException](provider.authenticate()(request)) {
        case e => e.getMessage must be equalTo UnexpectedResponse.format(
          provider.id, httpResponse.body.raw, Status.Unauthorized
        )
      }
    }

    "fail with UnexpectedResponseException if OAuth2Info can be build because of an unexpected response" in
      new Context {
        val code = "my.code"
        val httpResponse = mock[Response]
        val request = Fake.request.withQueryParams(Code -> code)

        httpResponse.status returns 200
        httpResponse.body returns Body.from(Json.obj())

        httpClient.withUri(settings.accessTokenUri) returns httpClient
        httpClient.withHeaders(any()) returns httpClient
        httpClient.withBody[Map[String, Seq[String]]](any())(any()) returns httpClient
        httpClient.withMethod(Method.POST) returns httpClient
        httpClient.execute returns Future.successful(httpResponse)

        stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(state)
        stateHandler.state returns Future.successful(state)

        failed[UnexpectedResponseException](provider.authenticate()(request)) {
          case e => e.getMessage must startWith(InvalidInfoFormat.format(provider.id, ""))
        }
      }

    "return the auth info" in new Context {
      val code = "my.code"
      val httpResponse = mock[Response]
      val request = Fake.request.withQueryParams(Code -> code)

      httpResponse.status returns 200
      httpResponse.body returns Body.from(oAuthInfo)

      httpClient.withUri(settings.accessTokenUri) returns httpClient
      httpClient.withHeaders(any()) returns httpClient
      httpClient.withBody[Map[String, Seq[String]]](any())(any()) returns httpClient
      httpClient.withMethod(Method.POST) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(state)
      stateHandler.state returns Future.successful(state)

      authInfo(provider.authenticate()(request))(_ must be equalTo oAuthInfoObject)
    }
  }

  "The `authenticate` method with user state" should {
    "return stateful auth info" in new Context {
      val code = "my.code"
      val httpResponse = mock[Response]
      implicit val request: Fake.Request = Fake.request.withQueryParams(Code -> code)

      httpResponse.status returns 200
      httpResponse.body returns Body.from(oAuthInfo)

      httpClient.withUri(settings.accessTokenUri) returns httpClient
      httpClient.withHeaders(any()) returns httpClient
      httpClient.withBody[Map[String, Seq[String]]](any())(any()) returns httpClient
      httpClient.withMethod(Method.POST) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(state)
      stateHandler.state returns Future.successful(state)
      stateHandler.withHandler(any[StateItemHandler]()) returns stateHandler
      state.items returns Set(userStateItem)

      statefulAuthInfo(provider.authenticate(userStateItem))(_ must be equalTo stateAuthInfo)
    }
  }

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new Context {
      val error = ErrorJson.asJson
      val httpResponse = mock[Response]
      httpResponse.status returns Status.`Bad Request`
      httpResponse.body returns Body.from(error)

      httpClient.withUri(DefaultApiUri) returns httpClient
      httpClient.withHeaders(
        Header(Header.Name.Authorization, s"Bearer ${oAuthInfoObject.accessToken}")
      ) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuthInfoObject)) {
        case e => e.getMessage must equalTo(SpecifiedProfileError.format(
          provider.id,
          "Invalid OAuth request.",
          Status.`Bad Request`,
          error
        ))
      }
    }

    "fail with ProfileRetrievalException if an unexpected error occurred" in new Context {
      val httpResponse = mock[Response]
      httpResponse.status returns 500
      httpResponse.body throws new RuntimeException("")

      httpClient.withUri(DefaultApiUri) returns httpClient
      httpClient.withHeaders(
        Header(Header.Name.Authorization, s"Bearer ${oAuthInfoObject.accessToken}")
      ) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuthInfoObject)) {
        case e => e.getMessage must equalTo(UnspecifiedProfileError.format(ID))
      }
    }

    "return the social profile" in new Context {
      val userProfile = UserProfileJson.asJson
      val httpResponse = mock[Response]
      httpResponse.status returns 200
      httpResponse.body returns Body.from(userProfile)

      httpClient.withUri(DefaultApiUri) returns httpClient
      httpClient.withHeaders(
        Header(Header.Name.Authorization, s"Bearer ${oAuthInfoObject.accessToken}")
      ) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      profile(provider.retrieveProfile(oAuthInfoObject)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "12345678"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          fullName = Some("Apollonia Vanova")
        )
      }
    }
  }

  /**
   * Defines the context for the abstract OAuth2 provider spec.
   *
   * @return The Context to use for the abstract OAuth2 provider spec.
   */
  override protected def context: OAuth2ProviderSpecContext = new Context {}

  /**
   * The context.
   */
  trait Context extends OAuth2ProviderSpecContext {

    /**
     * Paths to the Json fixtures.
     */
    lazy val ErrorJson = BaseFixture.load(Paths.get("dropbox.error.json"))
    lazy val AccessTokenJson = BaseFixture.load(Paths.get("dropbox.access.token.json"))
    lazy val UserProfileJson = BaseFixture.load(Paths.get("dropbox.profile.json"))

    /**
     * The OAuth2 settings.
     */
    override lazy val settings = spy(OAuth2Settings(
      authorizationUri = Some(new URI("https://www.dropbox.com/1/oauth2/authorize")),
      accessTokenUri = new URI("https://api.dropbox.com/1/oauth2/token"),
      redirectUri = Some(new URI("https://www.mohiva.com")),
      clientID = "my.client.id",
      clientSecret = "my.client.secret",
      scope = None
    ))

    /**
     * The OAuth2 info returned by Dropbox.
     *
     * @see https://www.dropbox.com/developers/core/docs#oa2-token
     */
    override lazy val oAuthInfo = AccessTokenJson.asJson

    /**
     * The OAuth2 info deserialized as case class object.
     */
    lazy val oAuthInfoObject = AccessTokenJson.as[OAuth2Info]

    /**
     * The stateful auth info.
     */
    override lazy val stateAuthInfo = StatefulAuthInfo(oAuthInfoObject, userStateItem)

    /**
     * The provider to test.
     */
    lazy val provider = new DropboxProvider(httpClient, stateHandler, settings)
  }
}
