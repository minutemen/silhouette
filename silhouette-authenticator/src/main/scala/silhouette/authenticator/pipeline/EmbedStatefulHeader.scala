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
import silhouette.authenticator.{ StatefulWrites, WritePipeline }
import silhouette.http.ResponsePipeline
import silhouette.http.transport.{ EmbedBearerTokenIntoHeader, EmbedIntoHeader }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Uses a stateful approach to embed the authenticator into a header.
 *
 * The stateful approach stores the authenticator in the header and the authenticator instance itself in a server
 * side backing store. On subsequent requests, the ID of the authenticator stored in the header will be mapped to
 * the authenticator in the backing store. This allows to invalidate the authenticators server side.
 *
 * Note: If deploying to multiple nodes, the backing store will need to synchronize.
 *
 * @param authenticatorWrites The writes which transforms the authenticator into a serialized form of the authenticator.
 * @param authenticatorWriter A writer to write the authenticator to a backing store.
 * @param responsePipeline    The response pipeline in which the authenticator should be embedded.
 * @param headerName          The header name in which the authenticator should be embedded.
 * @param ec                  The execution context.
 * @tparam R The type of the response.
 */
case class EmbedStatefulHeader[R](
  authenticatorWrites: StatefulWrites,
  authenticatorWriter: Authenticator => Future[Authenticator],
  responsePipeline: ResponsePipeline[R],
  headerName: String,
)(
  implicit
  ec: ExecutionContext
) extends WritePipeline[Future[ResponsePipeline[R]]] {

  /**
   * Apply the pipeline.
   *
   * @param authenticator The authenticator.
   * @return The response pipeline that contains the embedded authenticator.
   */
  override def apply(authenticator: Authenticator): Future[ResponsePipeline[R]] = {
    authenticatorWriter(authenticator).flatMap { authenticator =>
      authenticatorWrites.write(authenticator).map { payload =>
        EmbedIntoHeader(payload, headerName)(responsePipeline).write
      }
    }
  }
}


/**
 * The same as the [[EmbedStatefulHeader]] approach, expect that it embeds the header in the form:
 * "Authorization: Bearer some.token".
 *
 * @param authenticatorWrites The writes which transforms the authenticator into a serialized form of the authenticator.
 * @param authenticatorWriter A writer to write the authenticator to a backing store.
 * @param responsePipeline    The response pipeline in which the authenticator should be embedded.
 * @param headerName          The header name in which the authenticator should be embedded; Defaults to Authorization.
 * @param ec                  The execution context.
 * @tparam R The type of the response.
 */
case class EmbedStatefulBearerTokenHeader[R](
  authenticatorWrites: StatefulWrites,
  authenticatorWriter: Authenticator => Future[Authenticator],
  responsePipeline: ResponsePipeline[R],
  headerName: String = "Authorization"
)(
  implicit
  ec: ExecutionContext
) extends WritePipeline[Future[ResponsePipeline[R]]] {

  /**
   * Apply the pipeline.
   *
   * @param authenticator The authenticator.
   * @return The response pipeline that contains the embedded authenticator.
   */
  override def apply(authenticator: Authenticator): Future[ResponsePipeline[R]] = {
    authenticatorWriter(authenticator).flatMap { authenticator =>
      authenticatorWrites.write(authenticator).map { payload =>
        EmbedBearerTokenIntoHeader(payload, headerName)(responsePipeline).write
      }
    }
  }
}
