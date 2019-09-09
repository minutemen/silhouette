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
package silhouette.provider.oauth2

import java.net.URI
import java.time.Clock

import io.circe.Json
import silhouette.http.Method.GET
import silhouette.http.client.Request
import silhouette.http.{ HttpClient, Status }
import silhouette.provider.UnexpectedResponseException
import silhouette.provider.oauth2.GoogleProvider._
import silhouette.provider.oauth2.OAuth2Provider._
import silhouette.provider.social._
import silhouette.provider.social.state.StateHandler
import silhouette.{ ConfigURI, LoginInfo, RichACursor }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Base Google OAuth2 Provider.
 *
 * @see https://developers.google.com/people/api/rest/v1/people/get
 * @see https://developers.google.com/people/v1/how-tos/authorizing
 * @see https://developers.google.com/identity/protocols/OAuth2
 */
trait BaseGoogleProvider extends OAuth2Provider {

  /**
   * The provider ID.
   */
  override val id = ID

  /**
   * Builds the social profile.
   *
   * @param authInfo The auth info received from the provider.
   * @return On success the build social profile, otherwise a failure.
   */
  override protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {
    val uri = config.apiURI.getOrElse(DefaultApiURI).format(authInfo.accessToken)

    httpClient.execute(Request(GET, uri)).flatMap { response =>
      withParsedJson(response) { json =>
        response.status match {
          case Status.OK =>
            profileParser.parse(json, authInfo)
          case status =>
            Future.failed(new UnexpectedResponseException(UnexpectedResponse.format(id, json, status)))
        }
      }
    }
  }
}

/**
 * The profile parser for the common social profile.
 */
class GoogleProfileParser(implicit val ec: ExecutionContext)
  extends SocialProfileParser[Json, CommonSocialProfile, OAuth2Info] {

  /**
   * Parses the social profile.
   *
   * @see https://developers.google.com/people/api/rest/v1/people#Person.Name
   * @see https://developers.google.com/people/api/rest/v1/people#Person.EmailAddress
   * @see https://developers.google.com/people/api/rest/v1/people#Person.Photo
   *
   * @param json     The content returned from the provider.
   * @param authInfo The auth info to query the provider again for additional data.
   * @return The social profile from the given result.
   */
  override def parse(json: Json, authInfo: OAuth2Info): Future[CommonSocialProfile] = {
    json.hcursor.downField("names").downAt(isPrimary).focus.map(_.hcursor) match {
      case Some(names) =>
        val maybeID = names.downField("metadata").downField("source").downField("id").as[String]
        Future.fromTry(maybeID.getOrError(names.value, "metadata.source.id", ID)).map { id =>
          val email = json.hcursor.downField("emailAddresses").downAt(isPrimary).downField("value").as[String].toOption
          val maybePrimaryPhoto = json.hcursor.downField("photos").downAt(isPrimary).focus
          val maybeAvatarURL = maybePrimaryPhoto.flatMap(
            _.hcursor.downField("url").as[String].toOption.map(uri => new URI(uri))
          )
          val isDefaultAvatar = maybePrimaryPhoto.exists(
            _.hcursor.downField("default").as[Boolean].toOption.getOrElse(false)
          )

          CommonSocialProfile(
            loginInfo = LoginInfo(ID, id),
            firstName = names.downField("givenName").as[String].toOption,
            lastName = names.downField("familyName").as[String].toOption,
            fullName = names.downField("displayName").as[String].toOption,
            email = email,
            avatarUri = if (isDefaultAvatar) None else maybeAvatarURL // skip the default avatar picture
          )

        }
      case None =>
        Future.failed(new UnexpectedResponseException(NoPrimaryEntry.format(ID, "names", json)))
    }
  }

  /**
   * Indicates if the entry in the array is the primary entry.
   *
   * The profile contains many categories, each containing an array that contains one or more entries to the category.
   * The primary entry is marked with the "metadata.primary" property. This function indicates if the traversed entry
   * is the primary entry, by checking if the "metadata.primary" property is set to true.
   *
   * @param json The entry from a JSON array.
   * @return True if the entry is the primary entry, false otherwise.
   */
  private def isPrimary(json: Json): Boolean = {
    json.hcursor.downField("metadata").downField("primary").as[Boolean].toOption.getOrElse(false)
  }
}

/**
 * The Google OAuth2 Provider.
 *
 * @param httpClient   The HTTP client implementation.
 * @param stateHandler The state provider implementation.
 * @param clock        The current clock instance.
 * @param config       The provider config.
 * @param ec           The execution context.
 */
class GoogleProvider(
  protected val httpClient: HttpClient,
  protected val stateHandler: StateHandler,
  protected val clock: Clock,
  val config: OAuth2Config
)(
  implicit
  override implicit val ec: ExecutionContext
) extends BaseGoogleProvider with CommonProfileBuilder {

  /**
   * The type of this class.
   */
  override type Self = GoogleProvider

  /**
   * The profile parser implementation.
   */
  override val profileParser = new GoogleProfileParser

  /**
   * Gets a provider initialized with a new config object.
   *
   * @param f A function which gets the config passed and returns different config.
   * @return An instance of the provider initialized with new config.
   */
  override def withConfig(f: OAuth2Config => OAuth2Config): Self =
    new GoogleProvider(httpClient, stateHandler, clock, f(config))
}

/**
 * The companion object.
 */
object GoogleProvider {

  /**
   * The provider ID.
   */
  val ID = "google"

  /**
   * Default provider endpoint.
   */
  val DefaultApiURI = ConfigURI("https://people.googleapis.com/v1/people/me?personFields=names,photos,emailAddresses" +
    "&access_token=%s")

  /**
   * The error messages.
   */
  val NoPrimaryEntry = "Couldn't find a primary entry for path `%s` in Json: %s"
}
