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

import com.typesafe.scalalogging.LazyLogging
import io.circe.{ Decoder, Encoder, HCursor, Json }
import monocle.Optional
import silhouette.http._
import silhouette.http.client.Response
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social.state.handler.UserStateItemHandler
import silhouette.provider.social.state.{ StateHandler, StateItem }
import silhouette.provider.social.{ ProfileRetrievalException, SocialStateProvider, StatefulAuthInfo }
import silhouette.provider.{ AccessDeniedException, UnexpectedResponseException }
import silhouette.{ AuthInfo, ConfigURI, ConfigurationException }

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }

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
  def expired(clock: Clock): Boolean = {
    (expiresIn, createdAt) match {
      case (Some(in), Some(at)) => clock.instant().isAfter(at.plusSeconds(in.toLong))
      case _                    => false
    }
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
  def decoder(clock: Clock): Decoder[OAuth2Info] = (c: HCursor) => {
    for {
      accessToken <- c.downField(AccessToken).as[String]
      tokenType <- c.downField(TokenType).as[Option[String]]
      expiresIn <- c.downField(ExpiresIn).as[Option[Int]]
      refreshToken <- c.downField(RefreshToken).as[Option[String]]
    } yield {
      OAuth2Info(accessToken, tokenType, Some(clock.instant()), expiresIn, refreshToken)
    }
  }
}

/**
 * Base implementation for all OAuth2 providers.
 */
trait OAuth2Provider extends SocialStateProvider[OAuth2Config] with OAuth2Constants with LazyLogging {

  /**
   * The type of the auth info.
   */
  type A = OAuth2Info

  /**
   * The current clock instance.
   */
  protected val clock: Clock

  /**
   * The social state handler implementation.
   */
  protected val stateHandler: StateHandler

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
   * Starts the authentication process.
   *
   * @param request The current request.
   * @tparam R The type of the request.
   * @return Either a Result or the auth info from the provider.
   */
  def authenticate[R]()(
    implicit
    request: RequestPipeline[R]
  ): Future[Either[ResponsePipeline[SilhouetteResponse], OAuth2Info]] = {
    handleFlow(handleAuthorizationFlow(stateHandler)) { code =>
      stateHandler.unserialize(request.extractString(State).getOrElse("")).flatMap { _ =>
        getAccessToken(code).map(oauth2Info => oauth2Info)
      }
    }
  }

  /**
   * Authenticates the user and returns the auth information and the user state.
   *
   * Returns either a [[silhouette.provider.social.StatefulAuthInfo]] if all went OK or a `ResponsePipeline` that
   * the controller sends to the browser (e.g.: in the case of OAuth where the user needs to be redirected to the
   * service provider).
   *
   * @param decoder  The JSON decoder.
   * @param encoder  The JSON encoder.
   * @param request  The request.
   * @param classTag The class tag for the user state item.
   * @tparam S The type of the user state item.
   * @tparam R The type of the request.
   * @return Either a `ResponsePipeline` or the [[silhouette.provider.social.StatefulAuthInfo]] from the provider.
   */
  def authenticate[S <: StateItem, R](userState: S)(
    implicit
    decoder: Decoder[S],
    encoder: Encoder[S],
    request: RequestPipeline[R],
    classTag: ClassTag[S]
  ): Future[Either[ResponsePipeline[SilhouetteResponse], StatefulAuthInfo[A, S]]] = {
    val userStateItemHandler = new UserStateItemHandler(userState)
    val newStateHandler = stateHandler.withHandler(userStateItemHandler)

    handleFlow(handleAuthorizationFlow(newStateHandler)) { code =>
      newStateHandler.unserialize(request.extractString(State).getOrElse("")).flatMap { state =>
        val maybeUserState: Option[S] = state.items.flatMap(item => userStateItemHandler.canHandle(item)).headOption
        maybeUserState match {
          case Some(s) => getAccessToken(code).map(oauth2Info => StatefulAuthInfo(oauth2Info, s))
          case None    => Future.failed(new UnexpectedResponseException("Cannot extract user info from response"))
        }
      }
    }
  }

