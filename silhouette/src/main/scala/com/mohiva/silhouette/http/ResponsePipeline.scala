package com.mohiva.silhouette.http

/**
 * Decorates a framework specific response implementation.
 *
 * Frameworks should create an implicit conversion between the implementation of this pipeline and
 * the Framework specific response instance.
 *
 * @tparam P The type of the response.
 */
protected[silhouette] trait ResponsePipeline[P] {

  /**
   * The framework specific response implementation.
   */
  val response: P

  /**
   * A marker flag which indicates that an operation on an authenticator was processed and
   * therefore it shouldn't be updated automatically.
   *
   * Due the fact that the update method gets called on every subsequent request to update the
   * authenticator related data in the backing store and in the result, it isn't possible to
   * discard or renew the authenticator simultaneously. This is because the "update" method would
   * override the result created by the "renew" or "discard" method, because it will be executed
   * as last in the chain.
   *
   * As example:
   * If we discard the session in a Silhouette endpoint then it will be removed from session. But
   * at the end the update method will embed the session again, because it gets called with the
   * result of the endpoint.
   */
  protected[silhouette] val touched = false

  /**
   * Adds headers to the response.
   *
   * @param headers The headers to add.
   * @return A new response pipeline instance with the added headers.
   */
  def withHeaders(headers: (String, String)*): ResponsePipeline[P]

  /**
   * Adds cookies to the response.
   *
   * @param cookies The cookies to add.
   * @return A new response pipeline instance with the added cookies.
   */
  def withCookies(cookies: Cookie*): ResponsePipeline[P]

  /**
   * Adds session data to the response.
   *
   * @param data The session data to add.
   * @return A new response pipeline instance with the added session data.
   */
  def withSession(data: (String, String)*): ResponsePipeline[P]

  /**
   * Removes session data from response.
   *
   * @param keys The session keys to remove.
   * @return A new response pipeline instance with the removed session data.
   */
  def withoutSession(keys: String*): ResponsePipeline[P]

  /**
   * Unboxes the framework specific request implementation.
   *
   * @return The framework specific request implementation.
   */
  def unbox: P

  /**
   * Touches a response.
   *
   * @return A touched response pipeline.
   */
  protected[silhouette] def touch: ResponsePipeline[P]
}
