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
import java.time.Clock

import io.circe.Json
import io.circe.optics.JsonPath._
import silhouette.http.Method.GET
import silhouette.http.client.Request
import silhouette.http.{ HttpClient, Status }
import silhouette.provider.UnexpectedResponseException
import silhouette.provider.oauth2.FacebookProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.provider.social.state.StateHandler
import silhouette.{ ConfigURI, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Base Facebook OAuth2 Provider.
 *
 * @see https://developers.facebook.com/tools/explorer
 * @see https://developers.facebook.com/docs/graph-api/reference/user
 * @see https://developers.facebook.com/docs/facebook-login/access-tokens
 */
trait BaseFacebookProvider extends OAuth2Provider {

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
    val uri = config.apiURI.getOrElse(DefaultApiURI).format(authInfo.accessToken)

    httpClient.execute(Request(GET, uri)).flatMap { response =>
      withParsedJson(response) { json =>
        response.status match {
          case Status.OK =>
            profileParser.parse(json, authInfo)
          case status =>
            Future.failed(new UnexpectedResponseException(UnexpectedResponse.format(id, json, status)))
        }
      }
    }
  }
}

/**
 * The profile parser for the common social profile.
 */
class FacebookProfileParser extends SocialProfileParser[Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): Future[CommonSocialProfile] = Future.successful {
    CommonSocialProfile(
      loginInfo = LoginInfo(ID, root.id.string.getOrError(json, "id", ID)),
      firstName = root.first_name.string.getOption(json),
      lastName = root.last_name.string.getOption(json),
      fullName = root.name.string.getOption(json),
      email = root.email.string.getOption(json),
      avatarUri = root.picture.data.url.string.getOption(json).map(uri => new URI(uri))
    )
  }
}

/**
 * The Facebook OAuth2 Provider.
 *
 * @param httpClient   The HTTP client implementation.
 * @param stateHandler The state provider implementation.
 * @param clock        The current clock instance.
 * @param config       The provider config.
 * @param ec           The execution context.
 */
class FacebookProvider(
  protected val httpClient: HttpClient,
  protected val stateHandler: StateHandler,
  protected val clock: Clock,
  val config: OAuth2Config
)(
  implicit
  override implicit val ec: ExecutionContext
) extends BaseFacebookProvider with CommonProfileBuilder {

  /**
   * The type of this class.
   */
  override type Self = FacebookProvider

  /**
   * The profile parser implementation.
   */
  override val profileParser = new FacebookProfileParser

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  override def withConfig(f: OAuth2Config => OAuth2Config): Self =
    new FacebookProvider(httpClient, stateHandler, clock, f(config))
}

/**
 * The companion object.
 */
object FacebookProvider {

  /**
   * The provider ID.
   */
  val ID = "facebook"

  /**
   * Default provider endpoint.
   */
  val DefaultApiURI = ConfigURI("https://graph.facebook.com/v3.1/me?fields=name,first_name,last_name,picture,email&" +
    "return_ssl_resources=1&access_token=%s")
}
