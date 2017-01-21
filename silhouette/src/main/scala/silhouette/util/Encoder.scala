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
 * Represents an encode action that transform an instance of A to B.
 *
 * @tparam A The source type.
 * @tparam B The target type.
 */
trait Encoder[A, B] {

  /**
   * Encode from source A to target B.
   *
   * @param in The source to encode.
   * @return An instance of B.
   */
  def encode(in: A): B
}