  /**
   * Refreshes the access token.
   *
   * @param refreshToken The refresh token.
   * @return The auth info containing the access token.
   */
  def refresh(refreshToken: String): Future[OAuth2Info] = {
    config.refreshURI match {
      case Some(uri) =>
        val params = Map(
          GrantType -> Seq(RefreshToken),
          RefreshToken -> Seq(refreshToken)
        ) ++
          config.refreshParams.mapValues(Seq(_)) ++
          config.scope.map(scope => Map(Scope -> Seq(scope))).getOrElse(Map())

        httpClient
          .withUri(uri)
          .withHeaders(authorizationHeader)
          .withHeaders(refreshHeaders: _*)
          .withMethod(Method.POST)
          .withBody(Body.from(params))
          .execute
          .flatMap { response =>
            logger.debug("[%s] Access token response: [%s]".format(id, response.body.raw))
            Future.fromTry(buildInfo(response))
          }
      case None =>
        Future.failed(new ConfigurationException(RefreshUriUndefined.format(id)))
    }
  }

  /**
   * Handles the OAuth2 flow.
   *
   * The left flow is the authorization flow, which will be processed, if no `code` parameter exists
   * in the request. The right flow is the access token flow, which will be executed after a successful
   * authorization.
   *
   * @param left    The authorization flow.
   * @param right   The access token flow.
   * @param request The request.
   * @tparam LF The return type of the left flow.
   * @tparam RF The return type of the right flow.
   * @tparam R The type of the request.
   * @return Either the left or the right flow.
   */
  protected def handleFlow[LF, RF, R](left: => Future[LF])(right: String => Future[RF])(
    implicit
    request: RequestPipeline[R]
  ): Future[Either[LF, RF]] = {
    request.extractString(Error).map {
      case e @ AccessDenied => new AccessDeniedException(AuthorizationError.format(id, e))
      case e                => new UnexpectedResponseException(AuthorizationError.format(id, e))
    } match {
      case Some(throwable) => Future.failed(throwable)
      case None => request.extractString(Code) match {
        // We're being redirected back from the authorization server with the access code and the state
        case Some(code) => right(code).map(Right.apply)
        // There's no code in the request, this is the first step in the OAuth flow
        case None       => left.map(Left.apply)
      }
    }
  }

  /**
   * Handles the authorization step of the OAuth2 flow.
   *
   * @param stateHandler The state handler to use.
   * @param request      The request.
   * @tparam R The type of the request.
   * @return The redirect to the authorization URI of the OAuth2 provider.
   */
  protected def handleAuthorizationFlow[R](stateHandler: StateHandler)(
    implicit
    request: RequestPipeline[R]
  ): Future[ResponsePipeline[SilhouetteResponse]] = {
    stateHandler.state.map { state =>
      val params = List(
        Some(ClientID -> config.clientID),
        Some(ResponseType -> Code),
        stateHandler.serialize(state).map(State -> _),
        config.scope.map(Scope -> _),
        config.redirectURI.map(uri => RedirectUri -> resolveCallbackUri(uri).toString)
      ).flatten ++ config.authorizationParams.toList

      val encodedParams = params.map { p => encode(p._1, "UTF-8") + "=" + encode(p._2, "UTF-8") }
      val uri = config.authorizationURI.getOrElse {
        throw new ConfigurationException(AuthorizationUriUndefined.format(id))
      } + encodedParams.mkString("?", "&", "")
      val redirectResponse = SilhouetteResponse(Status.`See Other`)
      val redirectResponsePipeline = SilhouetteResponsePipeline(redirectResponse)
        .withHeaders(Header(Header.Name.Location, uri))
      val redirect = stateHandler.publish(redirectResponsePipeline, state)
      logger.debug("[%s] Use authorization URI: %s".format(id, config.authorizationURI))
      logger.debug("[%s] Redirecting to: %s".format(id, uri))
      redirect
    }
  }

