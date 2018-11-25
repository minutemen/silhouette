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

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import silhouette.crypto.Hash._
import silhouette.http.{ HttpClient, Method, Status }
import silhouette.util.GravatarService._
import silhouette.{ ConfigURI, ExecutionContextProvider }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Retrieves avatar URIs from the Gravatar service.
 *
 * @param httpClient The HTTP client implementation.
 * @param settings   The Gravatar service settings.
 */
class GravatarService @Inject() (
  httpClient: HttpClient,
  settings: GravatarServiceConfig = GravatarServiceConfig()
)(
  implicit
  val ec: ExecutionContext
) extends AvatarService with LazyLogging with ExecutionContextProvider {

  /**
   * Retrieves the URI for the given email address.
   *
   * @param email The email address for the avatar.
   * @return Maybe an avatar URI or None if no avatar could be found for the given email address.
   */
  override def retrieveURI(email: String): Future[Option[URI]] = {
    hash(email) match {
      case Some(hash) =>
        val encodedParams = settings.params.map { p => encode(p._1, "UTF-8") + "=" + encode(p._2, "UTF-8") }
        val url = (if (settings.secure) SecureURI else InsecureURI).format(hash, encodedParams.mkString("?", "&", ""))
        httpClient.withUri(url).withMethod(Method.GET).execute.map {
          _.status match {
            case Status.OK => Some(url.toURI)
            case status =>
              logger.info("Gravatar API returns status: " + status)
              None
          }
        }.recover {
          case e =>
            logger.info("Error invoking gravatar API", e)
            None
        }
      case None => Future.successful(None)
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
