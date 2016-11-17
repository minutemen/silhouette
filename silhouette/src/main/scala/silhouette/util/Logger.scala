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

import org.slf4j.{ LoggerFactory, Logger => SLF4JLogger }

object Logger {
  def apply(logger: SLF4JLogger): Logger = new Logger(logger)
  def apply(name: String): Logger = apply(LoggerFactory.getLogger(name))
  def apply[T](clazz: Class[T]): Logger = apply(clazz.getName)
  def apply(obj: AnyRef): Logger = apply(obj.getClass)
}

final class Logger private (underlying: SLF4JLogger) {
  def name: String = underlying.getName

  def isTraceEnabled: Boolean = underlying.isTraceEnabled
  def isDebugEnabled: Boolean = underlying.isDebugEnabled
  def isInfoEnabled: Boolean = underlying.isInfoEnabled
  def isWarnEnabled: Boolean = underlying.isWarnEnabled
  def isErrorEnabled: Boolean = underlying.isErrorEnabled

  def trace(message: => String): Unit = if (isTraceEnabled) underlying.trace(message)
  def trace(message: => String, cause: Throwable): Unit = if (isTraceEnabled) underlying.trace(message, cause)

  def debug(message: => String): Unit = if (isDebugEnabled) underlying.debug(message)
  def debug(message: => String, cause: Throwable): Unit = if (isDebugEnabled) underlying.debug(message, cause)

  def info(message: => String): Unit = if (isInfoEnabled) underlying.info(message)
  def info(message: => String, cause: Throwable): Unit = if (isInfoEnabled) underlying.info(message, cause)

  def warn(message: => String): Unit = if (isWarnEnabled) underlying.warn(message)
  def warn(message: => String, cause: Throwable): Unit = if (isWarnEnabled) underlying.warn(message, cause)

  def error(message: => String): Unit = if (isErrorEnabled) underlying.error(message)
  def error(message: => String, cause: Throwable): Unit = if (isErrorEnabled) underlying.error(message, cause)

}

/**
 * Implement this to get a named logger in scope.
 */
trait Logging {

  /**
   * A named logger instance.
   */
  @transient
  protected final lazy val logger = Logger(getClass)
}
