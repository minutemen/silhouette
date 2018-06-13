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
package silhouette.provider.social

import io.circe.{ Decoder, Encoder }
import silhouette.AuthInfo
import silhouette.http.{ RequestPipeline, ResponsePipeline }
import silhouette.provider.social.state.StateItem

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * A stateful auth info, wraps the `AuthInfo` with user state.
 *
 * @param authInfo  The auth info.
 * @param userState The user state.
 * @tparam A The type of the auth info.
 * @tparam S The type of the user state.
 */
case class StatefulAuthInfo[+A <: AuthInfo, +S <: StateItem](authInfo: A, userState: S)

/**
 * Extends the [[SocialProvider]] with the ability to handle provider specific state.
 */
trait SocialStateProvider extends SocialProvider {

  /**
   * Authenticates the user and returns the auth information and the user state.
   *
   * Returns either a [[StatefulAuthInfo]] if all went OK or a `ResponsePipeline` that the controller
   * sends to the browser (e.g.: in the case of OAuth where the user needs to be redirected to the service
   * provider).
   *
   * @param decoder  The JSON decoder.
   * @param encoder  The JSON encoder.
   * @param request  The request.
   * @param classTag The class tag for the user state item.
   * @tparam S The type of the user state item.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return Either a `ResponsePipeline` or the [[StatefulAuthInfo]] from the provider.
   */
  def authenticate[S <: StateItem, R, P](userState: S)(
    implicit
    decoder: Decoder[S],
    encoder: Encoder[S],
    request: RequestPipeline[R],
    classTag: ClassTag[S]
  ): Future[Either[ResponsePipeline[P], StatefulAuthInfo[A, S]]]
}
