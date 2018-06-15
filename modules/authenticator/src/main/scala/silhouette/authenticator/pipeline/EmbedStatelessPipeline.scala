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

import silhouette.authenticator.{ Authenticator, StatelessWrites, WritePipeline }
import silhouette.http.{ EmbedWrites, ResponsePipeline }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Uses a stateless approach to embed the [[Authenticator]] into the response.
 *
 * The stateless approach stores a serialized form of the [[Authenticator]] in the [[silhouette.http.ResponsePipeline]].
 * It doesn't need a backing store and it's more scalable. This stateless approach could also be named “client side
 * session”.
 *
 * @param authenticatorWrites A writes that transforms the [[Authenticator]] into a serialized form of
 *                            the [[Authenticator]].
 * @param embedWrites         A writes that embeds the [[Authenticator]] into the [[silhouette.http.ResponsePipeline]].
 * @param ec                  The execution context.
 * @tparam R The type of the response.
 */
final case class EmbedStatelessPipeline[R](
  authenticatorWrites: StatelessWrites,
  embedWrites: EmbedWrites[R, String]
)(
  implicit
  ec: ExecutionContext
) extends WritePipeline[ResponsePipeline[R]] {

  /**
   * Merges an [[Authenticator]] and a target [[silhouette.http.ResponsePipeline]] into a target
   * [[silhouette.http.ResponsePipeline]] that contains the given [[Authenticator]] in a serialized form.
   *
   * @param in A tuple consisting of the [[Authenticator]] to embed and the [[silhouette.http.ResponsePipeline]] in
   *           which the [[Authenticator]] should be embedded.
   * @return The response pipeline with the embedded [[Authenticator]].
   */
  override def write(in: (Authenticator, ResponsePipeline[R])): Future[ResponsePipeline[R]] =
    authenticatorWrites.write(in._1).map(payload => embedWrites.write(payload -> in._2))
}
