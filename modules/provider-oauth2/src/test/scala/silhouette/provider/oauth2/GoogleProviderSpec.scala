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

import silhouette.http.client.BodyFormat._
import silhouette.http.client.{ Body, Response }
import silhouette.http.{ Method, Status }
import silhouette.provider.oauth2.GoogleProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social.SocialProvider.UnspecifiedProfileError
import silhouette.provider.social.{ CommonSocialProfile, ProfileRetrievalException }
import silhouette.specs2.BaseFixture
import silhouette.{ ConfigURI, LoginInfo }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Test case for the [[GoogleProvider]] class.
 */
class GoogleProviderSpec extends OAuth2ProviderSpec {

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new Context {
      val apiResult = ErrorJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.`Bad Request`
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiUri.format(oAuth2Info.accessToken)) returns httpClient
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

      httpClient.withUri(DefaultApiUri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) {
        case e => e.getMessage must equalTo(UnspecifiedProfileError.format(ID))
      }
    }

    "use the overridden API URI" in new Context {
      val uri = ConfigURI("https://www.googleapis.com/plus/v1/people/me?access_token=%s&new")
      val apiResult = UserProfileJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.OK
      httpResponse.body returns Body.from(apiResult)

      config.apiUri returns Some(uri)

      httpClient.withUri(uri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      await(provider.retrieveProfile(oAuth2Info))

      there was one(httpClient).withUri(uri.format(oAuth2Info.accessToken))
    }

    "return the social profile with an email" in new Context {
      val apiResult = UserProfileJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.OK
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiUri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "109476598527568979481"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          fullName = Some("Apollonia Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarUri = Some(new URI("https://lh6.googleusercontent.com/-m34A6I77dJU/ASASAASADAAI/AVABAAAAAJk/" +
            "5cg1hcjo_4s/photo.jpg?sz=50"))
        )
      }
    }

    "return the social profile without an email" in new Context {
      val apiResult = WithoutEmailJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.OK
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiUri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "109476598527568979481"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          fullName = Some("Apollonia Vanova"),
          email = None,
          avatarUri = Some(new URI("https://lh6.googleusercontent.com/-m34A6I77dJU/ASASAASADAAI/AVABAAAAAJk/" +
            "5cg1hcjo_4s/photo.jpg?sz=50"))
        )
      }
    }

    "return the social profile with an avatar URI" in new Context {
      val apiResult = NonDefaultImageJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.OK
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiUri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "109476598527568979481"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          fullName = Some("Apollonia Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarUri = Some(new URI("https://lh6.googleusercontent.com/-m34A6I77dJU/ASASAASADAAI/AVABAAAAAJk/" +
            "5cg1hcjo_4s/photo.jpg?sz=50"))
        )
      }
    }

    "return the social profile without an avatar URI" in new Context {
      val apiResult = DefaultImageJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.OK
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiUri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "109476598527568979481"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          fullName = Some("Apollonia Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarUri = None
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
    override val ErrorJson = BaseFixture.load(Paths.get("google.error.json"))
    override val AccessTokenJson = BaseFixture.load(Paths.get("google.access.token.json"))
    override val UserProfileJson = BaseFixture.load(Paths.get("google.profile.json"))
    val DefaultImageJson = BaseFixture.load(Paths.get("google.img.default.json"))
    val NonDefaultImageJson = BaseFixture.load(Paths.get("google.img.non-default.json"))
    val WithoutEmailJson = BaseFixture.load(Paths.get("google.without.email.json"))

    /**
     * The OAuth2 config.
     */
    override lazy val config = spy(OAuth2Config(
      authorizationUri = Some(ConfigURI("https://accounts.google.com/o/oauth2/auth")),
      accessTokenUri = ConfigURI("https://accounts.google.com/o/oauth2/token"),
      redirectUri = Some(ConfigURI("https://minutemen.group")),
      clientID = "my.client.id",
      clientSecret = "my.client.secret",
      scope = Some("profile,email")
    ))

    /**
     * The provider to test.
     */
    lazy val provider = new GoogleProvider(httpClient, stateHandler, config)
  }
}
