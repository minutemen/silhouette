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
package silhouette.http.client.decoder

import silhouette.http.client.Body
import silhouette.util.Decoder

import scala.util.Try

/**
 * Represents a decode action that extract from [[Body]] to an instance of T.
 *
 * @tparam T The target type.
 */
trait BodyDecoder[T] extends Decoder[Body, Try[T]]

/**
 * The only aim of this object is to provide a default implicit [[BodyDecoder]], that uses
 * the [[DefaultBodyDecoder]] trait to provide the lowest implicit conversion chain.
 */
object BodyDecoder extends DefaultBodyDecoder
