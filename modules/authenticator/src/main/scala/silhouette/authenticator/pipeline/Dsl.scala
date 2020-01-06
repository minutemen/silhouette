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

import cats.effect.Sync
import silhouette.Maybe.{ Maybe, MaybeWriter }

import scala.language.implicitConversions

/**
 * A simple DSL to model authenticator pipelines.
 */
object Dsl {

  trait ReaderPipeline[F[_], A, B] extends (A => Maybe[F, B]) { self =>

    /**
     * Composes this reader with a transformation function that gets applied to the result of this reader.
     *
     * @param reads The transformation function.
     * @tparam C The result type of the new transformation function.
     * @return A new composable reader.
     */
    def >>[C](reads: ReaderPipeline[F, B, C])(implicit F: Sync[F]): ReaderPipeline[F, A, C] = { x =>
      self.apply(x).flatMap(reads.apply)
    }
  }

  implicit def toReadsPipeline[F[_], A, B, C](reader: A => B)(
    implicit
    maybeWrites: MaybeWriter[F, B, C]
  ): ReaderPipeline[F, A, C] = {
    in: A => maybeWrites(reader(in))
  }
}
