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

import java.net.URLEncoder._
import java.time.{ Clock, Instant }

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.{ Decoder, HCursor, Json }
import silhouette.http._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social.state.StateHandler
import silhouette.provider.social.{ SocialStateProvider, StatefulAuthInfo }
import silhouette.provider.{ AccessDeniedException, UnexpectedResponseException }
import silhouette.{ AuthInfo, ConfigurationException }
import sttp.client._
import sttp.client.circe._
import sttp.model.{ Header, HeaderNames, StatusCode, Uri }

import scala.util.{ Failure, Try }

/**
 * The OAuth2 info.
 *
 * @param accessToken  The access token.
 * @param tokenType    The access token type.
 * @param createdAt    The time the OAuth2 info instance was created.
 * @param expiresIn    The number of seconds before the access token expires.
 * @param refreshToken The refresh token.
 * @param params       Additional params transported in conjunction with the token.
 */
case class OAuth2Info(
  accessToken: String,
  tokenType: Option[String] = None,
  createdAt: Option[Instant] = None,
  expiresIn: Option[Int] = None,
  refreshToken: Option[String] = None,
  params: Option[Map[String, String]] = None
) extends AuthInfo {

  /**
   * Checks if the access token is expired and needs to be refreshed.
   *
   * @param clock The clock to check against.
   */
  def expired(clock: Clock): Boolean =
    (expiresIn, createdAt) match {
      case (Some(in), Some(at)) => clock.instant().isAfter(at.plusSeconds(in.toLong))
      case _                    => false
    }
}

/**
 * The Oauth2 info companion object.
 */
object OAuth2Info extends OAuth2Constants {

  /**
   * Converts the JSON into a [[OAuth2Info]] object.
   *
   * @param clock The current clock instance.
   * @see https://tools.ietf.org/html/rfc6749#section-5.1
   */
  def decoder(clock: Clock): Decoder[OAuth2Info] =
    (c: HCursor) => {
      for {
        accessToken <- c.downField(AccessToken).as[String]
        tokenType <- c.downField(TokenType).as[Option[String]]
        expiresIn <- c.downField(ExpiresIn).as[Option[Int]]
        refreshToken <- c.downField(RefreshToken).as[Option[String]]
      } yield OAuth2Info(accessToken, tokenType, Some(clock.instant()), expiresIn, refreshToken)
    }
}

/**
 * Base implementation for all OAuth2 providers.
 *
 * @tparam F The type of the IO monad.
 */
trait OAuth2Provider[F[_]] extends SocialStateProvider[F, OAuth2Config] with OAuth2Constants with LazyLogging {

  /**
   * The type of the auth info.
   */
  type A = OAuth2Info

  /**
   * The current clock instance.
   */
  protected val clock: Clock

  /**
   * A list with headers to send to the API to get the access token.
   *
   * Override this if a specific provider uses additional headers to send with the access token request.
   */
  protected val accessTokenHeaders: Seq[Header] = Seq()

  /**
   * A list with headers to send to the API to refresh the access token.
   *
   * Override this if a specific provider uses additional headers to send with the access token request.
   */
  protected val refreshHeaders: Seq[Header] = Seq()

  /**
   * Gets the authorization header.
   *
   * @return The authorization header.
   * @see https://tools.ietf.org/html/rfc6749#section-2.3.1
   */
  protected val authorizationHeader: Header = BasicAuthorizationHeader(
    BasicCredentials(encode(config.clientID, "UTF-8"), encode(config.clientSecret, "UTF-8"))
  )

  /**
   * The implicit access token decoder.
   *
   * Override this if a specific provider needs another decoder.
   */
  implicit protected val accessTokenDecoder: Decoder[OAuth2Info] = OAuth2Info.decoder(clock)

  /**
   * The implicit STTP backend.
   */
  implicit protected val sttpBackend: SttpBackend[F, Nothing, Nothing]

  /**
   * Starts the authentication process.
   *
   * @param request The current request.
   * @tparam R The type of the request.
   * @return Either the [[silhouette.http.ResponsePipeline]] on the left or the [[AuthInfo]] from the provider on
   *         the right.
   */
  def authenticate[R](request: RequestPipeline[R]): F[Either[ResponsePipeline[SilhouetteResponse], A]] =
    handleFlow(request)(handleAuthorizationFlow(request, None))(code => getAccessToken(request, code))

