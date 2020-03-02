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

import scala.collection.compat.immutable.ArraySeq
import scala.language.implicitConversions

/**
 * Represents a HTTP status.
 *
 * @param code         HTTP status code.
 * @param reasonPhrase A textual representation of the status code.
 */
case class Status(code: Int, reasonPhrase: String) {
  override def toString: String = s"$code ($reasonPhrase)"
}

/**
 * Common HTTP statuses.
 *
 * @see https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
 */
object Status {

  /**
   * Contains a list of all status instances. Will be initialized lazy if needed. The list is needed to determine the
   * status instance from only a status code.
   *
   * @see fromInt
   */
  private lazy val statuses: Seq[Status] = {
    ArraySeq.unsafeWrapArray(this.getClass.getDeclaredFields).filter(_.getType == classOf[Status])
      .map(_.get(this).asInstanceOf[Status])
  }

  implicit def toInt(status: Status): Int = status.code
  implicit def fromInt(code: Int): Status = statuses.find(status => status.code == code)
    .getOrElse(Status(code, "Unofficial code"))

  final val `Continue` = Status(100, "Continue")
  final val `Switching Protocols` = Status(101, "Switching Protocols")
  final val `Processing` = Status(102, "Processing")
  final val `Early Hints` = Status(103, "Early Hints")

  final val `OK` = Status(200, "OK")
  final val `Created` = Status(201, "Created")
  final val `Accepted` = Status(202, "Accepted")
  final val `Non-Authoritative Information` = Status(203, "Non-Authoritative Information")
  final val `No Content` = Status(204, "No Content")
  final val `Reset Content` = Status(205, "Reset Content")
  final val `Partial Content` = Status(206, "Partial Content")
  final val `Multi-Status` = Status(207, "Multi-Status")
  final val `Already Reported` = Status(208, "Already Reported")
  final val `IM Used` = Status(226, "IM Used")

  final val `Multiple Choices` = Status(300, "Multiple Choices")
  final val `Moved Permanently` = Status(301, "Moved Permanently")
  final val `Found` = Status(302, "Found")
  final val `See Other` = Status(303, "See Other")
  final val `Not Modified` = Status(304, "Not Modified")
  final val `Use Proxy` = Status(305, "Use Proxy")
  final val `Switch Proxy` = Status(306, "Switch Proxy")
  final val `Temporary Redirect` = Status(307, "Temporary Redirect")
  final val `Permanent Redirect` = Status(308, "Permanent Redirect")

  final val `Bad Request` = Status(400, "Bad Request")
  final val `Unauthorized` = Status(401, "Unauthorized")
  final val `Payment Required` = Status(402, "Payment Required")
  final val `Forbidden` = Status(403, "Forbidden")
  final val `Not Found` = Status(404, "Not Found")
  final val `Method Not Allowed` = Status(405, "Method Not Allowed")
  final val `Not Acceptable` = Status(406, "Not Acceptable")
  final val `Proxy Authentication Required` = Status(407, "Proxy Authentication Required")
  final val `Request Timeout` = Status(408, "Request Timeout")
  final val `Conflict` = Status(409, "Conflict")
  final val `Gone` = Status(410, "Gone")
  final val `Length Required` = Status(411, "Length Required")
  final val `Precondition Failed` = Status(412, "Precondition Failed")
  final val `Payload Too Large` = Status(413, "Payload Too Large")
  final val `URI Too Long` = Status(414, "URI Too Long")
  final val `Unsupported Media Type` = Status(415, "Unsupported Media Type")
  final val `Range Not Satisfiable` = Status(416, "Range Not Satisfiable")
  final val `Expectation Failed` = Status(417, "Expectation Failed")
  final val `I'm a teapot` = Status(418, "I'm a teapot")
  final val `Misdirected Request` = Status(421, "Misdirected Request")
  final val `Unprocessable Entity` = Status(422, "Unprocessable Entity")
  final val `Locked` = Status(423, "Locked")
  final val `Failed Dependency` = Status(424, "Failed Dependency")
  final val `Upgrade Required` = Status(426, "Upgrade Required")
  final val `Precondition Required` = Status(428, "Precondition Required")
  final val `Too Many Requests` = Status(429, "Too Many Requests")
  final val `Request Header Fields Too Large` = Status(431, "Request Header Fields Too Large")
  final val `Unavailable For Legal Reasons` = Status(451, "Unavailable For Legal Reasons")

  final val `Internal Server Error` = Status(500, "Internal Server Error")
  final val `Not Implemented` = Status(501, "Not Implemented")
  final val `Bad Gateway` = Status(502, "Bad Gateway")
  final val `Service Unavailable` = Status(503, "Service Unavailable")
  final val `Gateway Timeout` = Status(504, "Gateway Timeout")
  final val `HTTP Version Not Supported` = Status(505, "HTTP Version Not Supported")
  final val `Variant Also Negotiates` = Status(506, "Variant Also Negotiates")
  final val `Insufficient Storage` = Status(507, "Insufficient Storage")
  final val `Loop Detected` = Status(508, "Loop Detected")
  final val `Not Extended` = Status(510, "Not Extended")
  final val `Network Authentication Required` = Status(511, "Network Authentication Required")
}
