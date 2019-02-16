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

import silhouette.authenticator.{ Authenticator, StatefulWrites, WritePipeline }
import silhouette.http.{ EmbedWrites, ResponsePipeline }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Uses a stateful approach to embed the [[Authenticator]] into the response.
 *
 * The stateful approach stores the [[Authenticator]] instance itself in a server side backing store. On subsequent
 * requests the [[Authenticator]] ID which is stored in the serialized [[Authenticator]] can be validated against the
 * backing store. This stateful approach could also be named “server side session”.
 *
 * Note: If deploying to multiple nodes, the backing store will need to synchronize.
 *
 * @param statefulWriter      A writer to write the stateful [[Authenticator]] to a backing store.
 * @param authenticatorWrites A writes that transforms the [[Authenticator]] into a serialized form of
 *                            the [[Authenticator]].
 * @param embedWrites         A writes that embeds the [[Authenticator]] into the [[silhouette.http.ResponsePipeline]].
 * @param ec                  The execution context.
 * @tparam R The type of the response.
 */
final case class EmbedStatefulPipeline[R](
  statefulWriter: Authenticator => Future[Authenticator],
  authenticatorWrites: StatefulWrites[String],
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
  override def write(in: (Authenticator, ResponsePipeline[R])): Future[ResponsePipeline[R]] = {
    statefulWriter(in._1).flatMap { authenticator =>
      authenticatorWrites.write(authenticator).map { payload =>
        embedWrites.write(payload -> in._2)
      }
    }
  }
}
