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

import io.circe.{ Decoder, Json }
import org.specs2.execute.Result
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import silhouette.http.Body._
import silhouette.http.BodyWriter._
import silhouette.http._
import silhouette.http.client.{ Request, Response }
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.provider.social.state.handler.UserStateItem
import silhouette.provider.social.state.{ State, StateHandler, StateItem, StateItemHandler }
import silhouette.provider.{ AccessDeniedException, UnexpectedResponseException }
import silhouette.specs2.BaseFixture
import silhouette.{ ConfigURI, ConfigurationException }

import scala.concurrent.Future
import scala.io.Codec

/**
 * Abstract test case for the [[OAuth2Provider]] class.
 *
 * These tests will be additionally executed before every OAuth2 provider spec.
 */
abstract class OAuth2ProviderSpec extends SocialStateProviderSpec[OAuth2Info, StateItem] {
  isolated

  "The `authenticate` method" should {
    implicit val c: BaseContext = context
    "fail with an AccessDeniedException if `error` key with value `access_denied` exists in query string" in {
      val request = Fake.request.withQueryParams(Error -> AccessDenied)
      failed[AccessDeniedException](c.provider.authenticate()(request)) {
        case e => e.getMessage must startWith(AuthorizationError.format(c.provider.id, ""))
      }
    }

    "fail with an UnexpectedResponseException if `error` key with unspecified value exists in query string" in {
      val request = Fake.request.withQueryParams(Error -> "unspecified")
      failed[UnexpectedResponseException](c.provider.authenticate()(request)) {
        case e => e.getMessage must startWith(AuthorizationError.format(c.provider.id, "unspecified"))
      }
    }

    "fail with an ConfigurationException if authorization URI is undefined when it's needed" in {
      c.config.authorizationURI match {
        case None =>
          skipped("authorizationURI is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(_) =>
          c.stateHandler.serialize(c.state) returns Some("value")
          c.stateHandler.unserialize(anyString)(any[Fake.RequestPipeline]()) returns Future.successful(c.state)
          c.stateHandler.state returns Future.successful(c.state)
          c.config.authorizationURI returns None

          failed[ConfigurationException](c.provider.authenticate()(Fake.request)) {
            case e => e.getMessage must startWith(AuthorizationUriUndefined.format(c.provider.id))
          }: Result
      }
    }

    "redirect to authorization URI if authorization code doesn't exists in request" in {
      c.config.authorizationURI match {
        case None =>
          skipped("authorizationURI is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(authorizationURI) =>
          val headerName = "header-name"
          val headerValue = "header-value"

          c.stateHandler.serialize(c.state) returns Some(headerValue)
          c.stateHandler.unserialize(anyString)(any[Fake.RequestPipeline]()) returns Future.successful(c.state)
          c.stateHandler.state returns Future.successful(c.state)
          c.stateHandler.publish[SilhouetteRequest, SilhouetteResponse](any(), any())(any()) answers { (a, _) =>
            val result = a.asInstanceOf[Array[Any]](0).asInstanceOf[Fake.ResponsePipeline]
            val state = a.asInstanceOf[Array[Any]](1).asInstanceOf[c.TestState]

            result.withHeaders(Header(headerName, c.stateHandler.serialize(state).getOrElse("")))
          }

          result(c.provider.authenticate()(Fake.request)) { result =>
            await(result).status must equalTo(Status.`See Other`)
            await(result).header(headerName) must beSome(
              Header(headerName, c.stateHandler.serialize(c.state).getOrElse(""))
            )
            await(result).header(Header.Name.Location) must beSome.which { header: Header =>
              val urlParams = c.urlParams(header.values.head)
              val params = List(
                Some(ClientID -> c.config.clientID),
                Some(ResponseType -> Code),
                Some(OAuth2Provider.State -> urlParams(OAuth2Provider.State)),
                c.config.scope.map(Scope -> _),
                c.config.redirectURI.map(uri => RedirectUri -> uri.toString)
              ).flatten ++ c.config.authorizationParams.toList

              header.values.head must be equalTo (authorizationURI.toString + params.map { p =>
                encode(p._1, "UTF-8") + "=" + encode(p._2, "UTF-8")
              }.mkString("?", "&", ""))
            }
          }: Result
      }
    }

    "resolves relative redirectUri before starting the flow" in {
      verifyRedirectUri(
        redirectUri = Some(ConfigURI("/redirect-uri")),
        secure = false,
        resolvedRedirectUri = ConfigURI("http://www.example.com/redirect-uri")
      )
    }

    "resolves path relative redirectUri before starting the flow" in {
      verifyRedirectUri(
        redirectUri = Some(ConfigURI("redirect-uri")),
        secure = false,
        resolvedRedirectUri = ConfigURI("http://www.example.com/request-path/redirect-uri")
      )
    }

    "resolves relative redirectUri before starting the flow over https" in {
      verifyRedirectUri(
        redirectUri = Some(ConfigURI("/redirect-uri")),
        secure = true,
        resolvedRedirectUri = ConfigURI("https://www.example.com/redirect-uri")
      )
    }

    "verifying presence of redirect param in the access token post request" in {
      verifyRedirectUri(
        redirectUri = Some(ConfigURI("/redirect-uri")),
        secure = false,
        resolvedRedirectUri = ConfigURI("http://www.example.com/redirect-uri")
      )
    }

    "verifying presence of redirect param in the access token post request over https" in {
      verifyRedirectUri(
        redirectUri = Some(ConfigURI("/redirect-uri")),
        secure = true,
        resolvedRedirectUri = ConfigURI("https://www.example.com/redirect-uri")
      )
    }

    "verifying absence of redirect param in the access token post request" in {
      verifyRedirectUri(
        redirectUri = None,
        secure = false,
        resolvedRedirectUri = ConfigURI("http://www.example.com/request-path/redirect-uri")
      )
    }

    "verifying absence of redirect param in the access token post request over https" in {
      verifyRedirectUri(
        redirectUri = None,
        secure = true,
        resolvedRedirectUri = ConfigURI("https://www.example.com/redirect-uri")
      )
    }

    "not send state param if state is empty" in {
      c.config.authorizationURI match {
        case None =>
          skipped("authorizationURI is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(_) =>
          c.stateHandler.serialize(c.state) returns None
          c.stateHandler.unserialize(anyString)(any[Fake.RequestPipeline]()) returns Future.successful(c.state)
          c.stateHandler.state returns Future.successful(c.state)
          c.stateHandler.publish[SilhouetteRequest, SilhouetteResponse](any(), any())(any()) answers { (a, _) =>
            a.asInstanceOf[Array[Any]](0).asInstanceOf[Fake.ResponsePipeline]
          }

          result(c.provider.authenticate()(Fake.request))(result =>
            await(result).header(Header.Name.`Location`) must beSome[Header].like {
              case header =>
                header.value must not contain OAuth2Provider.State
            }
          ): Result
      }
    }

    "fail with UnexpectedResponseException if Json cannot be parsed from response" in {
      val code = "my.code"
      val request = Fake.request.withQueryParams(Code -> code)
      val httpResponse = Response(
        Status.OK,
        Body(MimeType.`application/json`, data = "<html></html>".getBytes(Codec.UTF8.charSet))
      )

      c.httpClient.execute(any[Request]()) returns Future.successful(httpResponse)
      c.stateHandler.unserialize(anyString)(any[Fake.RequestPipeline]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)

      failed[UnexpectedResponseException](c.provider.authenticate()(request)) {
        case e =>
          e.getMessage must startWith(JsonParseError.format(c.provider.id, "<html></html>"))
          e.getCause.getMessage must startWith("expected json value")
      }
    }

    "fail with UnexpectedResponseException for an unexpected response" in {
      val code = "my.code"
      val request = Fake.request.withQueryParams(Code -> code)
      val httpResponse = Response(Status.Unauthorized, Body.from("Unauthorized"))

      c.httpClient.execute(any[Request]()) returns Future.successful(httpResponse)
      c.stateHandler.unserialize(anyString)(any[Fake.RequestPipeline]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)

      failed[UnexpectedResponseException](c.provider.authenticate()(request)) {
        case e => e.getMessage must be equalTo UnexpectedResponse.format(
          c.provider.id, httpResponse.body.map(_.raw).getOrElse(""), Status.Unauthorized
        )
      }
    }

    "fail with UnexpectedResponseException if OAuth2Info can be build because of an unexpected response" in {
      val code = "my.code"
      val request = Fake.request.withQueryParams(Code -> code)
      val httpResponse = Response(Status.OK, Body.from(Json.obj()))

      c.httpClient.execute(any[Request]()) returns Future.successful(httpResponse)
      c.stateHandler.unserialize(anyString)(any[Fake.RequestPipeline]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)

      failed[UnexpectedResponseException](c.provider.authenticate()(request)) {
        case e => e.getMessage must startWith(InvalidInfoFormat.format(c.provider.id, ""))
      }
    }

    "return the auth info" in {
      val code = "my.code"
      val request = Fake.request.withQueryParams(Code -> code)
      val body = Body.from(Map(
        GrantType -> Seq(AuthorizationCode),
        Code -> Seq(code)
      ) ++
        c.config.accessTokenParams.map { case (key, value) => key -> Seq(value) } ++
        c.config.redirectURI.map(uri => Map(RedirectUri -> Seq(uri.toString))).getOrElse(Map())
      )

      val httpResponse = Response(Status.OK, Body.from(c.oAuth2InfoJson))
      val requestCaptor = capture[Request]

      c.httpClient.execute(any[Request]()) returns Future.successful(httpResponse)
      c.stateHandler.unserialize(anyString)(any[Fake.RequestPipeline]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)

      authInfo(c.provider.authenticate()(request))(_ must be equalTo c.oAuth2Info)
      there was one(c.httpClient).execute(requestCaptor)

      requestCaptor.value.headers must contain(c.authorizationHeader)
      requestCaptor.value.body must beSome(body)
    }
  }

  "The `authenticate` method with user state" should {
    val c = context
    "return stateful auth info" in {
      val code = "my.code"
      implicit val request: Fake.RequestPipeline = Fake.request.withQueryParams(Code -> code)

      val httpResponse = Response(Status.OK, Body.from(c.oAuth2InfoJson))

      c.httpClient.execute(any[Request]()) returns Future.successful(httpResponse)
      c.stateHandler.unserialize(anyString)(any[Fake.RequestPipeline]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)
      c.stateHandler.withHandler(any[StateItemHandler]()) returns c.stateHandler
      c.state.items returns Set(c.userStateItem)

      statefulAuthInfo(c.provider.authenticate(c.userStateItem))(_ must be equalTo c.stateAuthInfo)
    }
  }

  "The `refresh` method" should {
    val c = context
    "fail with an ConfigurationException if refresh URI is undefined when it's needed" in {
      c.config.refreshURI match {
        case None =>
          skipped("refreshURI is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(_) =>
          c.config.refreshURI returns None

          failed[ConfigurationException](c.provider.refresh("some-refresh-token")) {
            case e => e.getMessage must startWith(RefreshUriUndefined.format(c.provider.id))
          }: Result
      }
    }

    "fail with UnexpectedResponseException if Json cannot be parsed from response" in {
      c.config.refreshURI match {
        case None =>
          skipped("refreshURI is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(_) =>
          val refreshToken = "some-refresh-token"
          val httpResponse = Response(Status.OK, Body(
            MimeType.`application/json`, data = "<html></html>".getBytes(Codec.UTF8.charSet)
          ))

          c.httpClient.execute(any[Request]()) returns Future.successful(httpResponse)

          failed[UnexpectedResponseException](c.provider.refresh(refreshToken)) {
            case e =>
              e.getMessage must startWith(JsonParseError.format(c.provider.id, "<html></html>"))
              e.getCause.getMessage must startWith("expected json value")
          }: Result
      }
    }

    "fail with UnexpectedResponseException for an unexpected response" in {
      c.config.refreshURI match {
        case None =>
          skipped("refreshURI is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(_) =>
          val refreshToken = "some-refresh-token"
          val httpResponse = Response(Status.Unauthorized, Body.from("Unauthorized"))

          c.httpClient.execute(any[Request]()) returns Future.successful(httpResponse)

          failed[UnexpectedResponseException](c.provider.refresh(refreshToken)) {
            case e => e.getMessage must be equalTo UnexpectedResponse.format(
              c.provider.id, httpResponse.body.map(_.raw).getOrElse(""), Status.Unauthorized
            )
          }: Result
      }
    }

    "fail with UnexpectedResponseException if OAuth2Info can be build because of an unexpected response" in {
      c.config.refreshURI match {
        case None =>
          skipped("refreshURI is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(_) =>
          val refreshToken = "some-refresh-token"
          val httpResponse = Response(Status.OK, Body.from(Json.obj()))

          c.httpClient.execute(any[Request]()) returns Future.successful(httpResponse)

          failed[UnexpectedResponseException](c.provider.refresh(refreshToken)) {
            case e => e.getMessage must startWith(InvalidInfoFormat.format(c.provider.id, ""))
          }: Result
      }
    }

    "return the auth info" in {
      c.config.refreshURI match {
        case None =>
          skipped("refreshURI is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(_) =>
          val refreshToken = "some-refresh-token"
          val body = Body.from(Map(
            GrantType -> Seq(RefreshToken),
            RefreshToken -> Seq(refreshToken)
          ) ++
            c.config.refreshParams.map { case (key, value) => key -> Seq(value) } ++
            c.config.scope.map(scope => Map(Scope -> Seq(scope))).getOrElse(Map())
          )
          val httpResponse = Response(Status.OK, Body.from(c.oAuth2InfoJson))
          val requestCaptor = capture[Request]

          c.httpClient.execute(any[Request]()) returns Future.successful(httpResponse)

          await(c.provider.refresh(refreshToken)) must be equalTo c.oAuth2Info

          there was one(c.httpClient).execute(requestCaptor)

          requestCaptor.value.method must be equalTo Method.POST
          requestCaptor.value.headers must contain(c.authorizationHeader)
          requestCaptor.value.body must beSome(body): Result
      }
    }
  }

  "The `config` method" should {
    val c = context
    "return the config instance" in {
      c.provider.config must be equalTo c.config
    }
  }

  "The `withConfig` method" should {
    val c = context
    "create a new instance with customized config" in {
      val newProvider = c.provider.withConfig { s =>
        s.copy(accessTokenURI = ConfigURI("new-access-token-uri"))
      }

      newProvider.config.accessTokenURI must be equalTo ConfigURI("new-access-token-uri")
    }
  }

  /**
   * Defines the context for the abstract OAuth2 provider spec.
   *
   * @return The Context to use for the abstract OAuth2 provider spec.
   */
  protected def context: BaseContext

  /**
   * Helper method that validates the resolved redirect URI.
   */
  protected def verifyRedirectUri(
    redirectUri: Option[ConfigURI],
    secure: Boolean,
    resolvedRedirectUri: ConfigURI
  )(
    implicit
    c: BaseContext
  ) = {
    c.config.authorizationURI match {
      case None =>
        skipped("authorizationUri is not defined, so this step isn't needed for provider: " + c.provider.getClass)
      case Some(_) =>
        val scheme = if (secure) "https" else "http"
        val uri = ConfigURI(s"$scheme://www.example.com/request-path/something")
        val request = Fake.request(uri)

        c.config.redirectURI returns redirectUri

        c.stateHandler.serialize(c.state) returns Some("value")
        c.stateHandler.unserialize(anyString)(any[Fake.RequestPipeline]()) returns Future.successful(c.state)
        c.stateHandler.state returns Future.successful(c.state)
        c.stateHandler.publish[SilhouetteRequest, SilhouetteResponse](any(), any())(any()) answers { (a, _) =>
          a.asInstanceOf[Array[Any]](0).asInstanceOf[Fake.ResponsePipeline]
        }

        redirectUri match {
          case Some(_) =>
            result(c.provider.authenticate()(request)) { result =>
              await(result).header(Header.Name.Location) must beSome[Header].like {
                case header =>
                  header.values.head must contain(s"$RedirectUri=${encode(resolvedRedirectUri.toString, "UTF-8")}")
              }
            }: Result
          case None =>
            result(c.provider.authenticate()(request)) { result =>
              await(result).header(Header.Name.Location) must beSome[Header].like {
                case header =>
                  header.values.head must not contain s"$RedirectUri="
              }
            }: Result
        }
    }
  }

  /**
   * Base context.
   */
  trait BaseContext extends Scope with Mockito with ThrownExpectations {
    abstract class TestState extends State(Set.empty)
    abstract class TestStateHandler extends StateHandler {
      override type Self = TestStateHandler
      override def withHandler(handler: StateItemHandler): TestStateHandler
    }

    /**
     * Paths to the Json fixtures.
     */
    val ErrorJson: BaseFixture.F
    val AccessTokenJson: BaseFixture.F
    val UserProfileJson: BaseFixture.F

    /**
     * The clock instance.
     */
    val clock = {
      val c = mock[Clock].smart
      c.instant() returns Instant.ofEpochSecond(0)
      c
    }

    /**
     * The access token decoder to use to decode the [[OAuth2Info]] from JSON.
     */
    implicit val accessTokenDecoder: Decoder[OAuth2Info] = OAuth2Info.decoder(clock)

    /**
     * The HTTP client mock.
     */
    lazy val httpClient = mock[HttpClient].smart

    /**
     * The OAuth2 state.
     */
    lazy val state = mock[TestState].smart

    /**
     * A user state item.
     */
    lazy val userStateItem = UserStateItem(Map("path" -> "/login"))

    /**
     * The OAuth2 state handler.
     */
    lazy val stateHandler = mock[TestStateHandler].smart

    /**
     * A OAuth2 info as JSON.
     */
    lazy val oAuth2InfoJson = AccessTokenJson.asJson

    /**
     * The OAuth2 info decoded from the JSON.
     */
    lazy val oAuth2Info = AccessTokenJson.as[OAuth2Info]

    /**
     * The stateful auth info.
     */
    lazy val stateAuthInfo = StatefulAuthInfo(oAuth2Info, userStateItem)

    /**
     * The OAuth2 config.
     */
    def config: OAuth2Config

    /**
     * The provider to test.
     */
    def provider: OAuth2Provider

    /**
     * Gets the authorization header.
     *
     * @return The authorization header.
     * @see https://tools.ietf.org/html/rfc6749#section-2.3.1
     */
    def authorizationHeader: Header = BasicAuthorizationHeader(
      BasicCredentials(encode(config.clientID, "UTF-8"), encode(config.clientSecret, "UTF-8"))
    )

    /**
     * Extracts the params of a URL.
     *
     * @param url The url to parse.
     * @return The params of a URL.
     */
    def urlParams(url: String): Map[String, String] = (url.split('&') map { str =>
      val pair = str.split('=')
      pair(0) -> pair(1)
    }).toMap
  }
}
