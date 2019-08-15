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
package silhouette.authenticator

import silhouette.Fitting._
import silhouette.authenticator.{ Reads => AuthenticatorReads, Writes => AuthenticatorWrites }
import silhouette.http.{ EmbedWrites, RequestPipeline, ResponsePipeline, RetrieveReads }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

/**
 * A simple DSL to model authenticator pipelines.
 */
object PipelineDsl {

  /**
   * A DSL for working with [[silhouette.http.RetrieveReads]] implementations.
   *
   * @tparam P The type of the payload.
   */
  trait RetrieveReadsDsl[P] {
    def >>(reads: RetrieveReads[P]): Option[P]
  }

  /**
   * A DSL for working with [[silhouette.http.EmbedWrites]] implementations.
   *
   * @tparam R The type of the request.
   * @tparam P The type of the payload.
   */
  trait EmbedWritesDsl[R, P] {
    def >>(writes: EmbedWrites[R, P]): ResponsePipeline[R] => Future[ResponsePipeline[R]]
  }

  /**
   * A DSL for working with [[silhouette.authenticator.Reads]] implementations.
   *
   * @tparam S The source type that the reads transforms into an [[Authenticator]].
   */
  trait AuthenticatorReadsDsl[S] {
    def >>(reads: AuthenticatorReads[S]): Future[Option[Authenticator]]
  }

  /**
   * A DSL for working with [[silhouette.authenticator.Writes]] implementations.
   *
   * @tparam T The target type to which an [[Authenticator]] will be converted.
   */
  trait AuthenticatorWritesDsl[T] {
    def >>(writes: AuthenticatorWrites[T]): Future[T]
  }

  /**
   * Transforms a [[silhouette.http.RequestPipeline]] into a [[RetrieveReadsDsl]].
   *
   * @param requestPipeline The request pipeline to transform.
   * @tparam T The type of the value the reads returns.
   * @tparam R The type of the request.
   * @return A [[RetrieveReadsDsl]].
   */
  implicit def requestPipelineToRetrieveReadsDsl[T, R](requestPipeline: RequestPipeline[R]): RetrieveReadsDsl[T] = {
    _.read(requestPipeline)
  }

  /**
   * Transforms a value returned from a [[RetrieveReadsDsl]] into a [[AuthenticatorReadsDsl]].
   *
   * @param value The value to transform into an authenticator.
   * @tparam T The type of the value.
   * @return An [[AuthenticatorReadsDsl]].
   */
  implicit def retrieveReadsDslToAuthenticatorReadsDsl[T](value: Option[T]): AuthenticatorReadsDsl[T] = {
    reads => value andThenFuture reads
  }

  /**
   * Transforms an [[Authenticator]] into an [[AuthenticatorWritesDsl]].
   *
   * @param authenticator The authenticator to transform.
   * @tparam T The target type to which an [[Authenticator]] will be converted.
   * @return An [[AuthenticatorWritesDsl]].
   */
  implicit def authenticatorToAuthenticatorWritesDsl[T](authenticator: Authenticator): AuthenticatorWritesDsl[T] = {
    _.write(authenticator)
  }

  /**
   * Transforms a value returned from an [[AuthenticatorWritesDsl]] into a [[EmbedWritesDsl]].
   *
   * @param payload The serialized form of the authenticator to embed into the response.
   * @param ec The implicit execution context.
   * @tparam R The type of the request.
   * @tparam P The type of the payload.
   * @return A [[EmbedWritesDsl]].
   */
  implicit def authenticatorWritesDslToEmbedWritesDsl[R, P](payload: Future[P])(
    implicit
    ec: ExecutionContext
  ): EmbedWritesDsl[R, P] = writes => responsePipeline => payload.map(p => writes.write(p -> responsePipeline))
}
