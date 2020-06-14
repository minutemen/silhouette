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
import silhouette.http._
import silhouette.provider.UnexpectedResponseException
import silhouette.provider.oauth2.DropboxProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import sttp.client.circe.asJson
import sttp.client.{ basicRequest, SttpBackend }
import sttp.model.Uri._

/**
 * Base Dropbox OAuth2 Provider.
 *
 * @see https://www.dropbox.com/developers/blog/45/using-oauth-20-with-the-core-api
 * @see https://www.dropbox.com/developers/core/docs#oauth2-methods
 *
 * @tparam F The type of the IO monad.
 */
trait BaseDropboxProvider[F[_]] extends OAuth2Provider[F] {

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
  override protected def buildProfile(authInfo: OAuth2Info): F[Profile] =
    basicRequest
      .get(config.apiUri.getOrElse(DefaultApiUri))
      .header(BearerAuthorizationHeader(authInfo.accessToken))
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

/**
 * The profile parser for the common social profile.
 *
 * @tparam F The type of the IO monad.
 */
class DropboxProfileParser[F[_]: Async] extends SocialProfileParser[F, Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): F[CommonSocialProfile] =
    Async[F].fromTry(json.hcursor.downField("uid").as[Long].getOrError(json, "uid", ID)).map { id =>
      CommonSocialProfile(
        loginInfo = LoginInfo(ID, id.toString),
        firstName = json.hcursor.downField("name_details").downField("given_name").as[String].toOption,
        lastName = json.hcursor.downField("name_details").downField("surname").as[String].toOption,
        fullName = json.hcursor.downField("display_name").as[String].toOption
      )
    }
}

/**
 * The Dropbox OAuth2 Provider.
 *
 * @param clock       The current clock instance.
 * @param config      The provider config.
 * @param sttpBackend The STTP backend.
 * @param F           The IO monad type class.
 * @tparam F The type of the IO monad.
 */
class DropboxProvider[F[_]](
  protected val clock: Clock,
  val config: OAuth2Config
)(
  implicit
  protected val sttpBackend: SttpBackend[F, Nothing, Nothing],
  protected val F: Async[F]
) extends BaseDropboxProvider[F]
    with CommonProfileBuilder[F] {

  /**
   * The type of this class.
   */
  override type Self = DropboxProvider[F]

  /**
   * The profile parser implementation.
   */
  override val profileParser = new DropboxProfileParser

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  override def withConfig(f: OAuth2Config => OAuth2Config): Self = new DropboxProvider(clock, f(config))
}

/**
 * The companion object.
 */
object DropboxProvider {

  /**
   * The provider ID.
   */
  val ID = "dropbox"

  /**
   * Default provider endpoint.
   */
  val DefaultApiUri = uri"https://api.dropbox.com/1/account/info"
}
