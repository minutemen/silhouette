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
package silhouette.authenticator.pipeline

import silhouette.Authenticator
import silhouette.authenticator.WritePipeline

import scala.concurrent.Future

/**
 * Writes an authenticator to the store.
 *
 * This write can be an insert, an update or a delete to a persistence layer like a database or a cache.
 *
 * @param writer A writer to write the authenticator to a persistence layer like a database or a cache.
 */
case class WriteToStore(writer: Authenticator => Future[Authenticator])
  extends WritePipeline[Future[Authenticator]] {

  /**
   * Apply the pipeline.
   *
   * @param authenticator The authenticator.
   * @return The authenticator returned from the writer function.
   */
  override def apply(authenticator: Authenticator): Future[Authenticator] = writer(authenticator)
}
