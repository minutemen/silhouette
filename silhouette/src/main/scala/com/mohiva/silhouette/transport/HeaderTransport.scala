package com.mohiva.silhouette.transport

import com.mohiva.silhouette.{ResponseTransport, RequestTransport, AuthenticatorTransportSettings}
import com.mohiva.silhouette.http.{RequestPipeline, ResponsePipeline}
import scala.concurrent.Future

case class HeaderTransportSettings(headerName: String) extends AuthenticatorTransportSettings

class HeaderTransport(val settings: HeaderTransportSettings) extends RequestTransport with ResponseTransport {
  override def discard[P](value: String, response: ResponsePipeline[P]): Future[ResponsePipeline[P]] =
    Future.successful(response)

  override def retrieve[R](request: RequestPipeline[R]): Future[Option[String]] =
    Future.successful(request.header(settings.headerName).headOption)

  override def embed[R](value: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withHeaders(settings.headerName -> value)

  override def embed[P](value: String, response: ResponsePipeline[P]): Future[ResponsePipeline[P]] =
    Future.successful(response.withHeaders(settings.headerName -> value))
}
