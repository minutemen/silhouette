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
package silhouette.authenticator.format

import silhouette.Authenticator
import silhouette.authenticator.StatefulWrites

import scala.concurrent.Future

/**
 * A reads which transforms an authenticator into a SAT (simple authentication token).
 *
 * A simple authentication token represents a string that cannot store authenticator related data in it. Instead
 * it needs a mapping between this string and the authenticator related data, which is commonly handled through a
 * backing store. This writes doesn't store the authenticator directly. This should be done in a
 * [[silhouette.authenticator.WritePipeline]] instead.
 */
final case class SatWrites() extends StatefulWrites {

  /**
   * Transforms an [[Authenticator]] into a simple authentication token.
   *
   * @param authenticator The authenticator to transform.
   * @return A simple authentication token on success, an error on failure.
   */
  override def write(authenticator: Authenticator): Future[String] = Future.successful(authenticator.id)
}
