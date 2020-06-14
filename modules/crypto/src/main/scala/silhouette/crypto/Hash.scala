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
package silhouette.crypto

import java.security.MessageDigest

import scala.io.Codec
import scala.language.implicitConversions

/**
 * Hash helper.
 */
object Hash {

  /**
   * The default codec used to convert the string into a byte array.
   */
  implicit val codec: Codec = Codec.UTF8

  /**
   * An implicit converter which converts a string into a byte array.
   *
   * @param str   The string to convert.
   * @param codec The codec used to convert the string into a byte array.
   * @return The byte array representation of the string.
   */
  implicit def strToByteArray(str: String)(
    implicit
    codec: Codec
  ): Array[Byte] = str.getBytes(codec.charSet)

  /**
   * Creates a MD5 hash from the given byte array.
   *
   * @param bytes The bytes to create a hash from.
   * @return The MD5 hash of the bytes.
   */
  def md5(bytes: Array[Byte]): String = hash("MD5", bytes)

  /**
   * Creates a SHA-1 hash from the given byte array.
   *
   * @param bytes The bytes to create a hash from.
   * @return The SHA-1 hash of the bytes.
   */
  def sha1(bytes: Array[Byte]): String = hash("SHA-1", bytes)

  /**
   * Creates a SHA-256 hash from the given byte array.
   *
   * @param bytes The bytes to create a hash from.
   * @return The SHA-256 hash of the bytes.
   */
  def sha256(bytes: Array[Byte]): String = hash("SHA-256", bytes)

  /**
   * Creates a SHA-384 hash from the given byte array.
   *
   * @param bytes The bytes to create a hash from.
   * @return The SHA-384 hash of the bytes.
   */
  def sha384(bytes: Array[Byte]): String = hash("SHA-384", bytes)

  /**
   * Creates a SHA-512 hash from the given byte array.
   *
   * @param bytes The bytes to create a hash from.
   * @return The SHA-512 hash of the bytes.
   */
  def sha512(bytes: Array[Byte]): String = hash("SHA-512", bytes)

  /**
   * Gets the hash for the given algorithm and the given bytes.
   *
   * @param algorithm The hash algorithm to use.
   * @param bytes     The bytes to create a hash from.
   * @return The hash for the given algorithm and the given bytes.
   */
  private def hash(algorithm: String, bytes: Array[Byte]): String =
    MessageDigest.getInstance(algorithm).digest(bytes).map("%02x".format(_)).mkString
}
