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
package silhouette

/**
 * A type that signals that an operation is done.
 *
 * If an operation returns nothing but it should be signaled that the operation is done, this type could be helpful
 * instead of returning [[scala.Unit]]. That's especially helpful when you return a [[scala.concurrent.Future]] that
 * handles no special type.
 */
trait Done

/**
 * An instance of the [[Done]] type.
 */
case object Done extends Done
