package com.mohiva.silhouette.transport

import com.mohiva.silhouette.http.RequestPipeline
import com.mohiva.silhouette.{RequestTransport, AuthenticatorTransportSettings}
import scala.concurrent.Future

case class QueryStringTransportSettings(fieldName: String) extends AuthenticatorTransportSettings

class QueryStringRequestTransport(val settings: QueryStringTransportSettings) extends RequestTransport {
  override def retrieve[R](request: RequestPipeline[R]): Future[Option[String]] =
   Future.successful(request.queryParam(settings.fieldName))

  override def embed[R](value: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withQueryParam(settings.fieldName -> value)
}
