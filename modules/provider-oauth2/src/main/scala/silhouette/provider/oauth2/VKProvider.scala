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
import io.circe.{ Decoder, HCursor, Json }
import silhouette.LoginInfo
import silhouette.provider.UnexpectedResponseException
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.oauth2.VKProvider._
import silhouette.provider.social._
import sttp.client.circe.asJson
import sttp.client.{ basicRequest, SttpBackend }
import sttp.model.Uri._

/**
 * Base VK OAuth2 Provider.
 *
 * @see https://vk.com/dev/auth_sites
 * @see https://vk.com/dev/api_requests
 * @see https://vk.com/dev/users.get
 * @see https://vk.com/dev/objects/user
 *
 * @tparam F The type of the IO monad.
 */
trait BaseVKProvider[F[_]] extends OAuth2Provider[F] {

  /**
   * The provider ID.
   */
  override val id = ID

  /**
   * The implicit access token decoder.
   *
   * VK provider needs it own JSON decoder to extract the email from response.
   */
  implicit override protected val accessTokenDecoder: Decoder[OAuth2Info] = infoDecoder(clock)

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
            // The API returns a 200 status code for errors, so we must rely on the JSON here to detect an error
            json.hcursor.downField("error").focus match {
              case Some(_) =>
                F.raiseError(new UnexpectedResponseException(UnexpectedResponse.format(id, json, response.code)))
              case _ =>
                profileParser.parse(json, authInfo)
            }
        }
      }
  }
}

/**
 * The profile parser for the common social profile.
 *
 * @tparam F The type of the IO monad.
 */
class VKProfileParser[F[_]: Async] extends SocialProfileParser[F, Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): F[CommonSocialProfile] =
    json.hcursor.downField("response").downArray.focus.map(_.hcursor) match {
      case Some(response) =>
        Async[F].fromTry(response.downField("id").as[Long].getOrError(response.value, "id", ID)).map { id =>
          CommonSocialProfile(
            loginInfo = LoginInfo(ID, id.toString),
            firstName = response.downField("first_name").as[String].toOption,
            lastName = response.downField("last_name").as[String].toOption,
            email = authInfo.params.flatMap(_.get("email")),
            avatarUri = response.downField("photo_max_orig").as[String].toOption.map(uri => uri"$uri")
          )
        }
      case None =>
        Async[F].raiseError(new UnexpectedResponseException(JsonPathError.format(ID, "response", json)))

    }
}

/**
 * The VK OAuth2 Provider.
 *
 * @param clock       The current clock instance.
 * @param config      The provider config.
 * @param sttpBackend The STTP backend.
 * @param F           The IO monad type class.
 * @tparam F The type of the IO monad.
 */
class VKProvider[F[_]](
  protected val clock: Clock,
  val config: OAuth2Config
)(
  implicit
  protected val sttpBackend: SttpBackend[F, Nothing, Nothing],
  protected val F: Async[F]
) extends BaseVKProvider[F]
    with CommonProfileBuilder[F] {

  /**
   * The type of this class.
   */
  override type Self = VKProvider[F]

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
  override def withConfig(f: OAuth2Config => OAuth2Config): Self = new VKProvider(clock, f(config))
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
   * Default provider endpoint.
   */
  val DefaultApiUri = uri"https://api.vk.com/method/users.get?fields=id,first_name,last_name,photo_max_orig&v=5.85"

  /**
   * Converts the JSON into a [[OAuth2Info]] object.
   *
   * @param clock The current clock instance.
   */
  def infoDecoder(clock: Clock): Decoder[OAuth2Info] =
    (c: HCursor) => {
      for {
        accessToken <- c.downField(AccessToken).as[String]
        tokenType <- c.downField(TokenType).as[Option[String]]
        expiresIn <- c.downField(ExpiresIn).as[Option[Int]]
        refreshToken <- c.downField(RefreshToken).as[Option[String]]
        email <- c.downField("email").as[Option[String]]
      } yield OAuth2Info(
        accessToken,
        tokenType,
        Some(clock.instant()),
        expiresIn,
        refreshToken,
        email.map(e => Map("email" -> e))
      )
    }
}
