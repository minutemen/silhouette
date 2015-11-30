package com.mohiva.silhouette.persistence

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
 * A trait which provides a cache API.
 */
trait CacheLayer {

  /**
   * Finds a value in the cache.
   *
   * @param key The key of the item to found.
   * @tparam T The type of the object to return.
   * @return The found value or None if no value could be found.
   */
  def find[T: ClassTag](key: String): Future[Option[T]]

  /**
   * Save a value in cache.
   *
   * @param key The item key under which the value should be saved.
   * @param value The value to save.
   * @param expiration Expiration time in seconds (0 second means eternity).
   * @return The value saved in cache.
   */
  def save[T](key: String, value: T, expiration: Duration = Duration.Inf): Future[T]

  /**
   * Remove a value from the cache.
   *
   * @param key Item key.
   * @return An empty future to wait for removal.
   */
  def remove(key: String): Future[Unit]
}
