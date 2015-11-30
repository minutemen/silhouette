/**
 * Copyright 2015 Mohiva Organisation (license at mohiva dot com)
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
package com.mohiva.silhouette.authenticators

import com.mohiva.silhouette.Authenticator.Implicits._
import com.mohiva.silhouette.authenticators.SessionAuthenticatorService._
import com.mohiva.silhouette.exceptions._
import com.mohiva.silhouette.http.{ RequestPipeline, ResponsePipeline }
import com.mohiva.silhouette.services.AuthenticatorService
import com.mohiva.silhouette.services.AuthenticatorService._
import com.mohiva.silhouette.util.JsonFormats._
import com.mohiva.silhouette.util.{ Clock, Crypto, FingerprintGenerator, Logging }
import com.mohiva.silhouette.{ Authenticator, ExpirableAuthenticator, LoginInfo }
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }

/**
 * An authenticator that uses a stateless, session based approach. It works by storing a
 * serialized authenticator instance in a session.
 *
 * The authenticator can use sliding window expiration. This means that the authenticator times
 * out after a certain time if it wasn't used. This can be controlled with the [[idleTimeout]]
 * property.
 *
 * @param loginInfo The linked login info for an identity.
 * @param lastUsedDateTime The last used date/time.
 * @param expirationDateTime The expiration date/time.
 * @param idleTimeout The duration an authenticator can be idle before it timed out.
 * @param fingerprint Maybe a fingerprint of the user.
 */
case class SessionAuthenticator(
  loginInfo: LoginInfo,
  lastUsedDateTime: DateTime,
  expirationDateTime: DateTime,
  idleTimeout: Option[FiniteDuration],
  fingerprint: Option[String])
  extends Authenticator with ExpirableAuthenticator {

  /**
   * The Type of the generated value an authenticator will be serialized to.
   */
  override type Value = (String, String)
}

/**
 * The companion object of the authenticator.
 */
object SessionAuthenticator extends Logging {

  /**
   * Converts the SessionAuthenticator to Json and vice versa.
   */
  implicit val jsonFormat = Json.format[SessionAuthenticator]

  /**
   * Serializes the authenticator.
   *
   * @param authenticator The authenticator to serialize.
   * @param settings The authenticator settings.
   * @return The serialized authenticator.
   */
  def serialize(authenticator: SessionAuthenticator)(settings: SessionAuthenticatorSettings) = {
    if (settings.encryptAuthenticator) {
      Crypto.encryptAES(Json.toJson(authenticator).toString())
    } else {
      Crypto.encodeBase64(Json.toJson(authenticator))
    }
  }

  /**
   * Unserializes the authenticator.
   *
   * @param str The string representation of the authenticator.
   * @param settings The authenticator settings.
   * @return Some authenticator on success, otherwise None.
   */
  def unserialize(str: String)(settings: SessionAuthenticatorSettings): Try[SessionAuthenticator] = {
    if (settings.encryptAuthenticator) buildAuthenticator(Crypto.decryptAES(str))
    else buildAuthenticator(Crypto.decodeBase64(str))
  }

  /**
   * Builds the authenticator from Json.
   *
   * @param str The string representation of the authenticator.
   * @return Some authenticator on success, otherwise None.
   */
  private def buildAuthenticator(str: String): Try[SessionAuthenticator] = {
    Try(Json.parse(str)) match {
      case Success(json) => json.validate[SessionAuthenticator].asEither match {
        case Left(error) => Failure(new AuthenticatorException(InvalidJsonFormat.format(ID, error)))
        case Right(authenticator) => Success(authenticator)
      }
      case Failure(error) => Failure(new AuthenticatorException(JsonParseError.format(ID, str), error))
    }
  }
}

/**
 * The service that handles the session authenticator.
 *
 * @param settings The authenticator settings.
 * @param fingerprintGenerator The fingerprint generator implementation.
 * @param clock The clock implementation.
 * @param executionContext The execution context to handle the asynchronous operations.
 */
