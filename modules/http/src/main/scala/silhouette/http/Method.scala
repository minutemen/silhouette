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

/**
 * Represents a HTTP request method.
 *
 * @param value The request method as string.
 */
protected[silhouette] sealed abstract class Method(val value: String)

/**
 * The HTTP request methods.
 */
private[silhouette] object Method {
  case object GET extends Method("GET")
  case object POST extends Method("POST")
  case object PUT extends Method("PUT")
  case object PATCH extends Method("PATCH")
  case object DELETE extends Method("DELETE")
  case object HEAD extends Method("HEAD")
  case object OPTION extends Method("OPTION")
  case object CONNECT extends Method("CONNECT")
  case object TRACE extends Method("TRACE")
}
