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
import silhouette.http.ResponsePipeline
import silhouette.http.transport.DiscardFromSession

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Discards a stateful session.
 *
 * @param key              The session key in which the authenticator is transported.
 * @param writer           A writer to discard the authenticator from a persistence layer like a database or a cache.
 * @param responsePipeline The response pipeline.
 * @param ec               The execution context.
 * @tparam R The type of the response.
 */
final case class DiscardStatefulSession[R](
  key: String,
  writer: Authenticator => Future[Authenticator]
)(
  implicit
  responsePipeline: ResponsePipeline[R],
  ec: ExecutionContext
) extends WritePipeline[Future[ResponsePipeline[R]]] {

  /**
   * Apply the pipeline.
   *
   * @param authenticator The authenticator.
   * @return The response pipeline that discards the authenticator client side.
   */
  override def apply(authenticator: Authenticator): Future[ResponsePipeline[R]] = {
    WriteToStore(writer)(authenticator).map(_ => DiscardFromSession(key).write)
  }
}

/**
 * Discards a stateless session.
 *
 * @param key              The session key in which the authenticator is transported.
 * @param responsePipeline The response pipeline.
 * @tparam R The type of the response.
 */
final case class DiscardStatelessSession[R](key: String)(
  implicit
  responsePipeline: ResponsePipeline[R]
) extends WritePipeline[ResponsePipeline[R]] {

  /**
   * Apply the pipeline.
   *
   * @param authenticator The authenticator.
   * @return The response pipeline that discards the authenticator client side.
   */
  override def apply(authenticator: Authenticator): ResponsePipeline[R] = DiscardFromSession(key).write
}
