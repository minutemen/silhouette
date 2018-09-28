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

import java.net.URI

import silhouette.http.{ HttpClient, RequestPipeline, ResponsePipeline, SilhouetteResponse }
import silhouette.provider.Provider
import silhouette.provider.social.SocialProvider._
import silhouette.{ AuthInfo, ExecutionContextProvider }

import scala.concurrent.Future

/**
 * The base interface for all social providers.
 *
 * @tparam C The type of the config.
 */
trait SocialProvider[C] extends Provider with SocialProfileBuilder with ExecutionContextProvider {

  /**
   * The type of the concrete implementation of this abstract type.
   */
  type Self <: SocialProvider[C]

  /**
   * The type of the auth info.
   */
  type A <: AuthInfo

  /**
   * The HTTP client implementation.
   */
  protected val httpClient: HttpClient

  /**
   * Gets the provider config.
   *
   * @return The provider config.
   */
  def config: C

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  def withConfig(f: C => C): Self

  /**
   * Authenticates the user and returns the auth information.
   *
   * Returns either a `AuthInfo` if all went OK or a `RequestPipeline` that the controller sends
   * to the browser (e.g.: in the case of OAuth where the user needs to be redirected to the service
   * provider).
   *
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return Either a `ResponsePipeline` or the `AuthInfo` from the provider.
   */
  def authenticate[R]()(
    implicit
    request: RequestPipeline[R]
  ): Future[Either[ResponsePipeline[SilhouetteResponse], A]]

  /**
   * Retrieves the user profile for the given auth info.
   *
   * This method can be used to retrieve the profile information for an already authenticated identity.
   *
   * @param authInfo The auth info for which the profile information should be retrieved.
   * @return The profile information for the given auth info.
   */
  def retrieveProfile(authInfo: A): Future[Profile] = {
    buildProfile(authInfo).recoverWith {
      case e if !e.isInstanceOf[ProfileRetrievalException] =>
        Future.failed(new ProfileRetrievalException(UnspecifiedProfileError.format(id), Some(e)))
    }
  }

  /**
   * Resolves the URI to be absolute relative to the request.
   *
   * This will pass the URI through if its already absolute.
   *
   * @param uri     The URI to resolve.
   * @param request The current request.
   * @tparam R The type of the request.
   * @return The absolute url.
   */
  protected def resolveCallbackUri[R](uri: URI)(
    implicit
    request: RequestPipeline[R]
  ): URI = if (uri.isAbsolute) uri else request.uri.resolve(uri)
}

/**
 * The companion object.
 */
object SocialProvider {

  /**
   * Some error messages.
   */
  val UnspecifiedProfileError = "[%s] Error retrieving profile information"
}
