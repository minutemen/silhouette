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
import silhouette.http._
import silhouette.provider.oauth2.FoursquareProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.provider.social.state.StateHandler
import silhouette.{ ConfigURI, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Base Foursquare OAuth2 provider.
 *
 * @see https://developer.foursquare.com/overview/auth
 * @see https://developer.foursquare.com/overview/responses
 * @see https://developer.foursquare.com/docs/explore
 */
trait BaseFoursquareProvider extends OAuth2Provider {

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
    val version = config.customProperties.getOrElse(ApiVersion, DefaultApiVersion)
    httpClient.withUri(config.apiURI.getOrElse[ConfigURI](DefaultApiURI).format(authInfo.accessToken, version))
      .withMethod(Method.GET)
      .execute
      .flatMap { response =>
        withParsedJson(response.body) { json =>
          response.status match {
            case Status.OK =>
              val errorType = root.meta.errorType.string.getOption(json)
              if (errorType.contains("deprecated")) {
                logger.info("This implementation may be deprecated! Please contact the Silhouette team for a fix!")
              }

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
 *
 * @param config The provider config.
 */
class FoursquareProfileParser(config: OAuth2Config)
  extends SocialProfileParser[Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): Future[CommonSocialProfile] = Future.successful {
    val user = root.response.user.json.getOrError(json, "response.user", ID)
    val avatarURLPart1 = root.photo.prefix.string.getOption(user)
    val avatarURLPart2 = root.photo.suffix.string.getOption(user)
    val resolution = config.customProperties.getOrElse(AvatarResolution, DefaultAvatarResolution)

    CommonSocialProfile(
      loginInfo = LoginInfo(ID, root.id.string.getOrError(user, "id", ID)),
      firstName = root.firstName.string.getOption(user),
      lastName = root.lastName.string.getOption(user),
      email = root.contact.email.string.getOption(user).filter(!_.isEmpty),
      avatarUri = for (prefix <- avatarURLPart1; postfix <- avatarURLPart2)
        yield new URI(prefix + resolution + postfix)
    )
  }
}

/**
 * The Foursquare OAuth2 Provider.
 *
 * @param httpClient   The HTTP client implementation.
 * @param stateHandler The state provider implementation.
 * @param clock        The current clock instance.
 * @param config       The provider config.
 * @param ec           The execution context.
 */
class FoursquareProvider(
  protected val httpClient: HttpClient,
  protected val stateHandler: StateHandler,
  protected val clock: Clock,
  val config: OAuth2Config
)(
  implicit
  override implicit val ec: ExecutionContext
) extends BaseFoursquareProvider with CommonProfileBuilder {

  /**
   * The type of this class.
   */
  override type Self = FoursquareProvider

  /**
   * The profile parser implementation.
   */
  override val profileParser = new FoursquareProfileParser(config)

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  override def withConfig(f: OAuth2Config => OAuth2Config): Self =
    new FoursquareProvider(httpClient, stateHandler, clock, f(config))
}

/**
 * The companion object.
 */
object FoursquareProvider {

  /**
   * The provider ID.
   */
  val ID = "foursquare"

  /**
   * Default provider endpoint.
   */
  val DefaultApiURI: ConfigURI = ConfigURI("https://api.foursquare.com/v2/users/self?oauth_token=%s&v=%s")

  /**
   * The version of this implementation.
   *
   * @see https://developer.foursquare.com/overview/versioning
   */
  val DefaultApiVersion = "20181001"

  /**
   * The default avatar resolution.
   */
  val DefaultAvatarResolution = "100x100"

  /**
   * Some custom properties for this provider.
   */
  val ApiVersion = "api.version"
  val AvatarResolution = "avatar.resolution"
}
