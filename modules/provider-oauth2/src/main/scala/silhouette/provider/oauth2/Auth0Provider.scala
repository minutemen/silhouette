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

import io.circe.Json
import io.circe.optics.JsonPath._
import silhouette.LoginInfo
import silhouette.http._
import silhouette.http.client.BodyFormat._
import silhouette.provider.oauth2.Auth0Provider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.provider.social.state.StateHandler

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

/**
 * Base Auth0 OAuth2 Provider.
 *
 * OAuth Provider configuration in silhouette.conf must indicate:
 *
 *   # Auth0 Service URIs
 *   auth0.authorizationUri="https://mydomain.eu.auth0.com/authorize"
 *   auth0.accessTokenUri="https://mydomain.eu.auth0.com/oauth/token"
 *   auth0.apiUri="https://mydomain.eu.auth0.com/userinfo"
 *
 *   # Application URI and credentials
 *   auth0.redirectUri="http://localhost:9000/authenticate/auth0"
 *   auth0.clientID=myoauthclientid
 *   auth0.clientSecret=myoauthclientsecret
 *
 *   # Auth0 user's profile information requested
 *   auth0.scope="openid name email picture"
 *
 * See http://auth0.com for more information on the Auth0 Auth 2.0 Provider and Service.
 */
trait BaseAuth0Provider extends OAuth2Provider {

  /**
   * The content type to parse a profile from.
   */
  override type Content = Json

  /**
   * The provider ID.
   */
  override val id = ID

  /**
   * Builds the social profile.
   *
   * @param authInfo The auth info received from the provider.
   * @return On success the build social profile, otherwise a failure.
   */
  override protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {
    httpClient.withUri(settings.apiUri.getOrElse(DefaultApiUri))
      .withHeaders(Header(Header.Name.Authorization, s"Bearer ${authInfo.accessToken}"))
      .withMethod(Method.GET)
      .execute
      .flatMap { response =>
        response.status match {
          case Status.OK =>
            response.body.as[Json] match {
              case Failure(error) => Future.failed(
                new ProfileRetrievalException(JsonParseError.format(id, response.body.raw), Some(error))
              )
              case Success(json) => profileParser.parse(json, authInfo)
            }
          case _ =>
            response.body.as[Json] match {
              case Failure(error) => Future.failed(
                new ProfileRetrievalException(JsonParseError.format("test1", response.body.raw), Some(error))
              )
              case Success(json) => root.error.string.getOption(json) match {
                case Some(error) =>
                  Future.failed(new ProfileRetrievalException(SpecifiedProfileError.format(
                    id,
                    root.error_description.string.getOption(json),
                    error,
                    response.status.code
                  )))
                case _ =>
                  throw new ProfileRetrievalException(GenericHttpStatusProfileError.format(id, response.status.code))
              }
            }
        }
      }
  }

  /**
   * Gets the access token.
   *
   * @param code    The access code.
   * @param request The current request.
   * @tparam R The type of the request.
   * @return The info containing the access token.
   */
  override protected def getAccessToken[R](code: String)(implicit request: RequestPipeline[R]): Future[OAuth2Info] = {
    request.queryParam("token_type").headOption match {
      case Some("bearer") => Future(OAuth2Info(code))
      case _              => super.getAccessToken(code)
    }
  }
}

/**
 * The profile parser for the common social profile.
 */
class Auth0ProfileParser extends SocialProfileParser[Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json The content returned from the provider.
   * @return The social profile from given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): Future[CommonSocialProfile] = Future.successful {
    val userID = root.user_id.string.getOption(json).getOrElse(
      throw new ProfileRetrievalException(JsonPathError.format(ID, "user_id", json.toString()))
    )

    CommonSocialProfile(
      loginInfo = LoginInfo(ID, userID),
      email = root.email.string.getOption(json),
      avatarURL = root.picture.string.getOption(json)
    )
  }
}

/**
 * The Auth0 OAuth2 Provider.
 *
 * @param httpClient   The HTTP client implementation.
 * @param stateHandler The state provider implementation.
 * @param settings     The provider settings.
 * @param ec           The execution context.
 */
class Auth0Provider(
  protected val httpClient: HttpClient,
  protected val stateHandler: StateHandler,
  val settings: OAuth2Settings
)(
  implicit
  override implicit val ec: ExecutionContext
) extends BaseAuth0Provider with CommonProfileBuilder {

  /**
   * The type of this class.
   */
  override type Self = Auth0Provider

  /**
   * The profile parser implementation.
   */
  override val profileParser = new Auth0ProfileParser

  /**
   * Gets a provider initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the provider initialized with new settings.
   */
  override def withSettings(f: Settings => Settings): Self =
    new Auth0Provider(httpClient, stateHandler, f(settings))
}

/**
 * The companion object.
 */
object Auth0Provider {

  /**
   * The error messages.
   */
  val SpecifiedProfileError = "[%s] Error retrieving profile information. Error message: %s, type: %s, code: %s"
  val GenericHttpStatusProfileError = "[%s] Cannot get user profile from provider - HTTP status code %s"

  /**
   * The Auth0 provider constant.
   */
  val ID = "auth0"

  /**
   * Default Auth0 provider endpoint.
   */
  val DefaultApiUri = new URI("https://auth0.auth0.com/userinfo")
}
