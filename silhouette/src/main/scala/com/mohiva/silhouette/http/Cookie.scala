package com.mohiva.silhouette.http

/**
 * An HTTP cookie.
 *
 * We do not use the `Expires` attribute because Play doesn't support it and `Max-Age` is the newer standard.
 *
 * @param name The cookie name.
 * @param value The cookie value.
 * @param maxAge The cookie expiration date in seconds, `None` for a transient cookie, or a value less than 0 to expire a cookie now.
 * @param domain The cookie domain.
 * @param path The the cookie path.
 * @param secure Whether this cookie is secured, sent only for HTTPS requests.
 * @param httpOnly Whether this cookie is HTTP only, i.e. not accessible from client-side JavaScript code.
 */
case class Cookie(
  name: String,
  value: String,
  maxAge: Option[Int] = None,
  domain: Option[String] = None,
  path: Option[String] = None,
  secure: Boolean = false,
  httpOnly: Boolean = false)
