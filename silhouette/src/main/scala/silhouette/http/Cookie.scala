/**
 * Copyright 2016 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * An HTTP cookie.
 *
 * We do not use the `Expires` attribute because Play doesn't support it and `Max-Age` is the newer standard.
 *
 * @param name The cookie name.
 * @param value The cookie value.
 * @param maxAge The cookie expiration date in seconds, `None` for a transient cookie, or a value less than 0 to expire a cookie now.
 * @param domain The cookie domain.
 * @param path The the cookie path.
 * @param secure Whether this cookie is secured, sent only for HTTPS requests.
 * @param httpOnly Whether this cookie is HTTP only, i.e. not accessible from client-side JavaScript code.
 */
case class Cookie(
  name: String,
  value: String,
  maxAge: Option[Int] = None,
  domain: Option[String] = None,
  path: Option[String] = None,
  secure: Boolean = false,
  httpOnly: Boolean = false)
