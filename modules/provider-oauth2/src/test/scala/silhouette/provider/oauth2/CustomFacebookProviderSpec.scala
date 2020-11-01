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
import java.time.Clock

import cats.effect.IO._
import cats.effect.{ Async, IO }
import io.circe.Json
import silhouette.LoginInfo
import silhouette.provider.oauth2.FacebookProvider.DefaultApiUri
import silhouette.provider.oauth2.OAuth2Provider.UnexpectedResponse
import silhouette.provider.social.SocialProvider.ProfileError
import silhouette.provider.social._
import silhouette.specs2.BaseFixture
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{ HttpError, Request, Response, SttpBackend }
import sttp.model.Uri._
import sttp.model.{ Method, StatusCode, Uri }

/**
 * Test case for the [[FacebookProvider]] class.
 */
class CustomFacebookProviderSpec extends OAuth2ProviderSpec {

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new Context {
      val apiResult = ErrorJson.asJson

      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiUri.withParam("access_token", oAuth2Info.accessToken)))
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
        .whenRequestMatches(requestMatcher(DefaultApiUri.withParam("access_token", oAuth2Info.accessToken)))
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
        .whenRequestMatches(requestMatcher(uri.withParam("access_token", oAuth2Info.accessToken)))
        .thenRespond(Response(apiResult.toString, StatusCode.Ok))

      provider.retrieveProfile(oAuth2Info).unsafeRunSync()
    }

    "return the social profile" in new Context {
      sttpBackend = AsyncHttpClientCatsBackend.stub
        .whenRequestMatches(requestMatcher(DefaultApiUri.withParam("access_token", oAuth2Info.accessToken)))
        .thenRespond(Response(UserProfileJson.asJson.toString, StatusCode.Ok))

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        val uriStr = "https://fbcdn-sphotos-g-a.akamaihd.net/hphotos-ak-ash2/t1/" +
          "36245_155530314499277_2350717_n.jpg?lvh=1"
        p must be equalTo CustomSocialProfile(
          loginInfo = LoginInfo(provider.id, "134405962728980"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          fullName = Some("Apollonia Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarUri = Some(uri"$uriStr"),
          gender = Some("male")
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
     * The OAuth2 config.
     */
    override lazy val config = spy(
      OAuth2Config(
        authorizationUri = Some(uri"https://graph.facebook.com/oauth/authorize"),
        accessTokenUri = uri"https://graph.facebook.com/oauth/access_token",
        redirectUri = Some(uri"https://minutemen.group"),
        clientID = "my.client.id",
        clientSecret = "my.client.secret",
        scope = Some("email")
      )
    )

    /**
     * The provider to test.
     */
    lazy val provider = new CustomFacebookProvider(clock, config)

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

  /**
   * A custom social profile for testing purpose.
   */
  case class CustomSocialProfile(
    loginInfo: LoginInfo,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fullName: Option[String] = None,
    email: Option[String] = None,
    avatarUri: Option[Uri] = None,
    gender: Option[String] = None
  ) extends SocialProfile

  /**
   * The Facebook OAuth2 Provider.
   *
   * @param clock       The current clock instance.
   * @param config      The provider config.
   * @param sttpBackend The STTP backend.
   * @param F           The IO monad type class.
   */
  class CustomFacebookProvider(
    protected val clock: Clock,
    val config: OAuth2Config
  )(
    implicit
    protected val sttpBackend: SttpBackend[IO, Any],
    protected val F: Async[IO]
  ) extends BaseFacebookProvider[IO] {

    /**
     * The type of this class.
     */
    override type Self = CustomFacebookProvider

    /**
     * The type of the profile a profile builder is responsible for.
     */
    override type Profile = CustomSocialProfile

    /**
     * The profile parser implementation.
     */
    override val profileParser = (json: Json, authInfo: OAuth2Info) => {
      new FacebookProfileParser[IO]().parse(json, authInfo).map { commonProfile =>
        CustomSocialProfile(
          loginInfo = commonProfile.loginInfo,
          firstName = commonProfile.firstName,
          lastName = commonProfile.lastName,
          fullName = commonProfile.fullName,
          avatarUri = commonProfile.avatarUri,
          email = commonProfile.email,
          gender = json.hcursor.downField("gender").as[String].toOption
        )
      }
    }

    /**
     * Gets a provider initialized with a new config object.
     *
     * @param f A function which gets the config passed and returns different config.
     * @return An instance of the provider initialized with new config.
     */
    override def withConfig(f: OAuth2Config => OAuth2Config): Self =
      new CustomFacebookProvider(clock, f(config))
  }
}
