package silhouette.akka.http.conversions

import akka.http.scaladsl.model.headers.HttpCookie
import silhouette.http.Cookie

object HttpCookieConversion {

  val cookieToHttpCookie: Cookie => HttpCookie = cookie => HttpCookie(
    name = cookie.name,
    value = cookie.value,
    expires = None,
    maxAge = cookie.maxAge.map(_.toLong),
    domain = cookie.domain,
    path = cookie.path,
    secure = cookie.secure,
    httpOnly = cookie.httpOnly,
    extension = None
  )
  val httpCookieToCookie: HttpCookie => Cookie = httpCookie => Cookie(
    name = httpCookie.name,
    value = httpCookie.value,
    maxAge = httpCookie.maxAge.map(_.toInt),
    domain = httpCookie.domain,
    path = httpCookie.path,
    secure = httpCookie.secure,
    httpOnly = httpCookie.httpOnly
  )

}
