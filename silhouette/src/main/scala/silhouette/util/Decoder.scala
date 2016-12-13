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
package silhouette.util

/**
 * Represents a decode action that extract from A an instance of B.
 *
 * @tparam A The raw data source of decode action.
 * @tparam B The type target of decode action.
 */
trait Decoder[A, B] {

  /**
   * Decode from A to target B.
   *
   * @param in The raw data source of decode action.
   * @return An instance of B.
   */
  def decode(in: A): B
}
