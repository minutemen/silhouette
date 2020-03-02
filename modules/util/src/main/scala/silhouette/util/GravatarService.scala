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

import java.net.URI
import java.net.URLEncoder._

import cats.Monad
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import silhouette.ConfigURI
import silhouette.crypto.Hash._
import silhouette.util.GravatarService._
import sttp.client._

/**
 * Retrieves avatar URIs from the Gravatar service.
 *
 * @param settings The Gravatar service settings.
 */
final case class GravatarService[F[_]: Monad] @Inject() (settings: GravatarServiceConfig = GravatarServiceConfig())(
  implicit
  val sttpBackend: SttpBackend[F, Nothing, Nothing]
) extends AvatarService[F] with LazyLogging {

  /**
   * Retrieves the URI for the given email address.
   *
   * @param email The email address for the avatar.
   * @return Maybe an avatar URI or None if no avatar could be found for the given email address.
   */
  override def retrieveURI(email: String): F[Option[URI]] = {
    hash(email) match {
      case Some(hash) =>
        val encodedParams = settings.params.map { p => encode(p._1, "UTF-8") + "=" + encode(p._2, "UTF-8") }
        val url = (if (settings.secure) SecureURI else InsecureURI).format(hash, encodedParams.mkString("?", "&", ""))
        Monad[F].map(basicRequest.get(url).send()) { response =>
          response.body match {
            case Left(error) =>
              logger.info(s"Gravatar API returns status `${response.code}` with error: $error")
              None
            case Right(_) =>
              Some(url.toURI)
          }
        }
      case None => Monad[F].pure(None)
    }
  }

  /**
   * Builds the hash for the given email address.
   *
   * @param email The email address to build the hash for.
   * @return Maybe a hash for the given email address or None if the email address is empty.
   */
  private def hash(email: String): Option[String] = {
    val s = email.trim.toLowerCase
    if (s.length > 0) {
      Some(md5(s.getBytes))
    } else {
      None
    }
  }
}

/**
 * The companion object.
 */
object GravatarService {
  val InsecureURI = ConfigURI("http://www.gravatar.com/avatar/%s%s")
  val SecureURI = ConfigURI("https://secure.gravatar.com/avatar/%s%s")
}

/**
 * The gravatar service settings object.
 *
 * @param secure Indicates if the secure or insecure URL should be used to query the avatar images. Defaults to secure.
 * @param params A list of params to append to the URL.
 * @see https://en.gravatar.com/site/implement/images/
 */
case class GravatarServiceConfig(
  secure: Boolean = true,
  params: Map[String, String] = Map("d" -> "404")
)
