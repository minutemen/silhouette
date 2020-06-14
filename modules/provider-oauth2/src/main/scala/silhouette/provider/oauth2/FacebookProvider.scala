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

import java.time.Clock

import cats.effect.Async
import cats.implicits._
import io.circe.Json
import silhouette.LoginInfo
import silhouette.provider.UnexpectedResponseException
import silhouette.provider.oauth2.FacebookProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import sttp.client.circe.asJson
import sttp.client.{ basicRequest, SttpBackend }
import sttp.model.Uri._

/**
 * Base Facebook OAuth2 Provider.
 *
 * @see https://developers.facebook.com/tools/explorer
 * @see https://developers.facebook.com/docs/graph-api/reference/user
 * @see https://developers.facebook.com/docs/facebook-login/access-tokens
 *
 * @tparam F The type of the IO monad.
 */
trait BaseFacebookProvider[F[_]] extends OAuth2Provider[F] {

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
  override protected def buildProfile(authInfo: OAuth2Info): F[Profile] = {
    val uri = config.apiUri.getOrElse(DefaultApiUri)
    basicRequest
      .get(uri.param("access_token", authInfo.accessToken))
      .response(asJson[Json])
      .send()
      .flatMap { response =>
        response.body match {
          case Left(error) =>
            F.raiseError(new UnexpectedResponseException(UnexpectedResponse.format(id, error.body, response.code)))
          case Right(json) =>
            profileParser.parse(json, authInfo)
        }
      }
  }
}

/**
 * The profile parser for the common social profile.
 *
 * @tparam F The type of the IO monad.
 */
class FacebookProfileParser[F[_]: Async] extends SocialProfileParser[F, Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): F[CommonSocialProfile] =
    Async[F].fromTry(json.hcursor.downField("id").as[String].getOrError(json, "id", ID)).map { id =>
      CommonSocialProfile(
        loginInfo = LoginInfo(ID, id),
        firstName = json.hcursor.downField("first_name").as[String].toOption,
        lastName = json.hcursor.downField("last_name").as[String].toOption,
        fullName = json.hcursor.downField("name").as[String].toOption,
        email = json.hcursor.downField("email").as[String].toOption,
        avatarUri = json.hcursor
          .downField("picture")
          .downField("data")
          .downField("url")
          .as[String]
          .toOption
          .map(uri => uri"$uri")
      )
    }
}

/**
 * The Facebook OAuth2 Provider.
 *
 * @param clock       The current clock instance.
 * @param config      The provider config.
 * @param sttpBackend The STTP backend.
 * @param F           The IO monad type class.
 * @tparam F The type of the IO monad.
 */
class FacebookProvider[F[_]](
  protected val clock: Clock,
  val config: OAuth2Config
)(
  implicit
  protected val sttpBackend: SttpBackend[F, Nothing, Nothing],
  protected val F: Async[F]
) extends BaseFacebookProvider[F]
    with CommonProfileBuilder[F] {

  /**
   * The type of this class.
   */
  override type Self = FacebookProvider[F]

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
  override def withConfig(f: OAuth2Config => OAuth2Config): Self = new FacebookProvider(clock, f(config))
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
  val DefaultApiUri =
    uri"https://graph.facebook.com/v3.1/me?fields=name,first_name,last_name,picture,email&return_ssl_resources=1"
}
