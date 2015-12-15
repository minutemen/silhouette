package com.mohiva.silhouette.transport

import com.mohiva.silhouette.http.{RequestPipeline, ResponsePipeline}
import com.mohiva.silhouette.{ResponseTransport, RequestTransport, AuthenticatorTransportSettings}
import scala.concurrent.Future

case class SessionTransportSettings(sessionKey: String = "authenticator") extends AuthenticatorTransportSettings

class SessionTransport(val settings: SessionTransportSettings) extends RequestTransport with ResponseTransport {
  override def discard[P](value: String, response: ResponsePipeline[P]): Future[ResponsePipeline[P]] =
    Future.successful(response.withoutSession(settings.sessionKey).touch)

  override def retrieve[R](request: RequestPipeline[R]): Future[Option[String]] =
    Future.successful(request.session.get(settings.sessionKey))

  override def embed[R](value: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withSession(settings.sessionKey -> value)

  override def embed[P](value: String, response: ResponsePipeline[P]): Future[ResponsePipeline[P]] =
    Future.successful(response.withSession(settings.sessionKey -> value).touch)
}
