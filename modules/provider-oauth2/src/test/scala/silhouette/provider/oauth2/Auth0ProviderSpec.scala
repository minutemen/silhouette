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

import silhouette.{ ConfigURI, LoginInfo }
import silhouette.http.client.BodyFormat._
import silhouette.http.client.{ Body, Response }
import silhouette.http.{ Header, Method, Status }
import silhouette.provider.oauth2.Auth0Provider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social.SocialProvider.UnspecifiedProfileError
import silhouette.provider.social.{ CommonSocialProfile, ProfileRetrievalException }
import silhouette.specs2.BaseFixture

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Test case for the [[Auth0Provider]] class.
 */
class Auth0ProviderSpec extends OAuth2ProviderSpec {

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new Context {
      val apiResult = ErrorJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.`Bad Request`
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiUri) returns httpClient
      httpClient.withHeaders(
        Header(Header.Name.Authorization, s"Bearer ${oAuth2Info.accessToken}")
      ) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) {
        case e => e.getMessage must equalTo(SpecifiedProfileError.format(
          provider.id,
          Status.`Bad Request`,
          apiResult
        ))
      }
    }

    "fail with ProfileRetrievalException if an unexpected error occurred" in new Context {
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.`Internal Server Error`
      httpResponse.body throws new RuntimeException("")

      httpClient.withUri(DefaultApiUri) returns httpClient
      httpClient.withHeaders(
        Header(Header.Name.Authorization, s"Bearer ${oAuth2Info.accessToken}")
      ) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) {
        case e => e.getMessage must equalTo(UnspecifiedProfileError.format(ID))
      }
    }

    "use the overridden API URI" in new Context {
      val uri = DefaultApiUri.copy(uri = DefaultApiUri.uri + "&new")
      val apiResult = UserProfileJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.OK
      httpResponse.body returns Body.from(apiResult)

      config.apiUri returns Some(uri)

      httpClient.withUri(uri) returns httpClient
      httpClient.withHeaders(
        Header(Header.Name.Authorization, s"Bearer ${oAuth2Info.accessToken}")
      ) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      await(provider.retrieveProfile(oAuth2Info))

      there was one(httpClient).withUri(uri)
    }

    "return the social profile" in new Context {
      val apiResult = UserProfileJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.OK
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiUri) returns httpClient
      httpClient.withHeaders(
        Header(Header.Name.Authorization, s"Bearer ${oAuth2Info.accessToken}")
      ) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "auth0|56961100fc02d8a0339b1a2a"),
          fullName = Some("Apollonia Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarUri = Some(new URI("https://s.gravatar.com/avatar/c1c49231ce863e5f33d7f42cd44632f4?s=480&r=pg&" +
            "d=https%3A%2F%2Fcdn.auth0.com%2Favatars%2Flu.png"))
        )
      }
    }
  }

  /**
   * Defines the context for the abstract OAuth2 provider spec.
   *
   * @return The Context to use for the abstract OAuth2 provider spec.
   */
  override protected def context: BaseContext = new Context {}

  /**
   * The context.
   */
  trait Context extends BaseContext {

    /**
     * Paths to the Json fixtures.
     */
    override val ErrorJson = BaseFixture.load(Paths.get("auth0.error.json"))
    override val AccessTokenJson = BaseFixture.load(Paths.get("auth0.access.token.json"))
    override val UserProfileJson = BaseFixture.load(Paths.get("auth0.profile.json"))

    /**
     * The OAuth2 config.
     */
    override lazy val config = spy(OAuth2Config(
      authorizationUri = Some(ConfigURI("https://gerritforge.eu.auth0.com/authorize")),
      accessTokenUri = ConfigURI("https://gerritforge.eu.auth0.com/oauth/token"),
      redirectUri = Some(ConfigURI("https://minutemen.group")),
      clientID = "some.client.id",
      clientSecret = "some.secret",
      scope = Some("email")
    ))

    /**
     * The provider to test.
     */
    lazy val provider = new Auth0Provider(httpClient, stateHandler, config)
  }
}
