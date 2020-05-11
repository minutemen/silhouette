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
import silhouette.provider.oauth2.Auth0Provider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import sttp.client.circe.asJson
import sttp.client.{ SttpBackend, basicRequest }
import sttp.model.Uri._

/**
 * Base Auth0 OAuth2 Provider.
 *
 * OAuth Provider configuration in silhouette.conf must indicate:
 *
 *   # Auth0 Service URIs
 *   auth0.authorizationUri="https://mydomain.eu.auth0.com/authorize"
 *   auth0.accessTokenUri="https://mydomain.eu.auth0.com/oauth/token"
 *   auth0.apiUri="https://mydomain.eu.auth0.com/userinfo"
 *
 *   # Application URI and credentials
 *   auth0.redirectUri="http://localhost:9000/authenticate/auth0"
 *   auth0.clientID=myoauthclientid
 *   auth0.clientSecret=myoauthclientsecret
 *
 *   # Auth0 user's profile information requested
 *   auth0.scope="openid profile email"
 *
 * See http://auth0.com for more information on the Auth0 Auth 2.0 Provider and Service.
 *
 * @tparam F The type of the IO monad.
 */
trait BaseAuth0Provider[F[_]] extends OAuth2Provider[F] {

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
    basicRequest.get(config.apiUri.getOrElse(DefaultApiUri))
      .header(BearerAuthorizationHeader(authInfo.accessToken))
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

  /**
   * Gets the access token.
   *
   * @param request The current request.
   * @param code    The access code.
   * @tparam R The type of the request.
   * @return The info containing the access token.
   */
  override protected def getAccessToken[R](request: RequestPipeline[R], code: String): F[OAuth2Info] = {
    request.queryParamValue("token_type") match {
      case Some("bearer") => F.pure(OAuth2Info(code))
      case _              => super.getAccessToken(request, code)
    }
  }
}

/**
 * The profile parser for the common social profile.
 *
 * @tparam F The type of the IO monad.
 */
class Auth0ProfileParser[F[_]: Async] extends SocialProfileParser[F, Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): F[CommonSocialProfile] = {
    Async[F].fromTry(json.hcursor.downField("sub").as[String].getOrError(json, "sub", ID)).map { id =>
      CommonSocialProfile(
        loginInfo = LoginInfo(ID, id),
        firstName = json.hcursor.downField("given_name").as[String].toOption,
        lastName = json.hcursor.downField("family_name").as[String].toOption,
        fullName = json.hcursor.downField("name").as[String].toOption,
        email = json.hcursor.downField("email").as[String].toOption,
        avatarUri = json.hcursor.downField("picture").as[String].toOption.map(uri => uri"$uri")
      )
    }
  }
}

/**
 * The Auth0 OAuth2 Provider.
 *
 * @param clock       The current clock instance.
 * @param config      The provider config.
 * @param sttpBackend The STTP backend.
 * @param F           The IO monad type class.
 * @tparam F The type of the IO monad.
 */
class Auth0Provider[F[_]](
  protected val clock: Clock,
  val config: OAuth2Config
)(
  implicit
  protected val sttpBackend: SttpBackend[F, Nothing, Nothing],
  protected val F: Async[F]
) extends BaseAuth0Provider[F] with CommonProfileBuilder[F] {

  /**
   * The type of this class.
   */
  override type Self = Auth0Provider[F]

  /**
   * The profile parser implementation.
   */
  override val profileParser = new Auth0ProfileParser

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  override def withConfig(f: OAuth2Config => OAuth2Config): Self = new Auth0Provider(clock, f(config))
}

/**
 * The companion object.
 */
object Auth0Provider {

  /**
   * The provider ID.
   */
  val ID = "auth0"

  /**
   * Default provider endpoint.
   */
  val DefaultApiUri = uri"https://auth0.auth0.com/userinfo"
}