  /**
   * Gets the access token.
   *
   * @param code    The access code.
   * @param request The current request.
   * @tparam R The type of the request.
   * @return The auth info containing the access token.
   */
  protected def getAccessToken[R](code: String)(implicit request: RequestPipeline[R]): Future[OAuth2Info] = {
    val params = Map(
      GrantType -> Seq(AuthorizationCode),
      Code -> Seq(code)
    ) ++
      config.accessTokenParams.mapValues(Seq(_)) ++
      config.redirectURI.map(uri => Map(RedirectUri -> Seq(resolveCallbackUri(uri).toString))).getOrElse(Map())

    httpClient
      .withUri(config.accessTokenURI)
      .withHeaders(authorizationHeader)
      .withHeaders(accessTokenHeaders: _*)
      .withMethod(Method.POST)
      .withBody(Body.from(params))
      .execute
      .flatMap { response =>
        logger.debug("[%s] Access token response: [%s]".format(id, response.body.raw))
        Future.fromTry(buildInfo(response))
      }
  }

  /**
   * Builds the OAuth2 info from response.
   *
   * @param response The response from the provider.
   * @return The OAuth2 info on success, otherwise a failure.
   * @see https://tools.ietf.org/html/rfc6749#section-5.1
   */
  protected def buildInfo(response: Response): Try[OAuth2Info] = {
    import BodyReads.circeJsonReads
    response.status match {
      case Status.OK =>
        response.body.as[Json] match {
          case Success(json) => json.as[OAuth2Info].fold(
            error => Failure(new UnexpectedResponseException(InvalidInfoFormat.format(id), Some(error))),
            info => Success(info)
          )
          case Failure(error) =>
            Failure(new UnexpectedResponseException(JsonParseError.format(id, response.body.raw), Some(error)))
        }
      case status =>
        Failure(new UnexpectedResponseException(UnexpectedResponse.format(id, response.body.raw, status)))
    }
  }

  /**
   * Helper that executes the builder code with the parsed JSON body.
   *
   * @param body    The body to parse as JSON.
   * @param builder The profile builder block that parses the profile from the given JSON.
   * @return The parsed profile.
   */
  protected def withParsedJson(body: Body)(builder: Json => Future[Profile]): Future[Profile] = {
    body.as[Json] match {
      case Failure(error) => Future.failed(
        new ProfileRetrievalException(JsonParseError.format(id, body.raw), Some(error))
      )
      case Success(json) => builder(json)
    }
  }
}

/**
 * The OAuth2Provider companion object.
 */
object OAuth2Provider extends OAuth2Constants {

  /**
   * Monkey patches a `Optional[Json, T]` instance.
   *
   * @param optional The instance to patch.
   * @tparam T The type of the result.
   */
  implicit class RichMonocleOptional[T](optional: Optional[Json, T]) {
    def getOrError(json: Json, path: String, id: String): T = optional.getOption(json).getOrElse(
      throw new ProfileRetrievalException(JsonPathError.format(id, path, json))
    )
  }

  /**
   * The error messages.
   */
  val AuthorizationUriUndefined = "[%s] Authorization URI is undefined"
  val RefreshUriUndefined = "[%s] Refresh URI is undefined"
  val AuthorizationError = "[%s] Authorization server returned error: %s"
  val InvalidInfoFormat = "[%s] Cannot build OAuth2Info because of invalid response format"
  val JsonParseError = "[%s] Cannot parse response `%s` to Json"
  val JsonPathError = "[%s] Cannot access json path `%s` from Json: %s"
  val UnexpectedResponse = "[%s] Got unexpected response `%s`; status: %s"
  val SpecifiedProfileError = "[%s] Error retrieving profile information. Status: %s, Json: %s"
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
 * @param authorizationURI    The authorization URI provided by the OAuth provider.
 * @param accessTokenURI      The access token URI provided by the OAuth provider.
 * @param redirectURI         The redirect URI to the application after a successful authentication on the OAuth
 *                            provider. The URI can be a relative path which will be resolved against the current
 *                            request's host.
 * @param apiURI              The URI to fetch the profile from the API. Can be used to override the default URI
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
  authorizationURI: Option[ConfigURI] = None,
  accessTokenURI: ConfigURI,
  redirectURI: Option[ConfigURI] = None,
  refreshURI: Option[ConfigURI] = None,
  apiURI: Option[ConfigURI] = None,
  clientID: String, clientSecret: String,
  scope: Option[String] = None,
  authorizationParams: Map[String, String] = Map.empty,
  accessTokenParams: Map[String, String] = Map.empty,
  refreshParams: Map[String, String] = Map.empty,
  customProperties: Map[String, String] = Map.empty
)
