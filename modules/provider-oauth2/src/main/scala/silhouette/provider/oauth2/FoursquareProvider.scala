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
import cats.syntax.all._
import io.circe.Json
import silhouette.LoginInfo
import silhouette.provider.UnexpectedResponseException
import silhouette.provider.oauth2.FoursquareProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import sttp.client3.circe.asJson
import sttp.client3.{ basicRequest, SttpBackend }
import sttp.model.Uri._

/**
 * Base Foursquare OAuth2 provider.
 *
 * @see https://developer.foursquare.com/overview/auth
 * @see https://developer.foursquare.com/overview/responses
 * @see https://developer.foursquare.com/docs/explore
 *
 * @tparam F The type of the IO monad.
 */
trait BaseFoursquareProvider[F[_]] extends OAuth2Provider[F] {

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
      .get(uri.withParam("oauth_token", authInfo.accessToken))
      .response(asJson[Json])
      .send(sttpBackend)
      .flatMap { response =>
        response.body match {
          case Left(error) =>
            F.raiseError(
              new UnexpectedResponseException(UnexpectedResponse.format(id, error.getMessage, response.code))
            )
          case Right(json) =>
            val errorType = json.hcursor.downField("meta").downField("errorType").as[String].toOption
            if (errorType.contains("deprecated"))
              logger.info("This implementation may be deprecated! Please contact the Silhouette team for a fix!")

            profileParser.parse(json, authInfo)
        }
      }
  }
}

/**
 * The profile parser for the common social profile.
 *
 * @param config The provider config.
 * @tparam F The type of the IO monad.
 */
class FoursquareProfileParser[F[_]: Async](config: OAuth2Config)
  extends SocialProfileParser[F, Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): F[CommonSocialProfile] =
    json.hcursor.downField("response").downField("user").focus.map(_.hcursor) match {
      case Some(user) =>
        Async[F].fromTry(user.downField("id").as[String].getOrError(user.value, "id", ID)).map { id =>
          val avatarURLPart1 = user.downField("photo").downField("prefix").as[String].toOption
          val avatarURLPart2 = user.downField("photo").downField("suffix").as[String].toOption
          val resolution = config.customProperties.getOrElse(AvatarResolution, DefaultAvatarResolution)

          CommonSocialProfile(
            loginInfo = LoginInfo(ID, id),
            firstName = user.downField("firstName").as[String].toOption,
            lastName = user.downField("lastName").as[String].toOption,
            email = user.downField("contact").downField("email").as[String].toOption.filter(!_.isEmpty),
            avatarUri =
              for (prefix <- avatarURLPart1; postfix <- avatarURLPart2)
                yield uri"${prefix + resolution + postfix}"
          )
        }
      case None => Async[F].raiseError(new UnexpectedResponseException(JsonPathError.format(ID, "response.user", json)))
    }
}

/**
 * The Foursquare OAuth2 Provider.
 *
 * @param clock       The current clock instance.
 * @param config      The provider config.
 * @param sttpBackend The STTP backend.
 * @param F           The IO monad type class.
 * @tparam F The type of the IO monad.
 */
class FoursquareProvider[F[_]](
  protected val clock: Clock,
  val config: OAuth2Config
)(
  implicit
  protected val sttpBackend: SttpBackend[F, Any],
  protected val F: Async[F]
) extends BaseFoursquareProvider[F]
    with CommonProfileBuilder[F] {

  /**
   * The type of this class.
   */
  override type Self = FoursquareProvider[F]

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
  override def withConfig(f: OAuth2Config => OAuth2Config): Self = new FoursquareProvider(clock, f(config))
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
   *
   * @see https://developer.foursquare.com/overview/versioning
   */
  val DefaultApiUri = uri"https://api.foursquare.com/v2/users/self?v=20181001"

  /**
   * The default avatar resolution.
   */
  val DefaultAvatarResolution = "100x100"

  /**
   * Some custom properties for this provider.
   */
  val AvatarResolution = "avatar.resolution"
}
