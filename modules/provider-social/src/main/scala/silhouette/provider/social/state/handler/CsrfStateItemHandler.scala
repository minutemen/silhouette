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

import java.time.Clock

import cats.effect.Async
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import io.circe.{ Decoder, Encoder, HCursor, Json }
import javax.inject.Inject
import silhouette.RichInstant._
import silhouette.crypto.SecureID
import silhouette.http.transport.{ CookieTransportConfig, EmbedIntoCookie, RetrieveFromCookie }
import silhouette.http.{ RequestPipeline, ResponsePipeline, ResponseWriter }
import silhouette.jwt.{ Claims, JwtClaimReader, JwtClaimWriter }
import silhouette.provider.social.SocialStateException
import silhouette.provider.social.state.handler.CsrfStateItemHandler._
import silhouette.provider.social.state.{ StateItem, StateItemHandler }

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
 * @param secureID     A secure ID implementation, used to create the state value.
 * @param cookieConfig The cookie config.
 * @param claimReader  The JWT claim reader function.
 * @param claimWriter  The JWT claim writer function.
 * @param clock        The current clock.
 * @tparam F The type of the IO monad.
 */
class CsrfStateItemHandler[F[_]: Async] @Inject() (
  secureID: SecureID[F, String],
  cookieConfig: CookieTransportConfig,
  claimReader: JwtClaimReader,
  claimWriter: JwtClaimWriter,
  clock: Clock
) extends StateItemHandler[F, CsrfStateItem] with LazyLogging {

  /**
   * Gets the ID of the handler.
   *
   * @return The ID of the handler.
   */
  override def id: String = ID

  /**
   * Returns the [[io.circe.Json]] representation of the state item and a function that can embed item specific
   * state into a response.
   *
   * A state item handler is able to embed some item specific state into the response. In the unserialize method
   * it can then be extracted from the request. So this method returns also a function, that can write this state
   * to a response.
   *
   * @tparam R The type of the response.
   * @return Either an error or the item serialized as [[io.circe.Json]] and a function, that is able to embed item
   *         specific state into a response pipeline.
   */
  override def serialize[R]: F[(Json, ResponseWriter[R])] = {
    for {
      id <- secureID.get
      json <- Async[F].pure(CsrfStateItem(id).asJson)
      jwt <- Async[F].fromEither(claimWriter(Claims(
        issuer = Some(cookieConfig.name),
        subject = Some(ID),
        audience = cookieConfig.domain.map(d => List(d)),
        expirationTime = cookieConfig.maxAge.map(maxAge => clock.instant() + maxAge),
        notBefore = Some(clock.instant()),
        issuedAt = Some(clock.instant()),
        jwtID = Some(id)
      )))
    } yield {
      (json, (responsePipeline: ResponsePipeline[R]) => EmbedIntoCookie(cookieConfig)(responsePipeline)(jwt))
    }
  }

  /**
   * Unserializes the state item.
   *
   * A state item handler is able to embed some item specific state into the response. In this method it can then be
   * extracted from the request.Therefore the request is also passed in addition to the serialized [[io.circe.Json]]
   * instance.
   *
   * @tparam R The type of the request.
   * @param json    The serialized state item as [[io.circe.Json]].
   * @param request The request instance to get additional data to validate against.
   * @return Either an error or the unserialized state item.
   */
  override def unserialize[R](json: Json, request: RequestPipeline[R]): F[CsrfStateItem] = {
    (for {
      jwt <- Async[F].fromOption(
        RetrieveFromCookie(cookieConfig.name)(request),
        new SocialStateException(ClientStateDoesNotExists.format(cookieConfig.name))
      )
      clientClaims <- Async[F].fromEither(claimReader(jwt))
      clientState <- Async[F].pure(CsrfStateItem(clientClaims.jwtID.getOrElse("")))
      providerState <- Async[F].fromEither(json.as[CsrfStateItem].asInstanceOf[Either[Throwable, CsrfStateItem]])
    } yield {
      clientState -> providerState
    }).flatMap {
      case (clientState, providerState) if clientState == providerState => Async[F].pure(providerState)
      case _ => Async[F].raiseError(new SocialStateException(ClientStateDoesNotMatch))
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
  val ClientStateDoesNotMatch = "Potential CSRF attack detected! The client state doesn't match the provider state"
}