class SessionAuthenticatorService(
  settings: SessionAuthenticatorSettings,
  fingerprintGenerator: FingerprintGenerator,
  clock: Clock)(implicit val executionContext: ExecutionContext)
  extends AuthenticatorService[SessionAuthenticator]
  with Logging {

  import SessionAuthenticator._

  /**
   * Creates a new authenticator for the specified login info.
   *
   * @param loginInfo The login info for which the authenticator should be created.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return An authenticator.
   */
  override def create[R](loginInfo: LoginInfo)(implicit request: RequestPipeline[R]): Future[SessionAuthenticator] = {
    Future.fromTry(Try {
      val now = clock.now
      SessionAuthenticator(
        loginInfo = loginInfo,
        lastUsedDateTime = now,
        expirationDateTime = now + settings.authenticatorExpiry,
        idleTimeout = settings.authenticatorIdleTimeout,
        fingerprint = if (settings.useFingerprinting) Some(fingerprintGenerator.generate) else None
      )
    }).recover {
      case e => throw new AuthenticatorCreationException(CreateError.format(ID, loginInfo), e)
    }
  }

  /**
   * Retrieves the authenticator from request.
   *
   * @param request The request pipeline to retrieve the authenticator from.
   * @tparam R The type of the request.
   * @return Some authenticator or None if no authenticator could be found in request.
   */
  override def retrieve[R](implicit request: RequestPipeline[R]): Future[Option[SessionAuthenticator]] = {
    Future.fromTry(Try {
      if (settings.useFingerprinting) Some(fingerprintGenerator.generate) else None
    }).map { fingerprint =>
      request.session.get(settings.sessionKey).flatMap { value =>
        unserialize(value)(settings) match {
          case Success(authenticator) if fingerprint.isDefined && authenticator.fingerprint != fingerprint =>
            logger.info(InvalidFingerprint.format(ID, fingerprint, authenticator))
            None
          case Success(authenticator) => Some(authenticator)
          case Failure(error) =>
            logger.info(error.getMessage, error)
            None
        }
      }
    }.recover {
      case e => throw new AuthenticatorRetrievalException(RetrieveError.format(ID), e)
    }
  }

  /**
   * Returns a new user session containing the authenticator.
   *
   * @param authenticator The authenticator instance.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The serialized authenticator value.
   */
  override def init[R](authenticator: SessionAuthenticator)(implicit request: RequestPipeline[R]): Future[(String, String)] = {
    Future.successful(settings.sessionKey -> serialize(authenticator)(settings))
  }

  /**
   * Embeds the user session into the response.
   *
   * @param session The session to embed.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  override def embed[R, P](session: (String, String), response: ResponsePipeline[P])(
    implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]] = {

    Future.successful(response.withSession(session).touch)
  }

  /**
   * Overrides the user session in request.
   *
   * @param session The session to embed.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def embed[R](session: (String, String), request: RequestPipeline[R]): RequestPipeline[R] = {
    request.withSession(session)
  }

  /**
   * @inheritdoc
   *
   * @param authenticator The authenticator to touch.
   * @return The touched authenticator on the left or the untouched authenticator on the right.
   */
  override def touch(authenticator: SessionAuthenticator): Either[SessionAuthenticator, SessionAuthenticator] = {
    if (authenticator.idleTimeout.isDefined) {
      Left(authenticator.copy(lastUsedDateTime = clock.now))
    } else {
      Right(authenticator)
    }
  }

  /**
   * Updates the authenticator and store it in the user session.
   *
   * Because of the fact that we store the authenticator client side in the user session, we must update
   * the authenticator in the session on every subsequent request to keep the last used date in sync.
   *
   * @param authenticator The authenticator to update.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The original or a manipulated response pipeline.
   */
  override def update[R, P](authenticator: SessionAuthenticator, response: ResponsePipeline[P])(
    implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]] = {

    Future.fromTry(Try {
      response.withSession(settings.sessionKey -> serialize(authenticator)(settings)).touch
    }.recover {
      case e => throw new AuthenticatorUpdateException(UpdateError.format(ID, authenticator), e)
    })
  }

  /**
   * Renews an authenticator.
   *
   * The old authenticator needn't be revoked because we use a stateless approach here. So only
   * one authenticator can be bound to a user session. This method doesn't embed the the authenticator
   * into the response. This must be done manually if needed or use the other renew method otherwise.
   *
   * @param authenticator The authenticator to renew.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The serialized expression of the authenticator.
   */
  override def renew[R](authenticator: SessionAuthenticator)(implicit request: RequestPipeline[R]): Future[(String, String)] = {
    create(authenticator.loginInfo).flatMap(a => init(a)).recover {
      case e => throw new AuthenticatorRenewalException(RenewError.format(ID, authenticator), e)
    }
  }

  /**
   * Renews an authenticator and replaces the authenticator in session with a new one.
   *
   * The old authenticator needn't be revoked because we use a stateless approach here. So only
   * one authenticator can be bound to a user session.
   *
   * @param authenticator The authenticator to renew.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The original or a manipulated response pipeline.
   */
  override def renew[R, P](authenticator: SessionAuthenticator, response: ResponsePipeline[P])(
    implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]] = {

    renew(authenticator).flatMap(v => embed(v, response)).recover {
      case e => throw new AuthenticatorRenewalException(RenewError.format(ID, authenticator), e)
    }
  }

  /**
   * Removes the authenticator from session.
   *
   * @param authenticator The authenticator instance.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  override def discard[R, P](authenticator: SessionAuthenticator, response: ResponsePipeline[P])(
    implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]] = {

    Future.fromTry(Try {
      response.withoutSession(settings.sessionKey).touch
    }.recover {
      case e => throw new AuthenticatorDiscardingException(DiscardError.format(ID, authenticator), e)
    })
  }
}

/**
 * The companion object of the authenticator service.
 */
object SessionAuthenticatorService {

  /**
   * The ID of the authenticator.
   */
  val ID = "session-authenticator"

  /**
   * The error messages.
   */
  val JsonParseError = "[Silhouette][%s] Cannot parse Json: %s"
  val InvalidJsonFormat = "[Silhouette][%s] Invalid Json format: %s"
  val InvalidFingerprint = "[Silhouette][%s] Fingerprint %s doesn't match authenticator: %s"
}

/**
 * The settings for the session authenticator.
 *
 * @param sessionKey The key of the authenticator in the session.
 * @param encryptAuthenticator Indicates if the authenticator should be encrypted in session.
 * @param useFingerprinting Indicates if a fingerprint of the user should be stored in the authenticator.
 * @param authenticatorIdleTimeout The duration an authenticator can be idle before it timed out.
 * @param authenticatorExpiry The duration an authenticator expires after it was created.
 */
case class SessionAuthenticatorSettings(
  sessionKey: String = "authenticator",
  encryptAuthenticator: Boolean = true,
  useFingerprinting: Boolean = true,
  authenticatorIdleTimeout: Option[FiniteDuration] = None,
  authenticatorExpiry: FiniteDuration = 12 hours)
