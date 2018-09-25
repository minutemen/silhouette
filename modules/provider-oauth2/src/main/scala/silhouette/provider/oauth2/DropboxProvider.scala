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
import silhouette.http.{ Header, HttpClient, Method, Status }
import silhouette.provider.oauth2.DropboxProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.provider.social.state.StateHandler

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Base Dropbox OAuth2 Provider.
 *
 * @see https://www.dropbox.com/developers/blog/45/using-oauth-20-with-the-core-api
 * @see https://www.dropbox.com/developers/core/docs#oauth2-methods
 */
trait BaseDropboxProvider extends OAuth2Provider {

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
        withParsedJson(response.body) { json =>
          response.status match {
            case Status.OK =>
              profileParser.parse(json, authInfo)
            case status =>
              Future.failed(new ProfileRetrievalException(SpecifiedProfileError.format(
                id,
                root.error.string.getOption(json).getOrElse("undefined"),
                status,
                json
              )))
          }
        }
      }
  }
}

/**
 * The profile parser for the common social profile.
 */
class DropboxProfileParser extends SocialProfileParser[Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): Future[CommonSocialProfile] = Future.successful {
    CommonSocialProfile(
      loginInfo = LoginInfo(ID, root.uid.long.getOrError(json, "uid", ID).toString),
      firstName = root.name_details.given_name.string.getOption(json),
      lastName = root.name_details.surname.string.getOption(json),
      fullName = root.display_name.string.getOption(json)
    )
  }
}

/**
 * The Dropbox OAuth2 Provider.
 *
 * @param httpClient   The HTTP client implementation.
 * @param stateHandler The state provider implementation.
 * @param settings     The provider settings.
 * @param ec           The execution context.
 */
class DropboxProvider(
  protected val httpClient: HttpClient,
  protected val stateHandler: StateHandler,
  val settings: OAuth2Settings
)(
  implicit
  override implicit val ec: ExecutionContext
) extends BaseDropboxProvider with CommonProfileBuilder {

  /**
   * The type of this class.
   */
  override type Self = DropboxProvider

  /**
   * The profile parser implementation.
   */
  override val profileParser = new DropboxProfileParser

  /**
   * Gets a provider initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the provider initialized with new settings.
   */
  override def withSettings(f: Settings => Settings): Self =
    new DropboxProvider(httpClient, stateHandler, f(settings))
}

/**
 * The companion object.
 */
object DropboxProvider {

  /**
   * The error messages.
   */
  val SpecifiedProfileError = "[%s] Error retrieving profile information. Error message: %s, status: %s, Json: %s"

  /**
   * The provider ID.
   */
  val ID = "dropbox"

  /**
   * Default provider endpoint.
   */
  val DefaultApiUri = new URI("https://api.dropbox.com/1/account/info")
}
