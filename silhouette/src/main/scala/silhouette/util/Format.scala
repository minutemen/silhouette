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
package silhouette.util

import scala.util.{ Success, Try }

/**
 * Represents a reader that transform an instance of A to B.
 *
 * @tparam A The source type.
 * @tparam B The target type.
 */
trait Reads[A, B] {

  /**
   * Transforms from source A to target B.
   *
   * @param in The source to transform.
   * @return An instance of B.
   */
  def read(in: A): B
}

/**
 * Represents a writer that transform an instance of A to B.
 *
 * @tparam A The source type.
 * @tparam B The target type.
 */
trait Writes[A, B] {

  /**
   * Transforms from source A to target B.
   *
   * @param in The source to transform.
   * @return An instance of B.
   */
  def write(in: A): B
}

/**
 * Represents a transform operation which transforms between A and B.
 *
 * This is a default format which assumes that the read operation can throw an [[scala.Exception]] and the
 * write operation doesn't throw an exception. This is the most common case. For special cases you
 * should declare a specific format.
 *
 * @tparam A The source type on the read operation and the target type on the write operation.
 * @tparam B The target type on the read operation and the source type on the write operation.
 */
trait Format[A, B] extends Reads[A, Try[B]] with Writes[B, A]

/**
 * Some default formats.
 */
trait DefaultFormats {

  /**
   * Transforms from [[scala.Predef.String]] to [[scala.Predef.String]].
   */
  implicit val stringFormat: Format[String, String] = new Format[String, String] {

    /**
     * Transforms from source A to target B.
     *
     * @param in The source to transform.
     * @return An instance of B.
     */
    override def read(in: String): Try[String] = Success(in)

    /**
     * Transforms from source A to target B.
     *
     * @param in The source to transform.
     * @return An instance of B.
     */
    override def write(in: String): String = in
  }
}

/**
 * Provides the implicit default formats.
 */
object Format extends DefaultFormats