  /**
   * Authenticates the user and returns the auth information and the state params passed to the provider.
   *
   * Returns either a [[silhouette.provider.social.StatefulAuthInfo]] if all went OK or a
   * [[silhouette.http.ResponsePipeline]] that the controller sends to the browser (e.g.: in the case of OAuth where
   * the user needs to be redirected to the service provider).
   *
   * @param request      The request pipeline.
   * @param stateHandler The state handler instance which handles the state serialization/deserialization.
   * @tparam R The type of the request.
   * @return Either the [[silhouette.http.ResponsePipeline]] on the left or the
   *         [[silhouette.provider.social.StatefulAuthInfo]] from the provider on the right.
   */
  def authenticate[R](
    request: RequestPipeline[R],
    stateHandler: StateHandler[F]
  ): F[Either[ResponsePipeline[SilhouetteResponse], StatefulAuthInfo[A]]] =
    stateHandler.serialize[SilhouetteResponse].flatMap { case (state, responseWriter) =>
      handleFlow(request) {
        handleAuthorizationFlow(request, Some(state))
      } { code =>
        getAccessToken(request, code)
      }.flatMap {
        case Left(response) =>
          F.pure(Left(responseWriter(response)))
        case Right(oAuth2Info) =>
          stateHandler.unserialize(request.extractString(State).getOrElse(""), request).map { state =>
            Right(StatefulAuthInfo(oAuth2Info, state))
          }
      }
    }

  /**
   * Refreshes the access token.
   *
   * @param refreshToken The refresh token.
   * @return An error on failure or the auth info containing the access token on success.
   */
  def refresh(refreshToken: String): F[OAuth2Info] =
    config.refreshUri match {
      case Some(uri) =>
        val params = Map(
          GrantType -> RefreshToken,
          RefreshToken -> refreshToken
        ) ++
          config.refreshParams.map { case (key, value) => key -> value } ++
          config.scope.map(scope => Map(Scope -> scope)).getOrElse(Map())

        basicRequest
          .post(uri)
          .headers(authorizationHeader)
          .headers(refreshHeaders: _*)
          .body(params)
          .response(asJson[OAuth2Info])
          .send()
          .flatMap { response =>
            response.body match {
              case Left(error) =>
                F.raiseError(new UnexpectedResponseException(UnexpectedResponse.format(id, error.body, response.code)))
              case Right(info) =>
                logger.debug("[%s] Access token response: [%s]".format(id, response))
                F.pure(info)
            }
          }
      case None =>
        F.raiseError(new ConfigurationException(RefreshUriUndefined.format(id)))
    }

  /**
   * Handles the OAuth2 flow.
   *
   * The left flow is the authorization flow, which will be processed, if no `code` parameter exists
   * in the request. The right flow is the access token flow, which will be executed after a successful
   * authorization.
   *
   * @param request The request.
   * @param left    The authorization flow.
   * @param right   The access token flow.
   * @tparam R The type of the request.
   * @tparam LF The return type of the left flow.
   * @tparam RF The return type of the right flow.
   * @return Either the result of the left or the right flow.
   */
  protected def handleFlow[R, LF, RF](request: RequestPipeline[R])(
    left: => Either[Throwable, LF]
  )(
    right: String => F[RF]
  ): F[Either[LF, RF]] =
    request.extractString(Error).map {
      case e @ AccessDenied => new AccessDeniedException(AuthorizationError.format(id, e))
      case e                => new UnexpectedResponseException(AuthorizationError.format(id, e))
    } match {
      case Some(exception) => F.raiseError(exception)
      case None =>
        request.extractString(Code) match {
          // We're being redirected back from the authorization server with the access code and the state
          case Some(code) => F.map(right(code))(Right.apply)
          // There's no code in the request, this is the first step in the OAuth flow
          case None => F.fromEither(left).map(Left.apply)
        }
    }

  /**
   * Handles the authorization step of the OAuth2 flow.
   *
   * @param request    The request pipeline.
   * @param maybeState Maybe the state to pass to the provider.
   * @tparam R The type of the request.
   * @return The redirect to the authorization URI of the OAuth2 provider.
   */
  protected def handleAuthorizationFlow[R](
    request: RequestPipeline[R],
    maybeState: Option[String] = None
  ): Either[Throwable, ResponsePipeline[SilhouetteResponse]] =
    config.authorizationUri match {
      case Some(authorizationURI) =>
        val params = List(
          Some(ClientID -> config.clientID),
          Some(ResponseType -> Code),
          maybeState.map(State -> _),
          config.scope.map(Scope -> _),
          config.redirectUri.map(uri => RedirectUri -> resolveCallbackUri(uri, request).toString)
        ).flatten.toMap ++ config.authorizationParams

        val uri = uri"$authorizationURI?$params"
        logger.debug(
          "[%s] Use authorization URI: %s".format(
            id,
            config.authorizationUri.map(_.toString()).getOrElse("")
          )
        )
        logger.debug("[%s] Redirecting to: %s".format(id, uri))
        Right(
          SilhouetteResponsePipeline(SilhouetteResponse(StatusCode.SeeOther))
            .withHeaders(Header(HeaderNames.Location, uri.toString()))
        )
      case None => Left(new ConfigurationException(AuthorizationUriUndefined.format(id)))
    }

