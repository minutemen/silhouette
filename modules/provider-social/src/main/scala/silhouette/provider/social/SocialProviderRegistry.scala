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
package silhouette.provider.social

import scala.reflect.{ classTag, ClassTag }

/**
 * A registry that holds and provides access to all social provider implementations.
 *
 * @param providers The list of social providers.
 */
case class SocialProviderRegistry[F[_]](providers: Seq[SocialProvider[F, _]]) {

  /**
   * Gets a specific provider by its type.
   *
   * @tparam T The type of the provider.
   * @return Some specific provider type or None if no provider for the given type could be found.
   */
  def get[T <: SocialProvider[F, _]: ClassTag]: Option[T] =
    providers.find(p => classTag[T].runtimeClass.isInstance(p)).map(_.asInstanceOf[T])

  /**
   * Gets a specific provider by its ID.
   *
   * @param id The ID of the provider to return.
   * @return Some social provider or None if no provider for the given ID could be found.
   */
  def get[T <: SocialProvider[F, _]: ClassTag](id: String): Option[T] = getSeq[T].find(_.id == id)

  /**
   * Gets a list of providers that match a certain type.
   *
   * @tparam T The type of the provider.
   * @return A list of providers that match a certain type.
   */
  def getSeq[T <: SocialProvider[F, _]: ClassTag]: Seq[T] =
    providers.filter(p => classTag[T].runtimeClass.isInstance(p)).map(_.asInstanceOf[T])
}
