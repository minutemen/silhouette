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
package silhouette.provider.social.state.handler

import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import io.circe.{ Decoder, Encoder, HCursor, Json }
import javax.inject.Inject
import silhouette.crypto.{ SecureAsyncID, Signer }
import silhouette.http.{ Cookie, RequestPipeline, ResponsePipeline }
import silhouette.provider.social.SocialStateException
import silhouette.provider.social.state.StateItem.ItemStructure
import silhouette.provider.social.state.handler.CsrfStateItemHandler._
import silhouette.provider.social.state.{ PublishableStateItemHandler, StateItem, StateItemHandler }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

/**
 * The item the handler can handle.
 *
 * @param token A unique token used to protect the application against CSRF attacks.
 */
case class CsrfStateItem(token: String) extends StateItem

/**
 * The companion object of the [[CsrfStateItem]].
 */
object CsrfStateItem {
  implicit val jsonDecoder: Decoder[CsrfStateItem] = (c: HCursor) => for {
    token <- c.downField("token").as[String]
  } yield {
    new CsrfStateItem(token)
  }
  implicit val jsonEncoder: Encoder[CsrfStateItem] = (a: CsrfStateItem) => Json.obj(
    ("token", Json.fromString(a.token))
  )
}

/**
 * Protects the application against CSRF attacks.
 *
 * The handler stores a unique token in provider state and the same token in a signed client side cookie. After the
 * provider redirects back to the application both tokens will be compared. If both tokens are the same than the
 * application can trust the redirect source.
 *
 * @param settings The state settings.
 * @param secureID A secure ID implementation, used to create the state value.
 * @param signer   The signer implementation.
 */
class CsrfStateItemHandler @Inject() (
  settings: CsrfStateSettings,
  secureID: SecureAsyncID[String],
  signer: Signer
) extends StateItemHandler with LazyLogging
  with PublishableStateItemHandler {

  /**
   * The item the handler can handle.
   */
  override type Item = CsrfStateItem

  /**
   * Gets the state item the handler can handle.
   *
   * @param ec The execution context to handle the asynchronous operations.
   * @return The state params the handler can handle.
   */
  override def item(implicit ec: ExecutionContext): Future[Item] = secureID.get.map(CsrfStateItem.apply)

  /**
   * Indicates if a handler can handle the given [[StateItem]].
   *
   * This method should check if the [[serialize]] method of this handler can serialize the given
   * unserialized state item.
   *
   * @param item The item to check for.
   * @return `Some[Item]` casted state item if the handler can handle the given state item, `None` otherwise.
   */
  override def canHandle(item: StateItem): Option[Item] = item match {
    case i: Item => Some(i)
    case _       => None
  }

  /**
   * Indicates if a handler can handle the given unserialized state item.
   *
   * This method should check if the [[unserialize]] method of this handler can unserialize the given
   * serialized state item.
   *
   * @param item    The item to check for.
   * @param request The request instance to get additional data to validate against.
   * @tparam R The type of the request.
   * @return True if the handler can handle the given state item, false otherwise.
   */
  override def canHandle[R](item: ItemStructure)(implicit request: RequestPipeline[R]): Boolean = {
    item.id == ID && {
      clientState match {
        case Success(i) => item.data.as[Item].exists(_ == i)
        case Failure(e) =>
          logger.warn(e.getMessage, e)
          false
      }
    }
  }

  /**
   * Returns a serialized value of the state item.
   *
   * @param item The state item to serialize.
   * @return The serialized state item.
   */
  override def serialize(item: Item): ItemStructure = ItemStructure(ID, item.asJson)

  /**
   * Unserializes the state item.
   *
   * @param item    The state item to unserialize.
   * @param request The request instance to get additional data to validate against.
   * @param ec      The execution context to handle the asynchronous operations.
   * @tparam R The type of the request.
   * @return The unserialized state item.
   */
  override def unserialize[R](item: ItemStructure)(
    implicit
    request: RequestPipeline[R],
    ec: ExecutionContext
  ): Future[Item] = {
    Future.fromTry(item.data.as[Item].toTry)
  }

  /**
   * Publishes the CSRF token to the client.
   *
   * @param item     The item to publish.
   * @param response The response to send to the client.
   * @param request  The current request.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The result to send to the client.
   */
  override def publish[R, P](item: Item, response: ResponsePipeline[P])(
    implicit
    request: RequestPipeline[R]
  ): ResponsePipeline[P] = {
    response.withCookies(Cookie(
      name = settings.cookieName,
      value = signer.sign(item.token),
      maxAge = Some(settings.expirationTime.toSeconds.toInt),
      path = settings.cookiePath,
      domain = settings.cookieDomain,
      secure = settings.secureCookie,
      httpOnly = settings.httpOnlyCookie,
      sameSite = settings.sameSite
    ))
  }

  /**
   * Gets the CSRF token from the cookie.
   *
   * @param request The request header.
   * @tparam R The type of the request.
   * @return The CSRF token on success, otherwise a failure.
   */
  private def clientState[R](implicit request: RequestPipeline[R]): Try[Item] = {
    request.cookie(settings.cookieName) match {
      case Some(cookie) => signer.extract(cookie.value).map(token => CsrfStateItem(token))
      case None         => Failure(new SocialStateException(ClientStateDoesNotExists.format(settings.cookieName)))
    }
  }
}

/**
 * The companion object.
 */
object CsrfStateItemHandler {

  /**
   * The ID of the handler.
   */
  val ID = "csrf-state"

  /**
   * The error messages.
   */
  val ClientStateDoesNotExists = "State cookie doesn't exists for name: %s"
}

/**
 * The settings for the Csrf State.
 *
 * @param cookieName     The cookie name.
 * @param cookiePath     The cookie path.
 * @param cookieDomain   The cookie domain.
 * @param secureCookie   Whether this cookie is secured, sent only for HTTPS requests.
 * @param httpOnlyCookie Whether this cookie is HTTP only, i.e. not accessible from client-side JavaScript code.
 * @param sameSite       Whether this cookie forces the SameSite policy to prevent CSRF attacks.
 * @param expirationTime State expiration. Defaults to 5 minutes which provides sufficient time to log in, but
 *                       not too much. This is a balance between convenience and security.
 */
case class CsrfStateSettings(
  cookieName: String = "CsrfState",
  cookiePath: Option[String] = Some("/"),
  cookieDomain: Option[String] = None,
  secureCookie: Boolean = true,
  httpOnlyCookie: Boolean = true,
  sameSite: Option[Cookie.SameSite] = Some(Cookie.SameSite.Lax),
  expirationTime: FiniteDuration = 5 minutes
)
