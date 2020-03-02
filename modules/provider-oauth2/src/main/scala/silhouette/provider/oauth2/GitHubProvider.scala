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

import cats.effect.Async
import cats.implicits._
import io.circe.Json
import silhouette.provider.UnexpectedResponseException
import silhouette.provider.oauth2.GitHubProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.{ ConfigURI, LoginInfo }
import sttp.client.circe.asJson
import sttp.client.{ SttpBackend, basicRequest }
import sttp.model.Header

/**
 * Base GitHub OAuth2 Provider.
 *
 * @see https://developer.github.com/v3/oauth/
 * @see https://developer.github.com/v3/users/
 *
 * @tparam F The type of the IO monad.
 */
trait BaseGitHubProvider[F[_]] extends OAuth2Provider[F] {

  /**
   * The provider ID.
   */
  override val id = ID

  /**
   * A list with headers to send to the API.
   *
   * Without defining the accept header, the response will take the following form:
   * access_token=e72e16c7e42f292c6912e7710c838347ae178b4a&scope=user%2Cgist&token_type=bearer
   *
   * @see https://developer.github.com/v3/oauth/#response
   */
  override protected val accessTokenHeaders = Seq(Header.accept("application/json"))

  /**
   * Builds the social profile.
   *
   * @param authInfo The auth info received from the provider.
   * @return On success the build social profile, otherwise a failure.
   */
  override protected def buildProfile(authInfo: OAuth2Info): F[Profile] = {
    val uri = config.apiURI.getOrElse(DefaultApiURI).format(authInfo.accessToken)
    basicRequest.get(uri)
      .response(asJson[Json])
      .send().flatMap { response =>
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
class GitHubProfileParser[F[_]: Async] extends SocialProfileParser[F, Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): F[CommonSocialProfile] = {
    Async[F].fromTry(json.hcursor.downField("id").as[Long].getOrError(json, "id", ID)).map { id =>
      CommonSocialProfile(
        loginInfo = LoginInfo(ID, id.toString),
        fullName = json.hcursor.downField("name").as[String].toOption,
        email = json.hcursor.downField("email").as[String].toOption,
        avatarUri = json.hcursor.downField("avatar_url").as[String].toOption.map(uri => new URI(uri))
      )
    }
  }
}

/**
 * The GitHub OAuth2 Provider.
 *
 * @param clock       The current clock instance.
 * @param config      The provider config.
 * @param sttpBackend The STTP backend.
 * @param F           The IO monad type class.
 * @tparam F The type of the IO monad.
 */
class GitHubProvider[F[_]](
  protected val clock: Clock,
  val config: OAuth2Config
)(
  implicit
  protected val sttpBackend: SttpBackend[F, Nothing, Nothing],
  protected val F: Async[F]
) extends BaseGitHubProvider[F] with CommonProfileBuilder[F] {

  /**
   * The type of this class.
   */
  override type Self = GitHubProvider[F]

  /**
   * The profile parser implementation.
   */
  override val profileParser = new GitHubProfileParser

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  override def withConfig(f: OAuth2Config => OAuth2Config): Self = new GitHubProvider(clock, f(config))
}

/**
 * The companion object.
 */
object GitHubProvider {

  /**
   * The provider ID.
   */
  val ID = "github"

  /**
   * Default provider endpoint.
   */
  val DefaultApiURI = ConfigURI("https://api.github.com/user?access_token=%s")
}
