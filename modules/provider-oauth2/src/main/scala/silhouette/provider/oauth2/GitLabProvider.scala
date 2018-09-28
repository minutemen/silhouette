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
import silhouette.provider.oauth2.GitLabProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.provider.social.state.StateHandler
import silhouette.{ ConfigURI, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Base GitLab OAuth2 Provider.
 *
 * @see https://gitlab.com/help/api/oauth2.md
 * @see https://gitlab.com/help/integration/oauth_provider.md
 * @see https://gitlab.com/help/api/README.md#status-codes
 * @see https://gitlab.com/help/api/users.md
 */
trait BaseGitLabProvider extends OAuth2Provider {

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
class GitLabProfileParser extends SocialProfileParser[Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): Future[CommonSocialProfile] = Future.successful {
    CommonSocialProfile(
      loginInfo = LoginInfo(ID, root.id.long.getOrError(json, "id", ID).toString),
      fullName = root.name.string.getOption(json),
      email = root.email.string.getOption(json),
      avatarUri = root.avatar_url.string.getOption(json).map(uri => new URI(uri))
    )
  }
}

/**
 * The GitLab OAuth2 Provider.
 *
 * @param httpClient   The HTTP client implementation.
 * @param stateHandler The state provider implementation.
 * @param config       The provider config.
 * @param ec           The execution context.
 */
class GitLabProvider(
  protected val httpClient: HttpClient,
  protected val stateHandler: StateHandler,
  val config: OAuth2Config
)(
  implicit
  override implicit val ec: ExecutionContext
) extends BaseGitLabProvider with CommonProfileBuilder {

  /**
   * The type of this class.
   */
  override type Self = GitLabProvider

  /**
   * The profile parser implementation.
   */
  override val profileParser = new GitLabProfileParser

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  override def withConfig(f: OAuth2Config => OAuth2Config): Self =
    new GitLabProvider(httpClient, stateHandler, f(config))
}

/**
 * The companion object.
 */
object GitLabProvider {

  /**
   * The provider ID.
   */
  val ID = "gitlab"

  /**
   * Default provider endpoint.
   */
  val DefaultApiUri = ConfigURI("https://gitlab.com/api/v4/user?access_token=%s")
}
