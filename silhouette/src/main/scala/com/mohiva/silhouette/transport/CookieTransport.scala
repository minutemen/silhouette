package com.mohiva.silhouette.transport

import com.mohiva.silhouette.{ResponseTransport, RequestTransport, AuthenticatorTransportSettings}
import com.mohiva.silhouette.http.{Cookie, RequestPipeline, ResponsePipeline}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

case class CookieTransportSettings(
  cookieName: String = "id",
  cookiePath: String = "/",
  cookieDomain: Option[String] = None,
  secureCookie: Boolean = true,
  httpOnlyCookie: Boolean = true,
  cookieMaxAge: Option[FiniteDuration] = None
) extends AuthenticatorTransportSettings

class CookieTransport(val settings: CookieTransportSettings) extends RequestTransport with ResponseTransport {

  def cookie(settings: CookieTransportSettings, value: String) = Cookie(
    name = settings.cookieName,
    value = value,
    // TODO: Think how deal with Remember Me feature, here we need to take authenticator maxAge
    maxAge = settings.cookieMaxAge.map(_.toSeconds.toInt),
    domain = settings.cookieDomain,
    path = Some(settings.cookiePath),
    secure = settings.secureCookie,
    httpOnly = settings.httpOnlyCookie
  )
  val discardCookie = cookie(settings, value = "").copy(maxAge = Some(-86400))

  override def discard[P](value: String, response: ResponsePipeline[P]): Future[ResponsePipeline[P]] = Future.successful{
    response.withCookies(discardCookie).touch
  }

  override def retrieve[R](request: RequestPipeline[R]): Future[Option[String]] =
    Future.successful(request.cookie(settings.cookieName).map(_.value))

  override def embed[R](value: String, request: RequestPipeline[R]): RequestPipeline[R] = {
    request.withCookies(cookie(settings, value))
  }

  override def embed[P](value: String, response: ResponsePipeline[P]): Future[ResponsePipeline[P]] = Future.successful {
    response.withCookies(cookie(settings, value))
  }
}
