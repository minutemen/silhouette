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

import cats.data.NonEmptyMap

/**
 * Represents the state a social provider can handle.
 *
 * The state consists of a [[cats.data.NonEmptyMap]] containing different state items indexed by the ID of the related
 * [[StateItemHandler]].
 *
 * @param items The social state items.
 */
case class State(items: NonEmptyMap[String, StateItem])