  /**
   * Gets the access token.
   *
   * @param request The current request.
   * @param code    The access code.
   * @tparam R The type of the request.
   * @return An error on failure or the auth info containing the access token on success.
   */
  protected def getAccessToken[R](request: RequestPipeline[R], code: String): F[OAuth2Info] = {
    val params = Map(
      GrantType -> AuthorizationCode,
      Code -> code
    ) ++
      config.accessTokenParams.map { case (key, value) => key -> value } ++
      config.redirectUri.map(uri => Map(RedirectUri -> resolveCallbackUri(uri, request).toString)).getOrElse(Map())

    basicRequest
      .post(config.accessTokenUri)
      .headers(authorizationHeader)
      .headers(accessTokenHeaders: _*)
      .body(params)
      .response(asJson[OAuth2Info])
      .send()
      .flatMap { response =>
        response.body match {
          case Left(error) =>
            F.raiseError(new UnexpectedResponseException(UnexpectedResponse.format(id, error.body, response.code)))
          case Right(info) =>
            logger.debug("[%s] Access token response: [%s]".format(id, response))
            F.pure(info)
        }
      }
  }
}

/**
 * The OAuth2Provider companion object.
 */
object OAuth2Provider extends OAuth2Constants {

  /**
   * Monkey patches a `Decoder.Result[T]` instance.
   *
   * @param result The instance to patch.
   * @tparam T The type of the result.
   */
  implicit class RichDecoderResult[T](result: Decoder.Result[T]) {
    def getOrError(json: Json, path: String, id: String): Try[T] =
      result.toTry.recoverWith { case e: Exception =>
        Failure(new UnexpectedResponseException(JsonPathError.format(id, path, json), Some(e)))
      }
  }

  /**
   * The error messages.
   */
  val AuthorizationUriUndefined = "[%s] Authorization URI is undefined"
  val RefreshUriUndefined = "[%s] Refresh URI is undefined"
  val AuthorizationError = "[%s] Authorization server returned error: %s"
  val JsonPathError = "[%s] Cannot access json path `%s` in Json: %s"
  val UnexpectedResponse = "[%s] Got unexpected response `%s`; status: %s"
}

/**
 * The OAuth2 constants.
 */
trait OAuth2Constants {
  val ClientID = "client_id"
  val ClientSecret = "client_secret"
  val RedirectUri = "redirect_uri"
  val Scope = "scope"
  val ResponseType = "response_type"
  val State = "state"
  val GrantType = "grant_type"
  val AuthorizationCode = "authorization_code"
  val AccessToken = "access_token"
  val RefreshToken = "refresh_token"
  val Error = "error"
  val Code = "code"
  val TokenType = "token_type"
  val ExpiresIn = "expires_in"
  val Expires = "expires"
  val AccessDenied = "access_denied"
}

/**
 * The OAuth2 configuration.
 *
 * @param authorizationUri    The authorization URI provided by the OAuth provider.
 * @param accessTokenUri      The access token URI provided by the OAuth provider.
 * @param redirectUri         The redirect URI to the application after a successful authentication on the OAuth
 *                            provider. The URI can be a relative path which will be resolved against the current
 *                            request's host.
 * @param apiUri              The URI to fetch the profile from the API. Can be used to override the default URI
 *                            hardcoded in every provider implementation.
 * @param clientID            The client ID provided by the OAuth provider.
 * @param clientSecret        The client secret provided by the OAuth provider.
 * @param scope               The OAuth2 scope parameter provided by the OAuth provider.
 * @param authorizationParams Additional params to add to the authorization request.
 * @param accessTokenParams   Additional params to add to the initial access token request.
 * @param refreshParams       Additional params to add to the access token refresh request.
 * @param customProperties    A map of custom properties for the different providers.
 */
case class OAuth2Config(
  authorizationUri: Option[Uri] = None,
  accessTokenUri: Uri,
  redirectUri: Option[Uri] = None,
  refreshUri: Option[Uri] = None,
  apiUri: Option[Uri] = None,
  clientID: String,
  clientSecret: String,
  scope: Option[String] = None,
  authorizationParams: Map[String, String] = Map.empty,
  accessTokenParams: Map[String, String] = Map.empty,
  refreshParams: Map[String, String] = Map.empty,
  customProperties: Map[String, String] = Map.empty
)
