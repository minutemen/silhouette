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

import io.circe.optics.JsonPath._
import io.circe.{ Decoder, HCursor, Json }
import silhouette.http.{ HttpClient, Method }
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.oauth2.VKProvider._
import silhouette.provider.social._
import silhouette.provider.social.state.StateHandler
import silhouette.{ ConfigURI, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Base VK OAuth2 Provider.
 *
 * @see https://vk.com/dev/auth_sites
 * @see https://vk.com/dev/api_requests
 * @see https://vk.com/dev/users.get
 * @see https://vk.com/dev/objects/user
 */
trait BaseVKProvider extends OAuth2Provider {

  /**
   * The provider ID.
   */
  override val id = ID

  /**
   * The implicit access token decoder.
   *
   * VK provider needs it own JSON decoder to extract the email from response.
   */
  override implicit protected val accessTokenDecoder: Decoder[OAuth2Info] = VKProvider.infoDecoder

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
          // The API returns a 200 status code for errors, so we must rely on the JSON here to detect an error
          root.error.json.getOption(json) match {
            case Some(_) =>
              Future.failed(new ProfileRetrievalException(SpecifiedProfileError.format(id, response.status, json)))
            case _ =>
              profileParser.parse(json, authInfo)
          }
        }
      }
  }
}

/**
 * The profile parser for the common social profile.
 */
class VKProfileParser extends SocialProfileParser[Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): Future[CommonSocialProfile] = {
    root.response.each.json.getAll(json).headOption match {
      case Some(response) => Future.successful {
        CommonSocialProfile(
          loginInfo = LoginInfo(ID, root.id.long.getOrError(response, "id", ID).toString),
          firstName = root.first_name.string.getOption(response),
          lastName = root.last_name.string.getOption(response),
          email = authInfo.params.flatMap(_.get("email")),
          avatarUri = root.photo_max_orig.string.getOption(response).map(uri => new URI(uri))
        )
      }
      case None =>
        Future.failed(new ProfileRetrievalException(JsonPathError.format(ID, "response", json)))

    }
  }
}

/**
 * The VK OAuth2 Provider.
 *
 * @param httpClient   The HTTP client implementation.
 * @param stateHandler The state provider implementation.
 * @param config       The provider config.
 * @param ec           The execution context.
 */
class VKProvider(
  protected val httpClient: HttpClient,
  protected val stateHandler: StateHandler,
  val config: OAuth2Config
)(
  implicit
  override implicit val ec: ExecutionContext
) extends BaseVKProvider with CommonProfileBuilder {

  /**
   * The type of this class.
   */
  override type Self = VKProvider

  /**
   * The profile parser implementation.
   */
  override val profileParser = new VKProfileParser

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  override def withConfig(f: OAuth2Config => OAuth2Config): Self =
    new VKProvider(httpClient, stateHandler, f(config))
}

/**
 * The companion object.
 */
object VKProvider {

  /**
   * The provider ID.
   */
  val ID = "vk"

  /**
   * The used API version.
   */
  val ApiVersion = "5.85"

  /**
   * Default provider endpoint.
   */
  val DefaultApiUri = ConfigURI("https://api.vk.com/method/users.get?fields=id,first_name,last_name," +
    s"photo_max_orig&v=$ApiVersion&access_token=%s")

  /**
   * Converts the JSON into a [[OAuth2Info]] object.
   */
  implicit val infoDecoder: Decoder[OAuth2Info] = (c: HCursor) => {
    for {
      accessToken <- c.downField(AccessToken).as[String]
      tokenType <- c.downField(TokenType).as[Option[String]]
      expiresIn <- c.downField(ExpiresIn).as[Option[Int]]
      refreshToken <- c.downField(RefreshToken).as[Option[String]]
      email <- c.downField("email").as[Option[String]]
    } yield {
      OAuth2Info(accessToken, tokenType, expiresIn, refreshToken, email.map(e => Map("email" -> e)))
    }
  }
}
