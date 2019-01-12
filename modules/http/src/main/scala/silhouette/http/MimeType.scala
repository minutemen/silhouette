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
package silhouette.http

import scala.language.implicitConversions

/**
 * Represents a HTTP MIME type.
 *
 * @param value The MIME type as string.
 */
case class MimeType(value: String)

/**
 * Common HTTP MIME types used by Silhouette.
 */
object MimeType {
  implicit def toString(mimeType: MimeType): String = mimeType.value
  implicit def fromString(mimeType: String): MimeType = MimeType(mimeType)

  final val `text/plain` = MimeType("text/plain")
  final val `text/json` = MimeType("text/json")
  final val `text/xml` = MimeType("text/xml")
  final val `application/x-www-form-urlencoded` = MimeType("application/x-www-form-urlencoded")
  final val `application/json` = MimeType("application/json")
  final val `application/xml` = MimeType("application/xml")
}
