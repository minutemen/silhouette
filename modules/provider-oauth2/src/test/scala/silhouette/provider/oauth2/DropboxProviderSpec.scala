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

import cats.effect.IO._
import silhouette.http.BearerAuthorizationHeader
import silhouette.provider.oauth2.DropboxProvider._
import silhouette.provider.oauth2.OAuth2Provider.UnexpectedResponse
import silhouette.provider.social.SocialProvider.ProfileError
import silhouette.provider.social.{ CommonSocialProfile, ProfileRetrievalException }
import silhouette.specs2.BaseFixture
import silhouette.{ ConfigURI, LoginInfo }
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.{ Request, Response, StringBody, SttpClientException }
import sttp.model.{ MediaType, Method, StatusCode }

import scala.io.Codec

/**
 * Test case for the [[DropboxProvider]] class.
 */
class DropboxProviderSpec extends OAuth2ProviderSpec {

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new Context {
      val apiResult = ErrorJson.asJson

      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiURI))
        .thenRespond(Response(
          StringBody(apiResult.toString, Codec.UTF8.charSet.name(), Some(MediaType.ApplicationJson)),
          StatusCode.BadRequest
        ))

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) {
        case e =>
          e.getMessage must equalTo(ProfileError.format(provider.id))
          e.getCause.getMessage must equalTo(UnexpectedResponse.format(
            provider.id,
            apiResult,
            StatusCode.BadRequest
          ))
      }
    }

    "fail with ProfileRetrievalException if an unexpected error occurred" in new Context {
      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiURI))
        .thenRespond(throw new SttpClientException.ConnectException(new RuntimeException))

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) {
        case e => e.getMessage must equalTo(ProfileError.format(ID))
      }
    }

    "use the overridden API URI" in new Context {
      val uri = DefaultApiURI.copy(uri = DefaultApiURI.uri + "&new")
      val apiResult = UserProfileJson.asJson

      config.apiURI returns Some(uri)
      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(uri))
        .thenRespond(Response(
          StringBody(apiResult.toString, Codec.UTF8.charSet.name(), Some(MediaType.ApplicationJson)),
          StatusCode.Ok
        ))

      provider.retrieveProfile(oAuth2Info).unsafeRunSync()
    }

    "return the social profile" in new Context {
      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiURI))
        .thenRespond(Response(
          StringBody(UserProfileJson.asJson.toString, Codec.UTF8.charSet.name(), Some(MediaType.ApplicationJson)),
          StatusCode.Ok
        ))

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
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
  override protected def context: BaseContext = new Context {}

  /**
   * The context.
   */
  trait Context extends BaseContext {

    /**
     * Paths to the Json fixtures.
     */
    override val ErrorJson = BaseFixture.load(Paths.get("dropbox.error.json"))
    override val AccessTokenJson = BaseFixture.load(Paths.get("dropbox.access.token.json"))
    override val UserProfileJson = BaseFixture.load(Paths.get("dropbox.profile.json"))

    /**
     * The OAuth2 config.
     */
    override lazy val config = spy(OAuth2Config(
      authorizationURI = Some(ConfigURI("https://www.dropbox.com/1/oauth2/authorize")),
      accessTokenURI = ConfigURI("https://api.dropbox.com/1/oauth2/token"),
      redirectURI = Some(ConfigURI("https://minutemen.group")),
      clientID = "my.client.id",
      clientSecret = "my.client.secret",
      scope = None
    ))

    /**
     * The provider to test.
     */
    lazy val provider = new DropboxProvider(clock, config)

    /**
     * Matches the request for the STTP backend stub.
     *
     * @param uri To URI to match against.
     * @return A partial function that matches the request.
     */
    def requestMatcher(uri: ConfigURI): PartialFunction[Request[_, _], Boolean] = {
      case r: Request[_, _] =>
        r.method == Method.GET &&
          r.uri == uri.toSttpUri &&
          r.headers.contains(BearerAuthorizationHeader(oAuth2Info.accessToken))
    }
  }
}
