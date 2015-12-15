package com.mohiva.silhouette

import com.mohiva.silhouette.http.{ResponsePipeline, RequestPipeline}
import scala.concurrent.Future

trait AuthenticatorTransportSettings

trait AuthenticatorTransport {
  val settings: AuthenticatorTransportSettings
}

trait RequestTransport extends AuthenticatorTransport {
  def retrieve[R](request: RequestPipeline[R]): Future[Option[String]]
  def embed[R](value: String, request: RequestPipeline[R]): RequestPipeline[R]
}

trait ResponseTransport extends AuthenticatorTransport{
  def embed[P](value: String, response: ResponsePipeline[P]): Future[ResponsePipeline[P]]
  def discard[P](value: String, response: ResponsePipeline[P]): Future[ResponsePipeline[P]]
}