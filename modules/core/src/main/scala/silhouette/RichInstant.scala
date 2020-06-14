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
package silhouette

import java.time.Instant

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }
import scala.language.implicitConversions

/**
 * An extended [[java.time.Instant]] implementation with additional functionality.
 */
class RichInstant(instant: Instant) {

  /**
   * Adds a duration to an instant.
   *
   * @param duration The duration to add.
   * @return An [[java.time.Instant]] instance with the added duration.
   */
  def +(duration: FiniteDuration): Instant =
    instant.plusMillis(duration.toMillis)

  /**
   * Subtracts an instant from an instant and returns the duration of it.
   *
   * @param sub The instant to subtract.
   * @return An [[java.time.Instant]] instance with the added instance.
   */
  def -(sub: Instant): FiniteDuration =
    FiniteDuration(instant.minusMillis(sub.toEpochMilli).toEpochMilli, MILLISECONDS)

  /**
   * Subtracts a duration from an instant.
   *
   * @param duration The duration to subtract.
   * @return A [[java.time.Instant]] instance with the subtracted duration.
   */
  def -(duration: FiniteDuration): Instant =
    instant.minusMillis(duration.toMillis)
}

/**
 * The companion object.
 */
object RichInstant {
  implicit def instantToRichInstant(instant: Instant): RichInstant = new RichInstant(instant)
}
