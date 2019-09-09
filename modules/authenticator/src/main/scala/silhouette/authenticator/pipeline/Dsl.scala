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

import silhouette.Fitting._
import silhouette.authenticator.{ Authenticator, Reads => AuthenticatorReads, Writes => AuthenticatorWrites }
import silhouette.http.{ EmbedWrites, RequestPipeline, ResponsePipeline, RetrieveReads }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

/**
 * A simple DSL to model authenticator pipelines.
 */
object Dsl {

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
   * A DSL for working with [[silhouette.authenticator.pipeline.ModifyStep]] implementations.
   */
  trait ModifyStepDsl {
    def >>(step: ModifyStep): Authenticator
  }

  /**
   * A DSL for working with [[silhouette.authenticator.pipeline.AsyncStep]] implementations.
   */
  trait AsyncStepDsl {
    def >>(step: AsyncStep): Future[Authenticator]
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
   * Transforms an [[Authenticator]] into an [[AuthenticatorWritesDsl]].
   *
   * @param authenticator The authenticator to transform.
   * @tparam T The target type to which an [[Authenticator]] will be converted.
   * @return An [[AuthenticatorWritesDsl]].
   */
  implicit def authenticatorToAuthenticatorWritesDsl[T](authenticator: Authenticator): AuthenticatorWritesDsl[T] =
    _.write(authenticator)

  /**
   * Transforms an [[Authenticator]] into a [[ModifyStepDsl]].
   *
   * @param authenticator The authenticator to transform.
   * @return An [[ModifyStepDsl]].
   */
  implicit def authenticatorToModifyStepDsl(authenticator: Authenticator): ModifyStepDsl =
    _.apply(authenticator)

  /**
   * Transforms an [[Authenticator]] into an [[AsyncStepDsl]].
   *
   * @param authenticator The authenticator to transform.
   * @return An [[AsyncStepDsl]].
   */
  implicit def authenticatorToEffectStepDsl(authenticator: Authenticator): AsyncStepDsl =
    _.apply(authenticator)

  /**
   * Transforms a value returned from a [[RetrieveReadsDsl]] into an [[AuthenticatorReadsDsl]].
   *
   * @param value The value to transform into an authenticator.
   * @tparam T The type of the value.
   * @return An [[AuthenticatorReadsDsl]].
   */
  implicit def retrieveReadsDslToAuthenticatorReadsDsl[T](value: Option[T]): AuthenticatorReadsDsl[T] = {
    reads => value andThenFuture reads
  }

  /**
   * Transforms an [[Authenticator]] returned from an [[ModifyStepDsl]] into an [[AuthenticatorWritesDsl]].
   *
   * @param authenticator The authenticator to transform into T.
   * @tparam T The type of the result.
   * @return An [[AuthenticatorWritesDsl]].
   */
  implicit def modifyStepDslToAuthenticatorWritesDsl[T](
    authenticator: Authenticator
  ): AuthenticatorWritesDsl[T] = {
    writes => writes(authenticator)
  }

  /**
   * Transforms an [[Authenticator]] returned from an [[AsyncStepDsl]] into an [[AuthenticatorWritesDsl]].
   *
   * @param authenticator The authenticator to transform into T.
   * @tparam T The type of the result.
   * @return An [[AuthenticatorWritesDsl]].
   */
  implicit def asyncStepDslToAuthenticatorWritesDsl[T](
    authenticator: Future[Authenticator]
  ): AuthenticatorWritesDsl[T] = {
    writes => authenticator andThenFuture writes
  }

  /**
   * Transforms a value returned from an [[AuthenticatorWritesDsl]] into an [[EmbedWritesDsl]].
   *
   * @param payload The serialized form of the authenticator to embed into the response.
   * @param ec The implicit execution context.
   * @tparam R The type of the request.
   * @tparam P The type of the payload.
   * @return An [[EmbedWritesDsl]].
   */
  implicit def authenticatorWritesDslToEmbedWritesDsl[R, P](payload: Future[P])(
    implicit
    ec: ExecutionContext
  ): EmbedWritesDsl[R, P] = writes => responsePipeline => payload.map(p => writes.write(p -> responsePipeline))
}
