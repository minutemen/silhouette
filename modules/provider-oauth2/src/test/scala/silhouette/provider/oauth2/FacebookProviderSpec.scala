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

import java.nio.file.Paths

import io.circe.optics.JsonPath._
import silhouette.LoginInfo
import silhouette.http.client.BodyFormat._
import silhouette.http.client.{ Body, Response }
import silhouette.http.{ Method, Status, URI }
import silhouette.provider.oauth2.FacebookProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social.SocialProvider.UnspecifiedProfileError
import silhouette.provider.social.{ CommonSocialProfile, ProfileRetrievalException }
import silhouette.specs2.BaseFixture

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Test case for the [[FacebookProvider]] class.
 */
class FacebookProviderSpec extends OAuth2ProviderSpec {

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new Context {
      val apiResult = ErrorJson.asJson
      val httpResponse = mock[Response]
      httpResponse.status returns Status.`Bad Request`
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiUri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) {
        case e => e.getMessage must equalTo(SpecifiedProfileError.format(
          provider.id,
          Status.`Bad Request`,
          root.error.json.getOption(apiResult).get
        ))
      }
    }

    "fail with ProfileRetrievalException if an unexpected error occurred" in new Context {
      val httpResponse = mock[Response]
      httpResponse.status returns 500
      httpResponse.body throws new RuntimeException("")

      httpClient.withUri(DefaultApiUri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) {
        case e => e.getMessage must equalTo(UnspecifiedProfileError.format(ID))
      }
    }

    "return the social profile" in new Context {
      val apiResult = UserProfileJson.asJson
      val httpResponse = mock[Response]
      httpResponse.status returns 200
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiUri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "134405962728980"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          fullName = Some("Apollonia Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarURL = Some("https://fbcdn-sphotos-g-a.akamaihd.net/hphotos-ak-ash2/t1/36245_155530314499277_2350717_n" +
            ".jpg?lvh=1")
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
    override val ErrorJson = BaseFixture.load(Paths.get("facebook.error.json"))
    override val AccessTokenJson = BaseFixture.load(Paths.get("facebook.access.token.json"))
    override val UserProfileJson = BaseFixture.load(Paths.get("facebook.profile.json"))

    /**
     * The OAuth2 settings.
     */
    override lazy val settings = spy(OAuth2Settings(
      authorizationUri = Some(URI("https://graph.facebook.com/oauth/authorize")),
      accessTokenUri = URI("https://graph.facebook.com/oauth/access_token"),
      redirectUri = Some(URI("https://www.mohiva.com")),
      clientID = "my.client.id",
      clientSecret = "my.client.secret",
      scope = Some("email")
    ))

    /**
     * The provider to test.
     */
    lazy val provider = new FacebookProvider(httpClient, stateHandler, settings)
  }
}
