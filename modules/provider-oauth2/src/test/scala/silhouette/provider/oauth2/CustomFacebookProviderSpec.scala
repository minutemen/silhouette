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
import java.time.Clock

import io.circe.Json
import io.circe.optics.JsonPath._
import silhouette.http.client.BodyFormat._
import silhouette.http.client.{ Body, Response }
import silhouette.http.{ HttpClient, Method, Status }
import silhouette.provider.oauth2.FacebookProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social.SocialProvider.UnspecifiedProfileError
import silhouette.provider.social.state.StateHandler
import silhouette.provider.social._
import silhouette.specs2.BaseFixture
import silhouette.{ ConfigURI, LoginInfo }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Test case for the [[FacebookProvider]] class.
 */
class CustomFacebookProviderSpec extends OAuth2ProviderSpec {

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new Context {
      val apiResult = ErrorJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.`Bad Request`
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiURI.format(oAuth2Info.accessToken)) returns httpClient
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

      httpClient.withUri(DefaultApiURI.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuth2Info)) {
        case e => e.getMessage must equalTo(UnspecifiedProfileError.format(ID))
      }
    }

    "use the overridden API URI" in new Context {
      val uri = DefaultApiURI.copy(uri = DefaultApiURI.uri + "&new")
      val apiResult = UserProfileJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.OK
      httpResponse.body returns Body.from(apiResult)

      config.apiURI returns Some(uri)

      httpClient.withUri(uri.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      await(provider.retrieveProfile(oAuth2Info))

      there was one(httpClient).withUri(uri.format(oAuth2Info.accessToken))
    }

    "return the social profile" in new Context {
      val apiResult = UserProfileJson.asJson
      val httpResponse = mock[Response].smart
      httpResponse.status returns Status.OK
      httpResponse.body returns Body.from(apiResult)

      httpClient.withUri(DefaultApiURI.format(oAuth2Info.accessToken)) returns httpClient
      httpClient.withMethod(Method.GET) returns httpClient
      httpClient.execute returns Future.successful(httpResponse)

      profile(provider.retrieveProfile(oAuth2Info)) { p =>
        p must be equalTo CustomSocialProfile(
          loginInfo = LoginInfo(provider.id, "134405962728980"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          fullName = Some("Apollonia Vanova"),
          email = Some("apollonia.vanova@minutemen.group"),
          avatarUri = Some(new URI("https://fbcdn-sphotos-g-a.akamaihd.net/hphotos-ak-ash2/t1/" +
            "36245_155530314499277_2350717_n.jpg?lvh=1")),
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
    override lazy val config = spy(OAuth2Config(
      authorizationURI = Some(ConfigURI("https://graph.facebook.com/oauth/authorize")),
      accessTokenURI = ConfigURI("https://graph.facebook.com/oauth/access_token"),
      redirectURI = Some(ConfigURI("https://minutemen.group")),
      clientID = "my.client.id",
      clientSecret = "my.client.secret",
      scope = Some("email")
    ))

    /**
     * The provider to test.
     */
    lazy val provider = new CustomFacebookProvider(httpClient, stateHandler, clock, config)
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
    avatarUri: Option[URI] = None,
    gender: Option[String] = None
  ) extends SocialProfile

  /**
   * The Facebook OAuth2 Provider.
   *
   * @param httpClient   The HTTP client implementation.
   * @param stateHandler The state provider implementation.
   * @param clock        The clock instance.
   * @param config       The provider config.
   * @param ec           The execution context.
   */
  class CustomFacebookProvider(
    protected val httpClient: HttpClient,
    protected val stateHandler: StateHandler,
    protected val clock: Clock,
    val config: OAuth2Config
  )(
    implicit
    override implicit val ec: ExecutionContext
  ) extends BaseFacebookProvider {

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
      new FacebookProfileParser().parse(json, authInfo).map { commonProfile =>
        CustomSocialProfile(
          loginInfo = commonProfile.loginInfo,
          firstName = commonProfile.firstName,
          lastName = commonProfile.lastName,
          fullName = commonProfile.fullName,
          avatarUri = commonProfile.avatarUri,
          email = commonProfile.email,
          gender = root.gender.string.getOption(json)
        )
      }(ec)
    }

    /**
     * Gets a provider initialized with a new config object.
     *
     * @param f A function which gets the config passed and returns different config.
     * @return An instance of the provider initialized with new config.
     */
    override def withConfig(f: OAuth2Config => OAuth2Config): Self =
      new CustomFacebookProvider(httpClient, stateHandler, clock, f(config))(ec)
  }
}
