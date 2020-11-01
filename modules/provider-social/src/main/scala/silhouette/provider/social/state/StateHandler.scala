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
package silhouette.provider.social.state

import java.time.Clock

import cats.data.{ NonEmptyList => NEL }
import cats.effect.Async
import cats.syntax.all._
import io.circe.{ Json, JsonObject }
import javax.inject.Inject
import silhouette.RichInstant._
import silhouette.http.{ RequestPipeline, ResponseWriter }
import silhouette.jwt.{ Claims, JwtClaimReader, JwtClaimWriter }

import scala.concurrent.duration._

/**
 * Provides a way to handle different types of state for providers that allow a state param.
 *
 * Some authentication protocols defines a state param which can be used to transport some
 * state to an authentication provider. The authentication provider sends this state back
 * to the application, after the authentication to the provider was granted.
 *
 * The state parameter can be used for different things. Silhouette provides two state handlers
 * out of the box. One state handler can transport additional user state to the provider. This
 * could be an URL were the user should be redirected after authentication to the provider, or
 * any other per-authentication based state. An other important state handler protects the
 * application for CSRF attacks.
 *
 * @tparam F The type of the IO monad.
 * @param handlers    The item handlers configured for this handler.
 * @param config      The state handler config.
 * @param claimReader The JWT claim reader function.
 * @param claimWriter The JWT claim writer function.
 * @param clock       The current clock.
 */
case class StateHandler[F[_]: Async] @Inject() (
  handlers: NEL[StateItemHandler[F, StateItem]],
  config: StateHandlerConfig,
  claimReader: JwtClaimReader,
  claimWriter: JwtClaimWriter,
  clock: Clock
) {

  /**
   * Returns a JWT that contains the state items and a function that can embed item specific state into a response.
   *
   * A state item handler is able to embed some item specific state into the response. In the unserialize method
   * it can then be extracted from the request. So this method returns also a function, that can write this state
   * to a response.
   *
   * @tparam R The type of the response.
   * @return Either an error or a JWT and a function, that is able embed item specific state into a response pipeline.
   */
  def serialize[R]: F[(String, ResponseWriter[R])] =
    for {
      items <- handlers.map(h => h.serialize[R].map(t => (h.id, t._1, t._2))).sequence
      jsonMap <- Async[F].pure(items.foldLeft(Map.empty[String, Json]) { case (acc, (handlerID, json, _)) =>
        acc + (handlerID -> json)
      })
      responseWriter = items.foldLeft[ResponseWriter[R]](identity) { case (acc, (_, _, responseWriter)) =>
        acc andThen responseWriter
      }
      jwt <- Async[F].fromEither(
        claimWriter.apply(
          Claims(
            issuer = config.jwtIssuer,
            subject = config.jwtSubject,
            expirationTime = config.jwtExpiry.map(clock.instant() + _),
            custom = JsonObject.fromMap(jsonMap)
          )
        )
      )
    } yield jwt -> responseWriter

  /**
   * Unserializes the social state from the state param.
   *
   * A state item handler is able to embed some item specific state into the response. In this method it can then
   * be extracted from the request.
   *
   * @param state   The state to unserialize as JWT.
   * @param request The request to read the value of the state param from.
   * @tparam R The type of the request.
   * @return The social state on success, an error on failure.
   */
  def unserialize[R](state: String, request: RequestPipeline[R]): F[State] =
    for {
      claims <- Async[F].fromEither(claimReader(state))
      items <- handlers.map(h => h.unserialize(claims.custom.toMap(h.id), request).map(item => h.id -> item)).sequence
    } yield State(items.toNem)
}

/**
 * The state handler config.
 *
 * @param jwtIssuer  An issuer for the JWT token.
 * @param jwtSubject An subject for the JWT token.
 * @param jwtExpiry  An expiry for the JWT token.
 */
case class StateHandlerConfig(
  jwtIssuer: Option[String] = Some("silhouette-state-handler"),
  jwtSubject: Option[String] = Some("silhouette-state"),
  jwtExpiry: Option[FiniteDuration] = Some(5.minutes)
)
