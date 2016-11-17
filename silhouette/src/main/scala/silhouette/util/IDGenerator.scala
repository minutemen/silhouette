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

import scala.concurrent.Future

/**
 * A generator which creates an ID.
 */
trait IDGenerator {

  /**
   * Generates an ID.
   *
   * Generating secure IDs can block the application, while the system waits for resources. Therefore we
   * return a future so that the application doesn't get blocked while waiting for the generated ID.
   *
   * @return The generated ID.
   */
  def generate: Future[String]
}
