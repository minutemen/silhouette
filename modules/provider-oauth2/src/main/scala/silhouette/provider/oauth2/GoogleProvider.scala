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
import silhouette.http.{ HttpClient, Method, Status }
import silhouette.provider.oauth2.GoogleProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.provider.social.state.StateHandler
import silhouette.{ ConfigURI, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Base Google OAuth2 Provider.
 *
 * @see https://developers.google.com/+/api/auth-migration#timetable
 * @see https://developers.google.com/+/api/auth-migration#oauth2login
 * @see https://developers.google.com/accounts/docs/OAuth2Login
 * @see https://developers.google.com/+/api/latest/people
 */
trait BaseGoogleProvider extends OAuth2Provider {

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
    httpClient.withUri(config.apiUri.getOrElse(DefaultApiUri).format(authInfo.accessToken))
      .withMethod(Method.GET)
      .execute
      .flatMap { response =>
        withParsedJson(response.body) { json =>
          response.status match {
            case Status.OK =>
              profileParser.parse(json, authInfo)
            case status =>
              Future.failed(new ProfileRetrievalException(SpecifiedProfileError.format(id, status, json)))
          }
        }
      }
  }
}

/**
 * The profile parser for the common social profile.
 */
class GoogleProfileParser extends SocialProfileParser[Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): Future[CommonSocialProfile] = Future.successful {
    val avatarURL = root.image.url.string.getOption(json).map(uri => new URI(uri))
    val isDefaultAvatar = root.image.isDefault.boolean.getOption(json).getOrElse(false)

    // https://developers.google.com/+/api/latest/people#emails.type
    val email = root.emails.each.filter(root.`type`.string.exist(_ == "account")).value.string.getAll(json).headOption

    CommonSocialProfile(
      loginInfo = LoginInfo(ID, root.id.string.getOrError(json, "id", ID)),
      firstName = root.name.givenName.string.getOption(json),
      lastName = root.name.familyName.string.getOption(json),
      fullName = root.displayName.string.getOption(json),
      email = email,
      avatarUri = if (isDefaultAvatar) None else avatarURL // skip the default avatar picture
    )
  }
}

/**
 * The Google OAuth2 Provider.
 *
 * @param httpClient   The HTTP client implementation.
 * @param stateHandler The state provider implementation.
 * @param config       The provider config.
 * @param ec           The execution context.
 */
class GoogleProvider(
  protected val httpClient: HttpClient,
  protected val stateHandler: StateHandler,
  val config: OAuth2Config
)(
  implicit
  override implicit val ec: ExecutionContext
) extends BaseGoogleProvider with CommonProfileBuilder {

  /**
   * The type of this class.
   */
  override type Self = GoogleProvider

  /**
   * The profile parser implementation.
   */
  override val profileParser = new GoogleProfileParser

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  override def withConfig(f: OAuth2Config => OAuth2Config): Self =
    new GoogleProvider(httpClient, stateHandler, f(config))
}

/**
 * The companion object.
 */
object GoogleProvider {

  /**
   * The provider ID.
   */
  val ID = "google"

  /**
   * Default provider endpoint.
   */
  val DefaultApiUri = ConfigURI("https://www.googleapis.com/plus/v1/people/me?access_token=%s")
}
