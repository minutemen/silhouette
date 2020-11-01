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

import cats.effect.IO.{ Map => _, _ }
import silhouette.LoginInfo
import silhouette.provider.oauth2.FoursquareProvider.{ AvatarResolution, DefaultApiUri }
import silhouette.provider.oauth2.OAuth2Provider.UnexpectedResponse
import silhouette.provider.social.SocialProvider.ProfileError
import silhouette.provider.social.{ CommonSocialProfile, ProfileRetrievalException }
import silhouette.specs2.BaseFixture
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{ HttpError, Request, Response }
import sttp.model.Uri._
import sttp.model.{ Method, StatusCode, Uri }

/**
 * Test case for the [[FoursquareProvider]] class.
 */
class FoursquareProviderSpec extends OAuth2ProviderSpec {

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new Context {
      val apiResult = ErrorJson.asJson

      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiUri.withParam("oauth_token", oAuth2Info.accessToken)))
        .thenRespond(Response(apiResult.toString, StatusCode.BadRequest))

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) { case e =>
        e.getMessage must equalTo(ProfileError.format(provider.id))
        e.getCause.getMessage must equalTo(
          UnexpectedResponse.format(
            provider.id,
            HttpError(apiResult, StatusCode.BadRequest).getMessage,
            StatusCode.BadRequest
          )
        )
      }
    }

    "fail with ProfileRetrievalException if an unexpected error occurred" in new Context {
      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiUri.withParam("oauth_token", oAuth2Info.accessToken)))
        .thenRespond(throw new RuntimeException)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) { case e =>
        e.getMessage must equalTo(ProfileError.format(provider.id))
      }
    }

    "use the overridden API URI" in new Context {
      val uri = DefaultApiUri.withParam("new", "true")
      val apiResult = UserProfileJson.asJson

      config.apiUri returns Some(uri)
      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(uri.withParam("oauth_token", oAuth2Info.accessToken)))
        .thenRespond(Response(apiResult.toString, StatusCode.Ok))

      provider.retrieveProfile(oAuth2Info).unsafeRunSync()
    }

    "return the social profile" in new Context {
      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiUri.withParam("oauth_token", oAuth2Info.accessToken)))
        .thenRespond(Response(UserProfileJson.asJson.toString, StatusCode.Ok))

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "13221052"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarUri = Some(uri"https://irs0.4sqi.net/img/user/100x100/blank_girl.png")
        )
      }
    }

    "return the social profile if API is deprecated" in new Context {
      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiUri.withParam("oauth_token", oAuth2Info.accessToken)))
        .thenRespond(Response(DeprecatedJson.asJson.toString, StatusCode.Ok))

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "13221052"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarUri = Some(uri"https://irs0.4sqi.net/img/user/100x100/blank_girl.png")
        )
      }
    }

    "handle the custom avatar resolution property" in new Context {
      val customProperties = Map(AvatarResolution -> "150x150")

      config.customProperties returns customProperties
      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiUri.withParam("oauth_token", oAuth2Info.accessToken)))
        .thenRespond(Response(UserProfileJson.asJson.toString, StatusCode.Ok))

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "13221052"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarUri = Some(uri"https://irs0.4sqi.net/img/user/150x150/blank_girl.png")
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
    override val ErrorJson = BaseFixture.load(Paths.get("foursquare.error.json"))
    override val AccessTokenJson = BaseFixture.load(Paths.get("foursquare.access.token.json"))
    override val UserProfileJson = BaseFixture.load(Paths.get("foursquare.profile.json"))
    val DeprecatedJson = BaseFixture.load(Paths.get("foursquare.deprecated.json"))

    /**
     * The OAuth2 config.
     */
    override lazy val config = spy(
      OAuth2Config(
        authorizationUri = Some(uri"https://foursquare.com/oauth2/authenticate"),
        accessTokenUri = uri"https://foursquare.com/oauth2/access_token",
        redirectUri = Some(uri"https://minutemen.group"),
        clientID = "my.client.id",
        clientSecret = "my.client.secret",
        scope = None
      )
    )

    /**
     * The provider to test.
     */
    lazy val provider = new FoursquareProvider(clock, config)

    /**
     * Matches the request for the STTP backend stub.
     *
     * @param uri To URI to match against.
     * @return A partial function that matches the request.
     */
    def requestMatcher(uri: Uri): PartialFunction[Request[_, _], Boolean] = { case r: Request[_, _] =>
      r.method == Method.GET && r.uri == uri
    }
  }
}
