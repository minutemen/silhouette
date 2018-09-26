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

import io.circe.Json
import org.specs2.execute.Result
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import silhouette.ConfigurationException
import silhouette.http._
import silhouette.http.client.BodyFormat._
import silhouette.http.client.{ Body, BodyFormat, Response }
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.provider.social.state.handler.UserStateItem
import silhouette.provider.social.state.{ State, StateHandler, StateItem, StateItemHandler }
import silhouette.provider.{ AccessDeniedException, UnexpectedResponseException }
import silhouette.specs2.BaseFixture

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

    "fail with an ConfigurationException if authorization URL is undefined when it's needed" in {
      c.settings.authorizationUri match {
        case None =>
          skipped("authorizationURL is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(_) =>
          c.stateHandler.serialize(c.state) returns Some("session-value")
          c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
          c.stateHandler.state returns Future.successful(c.state)
          c.settings.authorizationUri returns None

          failed[ConfigurationException](c.provider.authenticate()(Fake.request)) {
            case e => e.getMessage must startWith(AuthorizationUriUndefined.format(c.provider.id))
          }: Result
      }
    }

    "redirect to authorization URL if authorization code doesn't exists in request" in {
      c.settings.authorizationUri match {
        case None =>
          skipped("authorizationURL is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(authorizationURL) =>
          val sessionKey = "session-key"
          val sessionValue = "session-value"

          c.stateHandler.serialize(c.state) returns Some(sessionValue)
          c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
          c.stateHandler.state returns Future.successful(c.state)
          c.stateHandler.publish[SilhouetteRequest, SilhouetteResponse](any(), any())(any()) answers { (a, _) =>
            val result = a.asInstanceOf[Array[Any]](0).asInstanceOf[Fake.Response]
            val state = a.asInstanceOf[Array[Any]](1).asInstanceOf[c.TestState]

            result.withSession(sessionKey -> c.stateHandler.serialize(state).getOrElse(""))
          }

          result(c.provider.authenticate()(Fake.request)) { result =>
            await(result).status must equalTo(Status.`See Other`)
            await(result).session.get(sessionKey) must beSome(c.stateHandler.serialize(c.state).getOrElse(""))
            await(result).header(Header.Name.Location) must beSome.which { header =>
              val urlParams = c.urlParams(header.values.head)
              val params = List(
                Some(ClientID -> c.settings.clientID),
                Some(ResponseType -> Code),
                Some(OAuth2Provider.State -> urlParams(OAuth2Provider.State)),
                c.settings.scope.map(Scope -> _),
                c.settings.redirectUri.map(uri => RedirectUri -> uri.toString)
              ).flatten ++ c.settings.authorizationParams.toList

              header.values.head must be equalTo (authorizationURL + params.map { p =>
                encode(p._1, "UTF-8") + "=" + encode(p._2, "UTF-8")
              }.mkString("?", "&", ""))
            }
          }: Result
      }
    }

    "resolves relative redirectUri before starting the flow" in {
      verifyRedirectUri(
        redirectUri = Some(URI("/redirect-url")),
        secure = false,
        resolvedRedirectUri = URI("http://www.example.com/redirect-url")
      )
    }

    "resolves path relative redirectUri before starting the flow" in {
      verifyRedirectUri(
        redirectUri = Some(URI("redirect-url")),
        secure = false,
        resolvedRedirectUri = URI("http://www.example.com/request-path/redirect-url")
      )
    }

    "resolves relative redirectUri before starting the flow over https" in {
      verifyRedirectUri(
        redirectUri = Some(URI("/redirect-url")),
        secure = true,
        resolvedRedirectUri = URI("https://www.example.com/redirect-url")
      )
    }

    "verifying presence of redirect param in the access token post request" in {
      verifyRedirectUri(
        redirectUri = Some(URI("/redirect-url")),
        secure = false,
        resolvedRedirectUri = URI("http://www.example.com/redirect-url")
      )
    }

    "verifying presence of redirect param in the access token post request over https" in {
      verifyRedirectUri(
        redirectUri = Some(URI("/redirect-url")),
        secure = true,
        resolvedRedirectUri = URI("https://www.example.com/redirect-url")
      )
    }

    "verifying absence of redirect param in the access token post request" in {
      verifyRedirectUri(
        redirectUri = None,
        secure = false,
        resolvedRedirectUri = URI("http://www.example.com/request-path/redirect-url")
      )
    }

    "verifying absence of redirect param in the access token post request over https" in {
      verifyRedirectUri(
        redirectUri = None,
        secure = true,
        resolvedRedirectUri = URI("https://www.example.com/redirect-url")
      )
    }

    "not send state param if state is empty" in {
      c.settings.authorizationUri match {
        case None =>
          skipped("authorizationURL is not defined, so this step isn't needed for provider: " + c.provider.getClass)
        case Some(_) =>
          c.stateHandler.serialize(c.state) returns None
          c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
          c.stateHandler.state returns Future.successful(c.state)
          c.stateHandler.publish[SilhouetteRequest, SilhouetteResponse](any(), any())(any()) answers { (a, _) =>
            a.asInstanceOf[Array[Any]](0).asInstanceOf[Fake.Response]
          }

          result(c.provider.authenticate()(Fake.request))(result =>
            await(result).header(Header.Name.`Location`) must beSome[Header].like {
              case header =>
                header.value must not contain OAuth2Provider.State
            }
          ): Result
      }
    }

    "submit the proper params to the access token post request" in {
      val code = "my.code"
      val request = Fake.request.withQueryParams(Code -> code)
      val params = Map(
        ClientID -> Seq(c.settings.clientID),
        ClientSecret -> Seq(c.settings.clientSecret),
        GrantType -> Seq(AuthorizationCode),
        Code -> Seq(code)
      ) ++
        c.settings.accessTokenParams.mapValues(Seq(_)) ++
        c.settings.redirectUri.map(uri => Map(RedirectUri -> Seq(uri.toString))).getOrElse(Map())

      val body = Body.from(params)(BodyFormat.formUrlEncodedFormat)

      c.httpClient.withUri(c.settings.accessTokenUri) returns c.httpClient
      c.httpClient.withHeaders(any()) returns c.httpClient
      c.httpClient.withMethod(Method.POST) returns c.httpClient
      c.httpClient.withBody(body) returns c.httpClient
      // We must use this neat trick here because it isn't possible to check the withBody call with a verification,
      // because of the implicit params needed for the withBody call. On the other hand we can test it in the abstract
      // spec, because we throw an exception in both cases which stops the test once the post method was called.
      // This protects as for an NPE because of the not mocked dependencies. The other solution would be to execute
      // this test in every provider with the full mocked dependencies.
      c.httpClient.withBody[Map[String, Seq[String]]](any())(any()) answers { (a, _) =>
        if (a.asInstanceOf[Array[Any]](0).asInstanceOf[Map[String, Seq[String]]].equals(params)) {
          throw new RuntimeException("success")
        } else {
          throw new RuntimeException("failure")
        }
      }
      c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)

      failed[RuntimeException](c.provider.authenticate()(request)) {
        case e => e.getMessage must startWith("success")
      }
    }

    "fail with UnexpectedResponseException if Json cannot be parsed from response" in {
      val code = "my.code"
      val httpResponse = mock[Response]
      val request = Fake.request.withQueryParams(Code -> code)

      httpResponse.status returns 200
      httpResponse.body returns Body(MimeType.`application/json`, data = "<html></html>".getBytes(Codec.UTF8.charSet))

      c.httpClient.withUri(c.settings.accessTokenUri) returns c.httpClient
      c.httpClient.withHeaders(any()) returns c.httpClient
      c.httpClient.withBody[Map[String, Seq[String]]](any())(any()) returns c.httpClient
      c.httpClient.withMethod(Method.POST) returns c.httpClient
      c.httpClient.execute returns Future.successful(httpResponse)

      c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)

      failed[UnexpectedResponseException](c.provider.authenticate()(request)) {
        case e =>
          e.getMessage must startWith(JsonParseError.format(c.provider.id, "<html></html>"))
          e.getCause.getMessage must be equalTo "expected json value got < (line 1, column 1)"
      }
    }

    "fail with UnexpectedResponseException for an unexpected response" in {
      val code = "my.code"
      val httpResponse = mock[Response]
      val request = Fake.request.withQueryParams(Code -> code)

      httpResponse.status returns 401
      httpResponse.body returns Body.from("Unauthorized")

      c.httpClient.withUri(c.settings.accessTokenUri) returns c.httpClient
      c.httpClient.withHeaders(any()) returns c.httpClient
      c.httpClient.withBody[Map[String, Seq[String]]](any())(any()) returns c.httpClient
      c.httpClient.withMethod(Method.POST) returns c.httpClient
      c.httpClient.execute returns Future.successful(httpResponse)

      c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)

      failed[UnexpectedResponseException](c.provider.authenticate()(request)) {
        case e => e.getMessage must be equalTo UnexpectedResponse.format(
          c.provider.id, httpResponse.body.raw, Status.Unauthorized
        )
      }
    }

    "fail with UnexpectedResponseException if OAuth2Info can be build because of an unexpected response" in {
      val code = "my.code"
      val httpResponse = mock[Response]
      val request = Fake.request.withQueryParams(Code -> code)

      httpResponse.status returns 200
      httpResponse.body returns Body.from(Json.obj())

      c.httpClient.withUri(c.settings.accessTokenUri) returns c.httpClient
      c.httpClient.withHeaders(any()) returns c.httpClient
      c.httpClient.withBody[Map[String, Seq[String]]](any())(any()) returns c.httpClient
      c.httpClient.withMethod(Method.POST) returns c.httpClient
      c.httpClient.execute returns Future.successful(httpResponse)

      c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)

      failed[UnexpectedResponseException](c.provider.authenticate()(request)) {
        case e => e.getMessage must startWith(InvalidInfoFormat.format(c.provider.id, ""))
      }
    }

    "return the auth info" in {
      val code = "my.code"
      val httpResponse = mock[Response]
      val request = Fake.request.withQueryParams(Code -> code)

      httpResponse.status returns 200
      httpResponse.body returns Body.from(c.oAuth2InfoJson)

      c.httpClient.withUri(c.settings.accessTokenUri) returns c.httpClient
      c.httpClient.withHeaders(any()) returns c.httpClient
      c.httpClient.withBody[Map[String, Seq[String]]](any())(any()) returns c.httpClient
      c.httpClient.withMethod(Method.POST) returns c.httpClient
      c.httpClient.execute returns Future.successful(httpResponse)

      c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)

      authInfo(c.provider.authenticate()(request))(_ must be equalTo c.oAuth2Info)
    }
  }

  "The `authenticate` method with user state" should {
    val c = context
    "return stateful auth info" in {
      val code = "my.code"
      val httpResponse = mock[Response]
      implicit val request: Fake.Request = Fake.request.withQueryParams(Code -> code)

      httpResponse.status returns 200
      httpResponse.body returns Body.from(c.oAuth2InfoJson)

      c.httpClient.withUri(c.settings.accessTokenUri) returns c.httpClient
      c.httpClient.withHeaders(any()) returns c.httpClient
      c.httpClient.withBody[Map[String, Seq[String]]](any())(any()) returns c.httpClient
      c.httpClient.withMethod(Method.POST) returns c.httpClient
      c.httpClient.execute returns Future.successful(httpResponse)

      c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
      c.stateHandler.state returns Future.successful(c.state)
      c.stateHandler.withHandler(any[StateItemHandler]()) returns c.stateHandler
      c.state.items returns Set(c.userStateItem)

      statefulAuthInfo(c.provider.authenticate(c.userStateItem))(_ must be equalTo c.stateAuthInfo)
    }
  }

  "The `settings` method" should {
    val c = context
    "return the settings instance" in {
      c.provider.settings must be equalTo c.settings
    }
  }

  "The `withSettings` method" should {
    val c = context
    "create a new instance with customized settings" in {
      val newProvider = c.provider.withSettings { s =>
        s.copy(accessTokenUri = URI("new-access-token-url"))
      }

      newProvider.settings.accessTokenUri must be equalTo URI("new-access-token-url")
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
    redirectUri: Option[URI],
    secure: Boolean,
    resolvedRedirectUri: URI
  )(
    implicit
    c: BaseContext
  ) = {
    c.settings.authorizationUri match {
      case None =>
        skipped("authorizationUri is not defined, so this step isn't needed for provider: " + c.provider.getClass)
      case Some(_) =>
        val scheme = if (secure) "https" else "http"
        val uri = URI(s"$scheme://www.example.com/request-path/something")
        val request = Fake.request(uri)

        val sessionKey = "session-key"
        val sessionValue = "session-value"

        c.settings.redirectUri returns redirectUri

        c.stateHandler.serialize(c.state) returns Some(sessionValue)
        c.stateHandler.unserialize(anyString)(any[Fake.Request]()) returns Future.successful(c.state)
        c.stateHandler.state returns Future.successful(c.state)
        c.stateHandler.publish[SilhouetteRequest, SilhouetteResponse](any(), any())(any()) answers { (a, _) =>
          val result = a.asInstanceOf[Array[Any]](0).asInstanceOf[Fake.Response]
          val state = a.asInstanceOf[Array[Any]](1).asInstanceOf[BaseContext#TestState]

          result.withSession(sessionKey -> c.stateHandler.serialize(state).getOrElse(""))
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
     * The OAuth2 settings.
     */
    def settings: OAuth2Settings

    /**
     * The provider to test.
     */
    def provider: OAuth2Provider

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
