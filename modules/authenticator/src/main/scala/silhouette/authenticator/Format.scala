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

import scala.concurrent.Future

/**
 * Transforms a source into an [[Authenticator]].
 *
 * @tparam S The source type.
 */
trait Reads[S] extends silhouette.Reads[S, Future[Authenticator]]

/**
 * Transforms an [[Authenticator]] into a target.
 *
 * @tparam T The target type.
 */
trait Writes[T] extends silhouette.Writes[Authenticator, Future[T]]

/**
 * A marker trait for a [[Reads]] that can transform a stateful [[Authenticator]].
 *
 * @tparam S The source type.
 */
trait StatefulReads[S] extends Reads[S]

/**
 * A marker trait for a [[Writes]] that can transform a stateful [[Authenticator]].
 *
 * @tparam T The target type.
 */
trait StatefulWrites[T] extends Writes[T]

/**
 * A marker trait for a [[Reads]] that can transform a stateless [[Authenticator]].
 *
 * @tparam S The source type.
 */
trait StatelessReads[S] extends Reads[S]

/**
 * A marker trait for a [[Writes]] that can transform a stateless [[Authenticator]].
 *
 * @tparam T The target type.
 */
trait StatelessWrites[T] extends Writes[T]
