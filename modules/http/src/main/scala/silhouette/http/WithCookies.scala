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

import sttp.model.CookieWithMeta

/**
 * A trait that provides methods to access cookies.
 */
trait WithCookies {

  /**
   * Gets the list of cookies.
   *
   * @return The list of cookies.
   */
  def cookies: Seq[CookieWithMeta]

  /**
   * Gets a cookie.
   *
   * @param name The name for which the cookie should be returned.
   * @return Some cookie or None if no cookie for the given name could be found.
   */
  def cookie(name: String): Option[CookieWithMeta] = cookies.find(_.name == name)
}
