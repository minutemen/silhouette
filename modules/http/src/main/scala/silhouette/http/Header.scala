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
package silhouette.http

import scala.language.implicitConversions

/**
 * Represents a HTTP header.
 *
 * The HTTP RFC2616 allows duplicate request headers with the same name. Therefore we must define a
 * header values as sequence of values.
 *
 * @see https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
 *
 * @param name   The header name as string.
 * @param values The header values.
 */
protected[silhouette] case class Header(name: Header.Name, values: Seq[String]) {

  /**
   * Creates a header with a single value.
   *
   * @param name  The header name as string.
   * @param value The header value.
   * @return
   */
  def this(name: Header.Name, value: String) = this(name, Seq(value))

  /**
   * Gets the value of a header.
   *
   * If multiple values are defined for an header, then the values must be concatenated and each separated by a comma.
   *
   * @see https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
   *
   * @return The value of the header.
   */
  def value: String = values.mkString(",")
}

/**
 * The companion object.
 */
private[silhouette] object Header {

  def apply(name: Header.Name, value: String): Header = Header(name, Seq(value))

  /**
   * Represents a HTTP header name.
   *
   * @param value The header name as string.
   */
  protected[silhouette] case class Name(value: String)

  /**
   * Common HTTP header names.
   *
   * @see https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
   */
  private[silhouette] object Name {
    implicit def toString(name: Name): String = name.value
    implicit def fromString(name: String): Name = Name(name)

    // Standard request fields
    final val `A-IM` = Name("A-IM")
    final val `Accept` = Name("Accept")
    final val `Accept-Charset` = Name("Accept-Charset")
    final val `Accept-Encoding` = Name("Accept-Encoding")
    final val `Accept-Language` = Name("Accept-Language")
    final val `Accept-Datetime` = Name("Accept-Datetime")
    final val `Access-Control-Request-Method` = Name("Access-Control-Request-Method")
    final val `Access-Control-Request-Headers` = Name("Access-Control-Request-Headers")
    final val `Authorization` = Name("Authorization")
    final val `Cookie` = Name("Cookie")
    final val `Expect` = Name("Expect")
    final val `Forwarded` = Name("Forwarded")
    final val `From` = Name("From")
    final val `Host` = Name("Host")
    final val `If-Match` = Name("If-Match")
    final val `If-Modified-Since` = Name("If-Modified-Since")
    final val `If-None-Match` = Name("If-None-Match")
    final val `If-Range` = Name("If-Range")
    final val `If-Unmodified-Since` = Name("If-Unmodified-Since")
    final val `Max-Forwards` = Name("Max-Forwards")
    final val `Origin` = Name("Origin")
    final val `Proxy-Authorization` = Name("Proxy-Authorization")
    final val `Range` = Name("Range")
    final val `Referer` = Name("Referer")
    final val `TE` = Name("TE")
    final val `User-Agent` = Name("User-Agent")

    // Standard response fields
    final val `Access-Control-Allow-Origin` = Name("Access-Control-Allow-Origin")
    final val `Access-Control-Allow-Credentials` = Name("Access-Control-Allow-Credentials")
    final val `Access-Control-Expose-Headers` = Name("Access-Control-Expose-Headers")
    final val `Access-Control-Max-Age` = Name("Access-Control-Max-Age")
    final val `Access-Control-Allow-Methods` = Name("Access-Control-Allow-Methods")
    final val `Access-Control-Allow-Headers` = Name("Access-Control-Allow-Headers")
    final val `Accept-Patch` = Name("Accept-Patch")
    final val `Accept-Ranges` = Name("Accept-Ranges")
    final val `Age` = Name("Age")
    final val `Allow` = Name("Allow")
    final val `Alt-Svc` = Name("Alt-Svc")
    final val `Content-Disposition` = Name("Content-Disposition")
    final val `Content-Encoding` = Name("Content-Encoding")
    final val `Content-Language` = Name("Content-Language")
    final val `Content-Location` = Name("Content-Location")
    final val `Content-Range` = Name("Content-Range")
    final val `Delta-Base` = Name("Delta-Base")
    final val `ETag` = Name("ETag")
    final val `Expires` = Name("Expires")
    final val `IM` = Name("IM")
    final val `Last-Modified` = Name("Last-Modified")
    final val `Link` = Name("Link")
    final val `Location` = Name("Location")
    final val `P3P` = Name("P3P")
    final val `Proxy-Authenticate` = Name("Proxy-Authenticate")
    final val `Public-Key-Pins` = Name("Public-Key-Pins")
    final val `Retry-After` = Name("Retry-After")
    final val `Server` = Name("Server")
    final val `Set-Cookie` = Name("Set-Cookie")
    final val `Strict-Transport-Security` = Name("Strict-Transport-Security")
    final val `Trailer` = Name("Trailer")
    final val `Transfer-Encoding` = Name("Transfer-Encoding")
    final val `Tk` = Name("Tk")
    final val `Vary` = Name("Vary")
    final val `WWW-Authenticate` = Name("WWW-Authenticate")
    final val `X-Frame-Options` = Name("X-Frame-Options")

    // Shared standard request and response fields
    final val `Cache-Control` = Name("Cache-Control")
    final val `Connection` = Name("Connection")
    final val `Content-Length` = Name("Content-Length")
    final val `Content-MD5` = Name("Content-MD5")
    final val `Content-Type` = Name("Content-Type")
    final val `Date` = Name("Date")
    final val `Pragma` = Name("Pragma")
    final val `Upgrade` = Name("Upgrade")
    final val `Via` = Name("Via")
    final val `Warning` = Name("Warning")

    // Common non-standard request fields
    final val `Upgrade-Insecure-Requests` = Name("Upgrade-Insecure-Requests")
    final val `X-Requested-With` = Name("X-Requested-With")
    final val `DNT` = Name("DNT")
    final val `X-Forwarded-For` = Name("X-Forwarded-For")
    final val `X-Forwarded-Hos` = Name("X-Forwarded-Hos")
    final val `X-Forwarded-Proto` = Name("X-Forwarded-Proto")
    final val `Front-End-Https` = Name("Front-End-Https")
    final val `X-Http-Method-Override` = Name("X-Http-Method-Override")
    final val `X-ATT-DeviceId` = Name("X-ATT-DeviceId")
    final val `X-Wap-Profile` = Name("X-Wap-Profile")
    final val `Proxy-Connection` = Name("Proxy-Connection")
    final val `X-UIDH` = Name("X-UIDH")
    final val `X-Csrf-Token` = Name("X-Csrf-Token")
    final val `Save-Data` = Name("Save-Data")

    // Common non-standard response fields
    final val `Content-Security-Policy` = Name("Content-Security-Policy")
    final val `X-Content-Security-Policy` = Name("X-Content-Security-Policy")
    final val `X-WebKit-CSP` = Name("X-WebKit-CSP")
    final val `Refresh` = Name("Refresh")
    final val `Status` = Name("Status")
    final val `Timing-Allow-Origin` = Name("Timing-Allow-Origin")
    final val `X-Content-Duration` = Name("X-Content-Duration")
    final val `X-Content-Type-Options` = Name("X-Content-Type-Options")
    final val `X-Powered-By` = Name("X-Powered-By")
    final val `X-UA-Compatible` = Name("X-UA-Compatible")
    final val `X-XSS-Protection` = Name("X-XSS-Protection")

    // Shared non-standard request and response fields
    final val `X-Request-ID` = Name("X-Request-ID")
    final val `X-Correlation-ID` = Name("X-Correlation-ID")
  }
}
