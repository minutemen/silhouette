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
package silhouette.authenticator

import silhouette.util.Source
import silhouette.{ Authenticator, Identity }

import scala.concurrent.Future

/**
 * An authenticator pipeline represents a step in the authentication process which is composed of multiple single steps.
 */
sealed trait Pipeline

/**
 * Pipeline to authenticate an identity by transforming a source into an authentication state.
 *
 * An authenticator can be read from different sources. The most common case would be to read the authenticator
 * from the request. But it would also be possible to read the authenticator from an actor or any other source.
 * If the source holds a serialized representation of an authenticator, we must transform it into an authenticator
 * instance by applying an authenticator [[Reads]]. Then we could check the validity of the authenticator by applying
 * one or multiple validators. As next we retrieve an [[Identity]] for our authenticator, either from a backing store
 * or any other service. At last we could apply some authorization rules to our identity. Any of these steps in our
 * pipeline can be by represented by a state that describes the result of each step.
 *
 * @param pipeline The authentication pipeline.
 * @tparam S The type of the source.
 * @tparam I The type of the identity.
 */
final case class AuthenticationPipeline[S, I <: Identity](pipeline: Source[S] => Future[State[I]]) extends Pipeline

/**
 * Pipeline which transforms an authenticator into another authenticator.
 *
 * @param pipeline The transform pipeline.
 */
final case class TransformPipeline(pipeline: Authenticator => Future[Authenticator]) extends Pipeline

/**
 * Pipeline which writes an authenticator to a target.
 *
 * @param pipeline The write pipeline.
 * @tparam T The type of the target.
 */
final case class WritePipeline[T](pipeline: Authenticator => T) extends Pipeline
