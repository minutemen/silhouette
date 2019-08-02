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
import silhouette.authenticator.{ Reads => AuthenticatorReads }
import silhouette.http.{ RequestPipeline, RetrieveReads }

import scala.concurrent.Future
import scala.language.implicitConversions

/**
 * A simple DSL to model authenticator pipelines.
 */
object Dsl {

  /**
   * A DSL for working with [[silhouette.http.RetrieveReads]] implementations.
   *
   * @tparam T The type of the value the reads returns.
   */
  trait RetrieveDsl[T] {
    def >>(reads: RetrieveReads[T]): Option[T]
  }

  /**
   * A DSL for working with [[silhouette.authenticator.Reads]] implementations.
   *
   * @tparam T The type of the value that the reads transforms into an [[Authenticator]].
   */
  trait TransformDsl[T] {
    def >>(reads: AuthenticatorReads[T]): Future[Option[Authenticator]]
  }

  /**
   * Transforms a [[silhouette.http.RequestPipeline]] into a [[RetrieveDsl]].
   *
   * @param requestPipeline The request pipeline to transform.
   *
   * @tparam T The type of the value the reads returns.
   * @tparam R The type of the request.
   * @return A [[RetrieveDsl]].
   */
  implicit def requestPipelineToRetrieveDsl[T, R](requestPipeline: RequestPipeline[R]): RetrieveDsl[T] = {
    reads: RetrieveReads[T] => reads.read(requestPipeline)
  }

  /**
   * Transforms a value returned from a [[RetrieveDsl]] into a [[TransformDsl]].
   *
   * @param value The value to transform into an authenticator.
   * @tparam T The type of the value.
   * @return A [[TransformDsl]].
   */
  implicit def retrieveDslToTransformDsl[T](value: Option[T]): TransformDsl[T] = {
    reads: AuthenticatorReads[T] => value andThenFuture reads
  }
}
