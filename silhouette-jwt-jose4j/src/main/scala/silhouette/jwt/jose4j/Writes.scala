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
package silhouette.jwt.jose4j

import org.jose4j.jwt.{ JwtClaims, NumericDate }
import silhouette.exceptions.JwtException
import silhouette.jwt.jose4j.Writes._

import scala.collection.JavaConverters._
import scala.json.ast._
import scala.language.implicitConversions
import scala.util.Try

/**
 * JWT transformer based on the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The library supports the JWS/JWE compact serializations with the complete suite of JOSE algorithms.
 *
 * @param producer The JWT producer.
 */
final case class Writes(producer: Producer) extends silhouette.jwt.Writes {

  /**
   * Transforms a JWT claims object into a JWT as string.
   *
   * @param jwt The JWT claims object to transform.
   * @return The JWT string representation or an error if the JWT claims object couldn't be transformed.
   */
  override def write(jwt: silhouette.jwt.Claims): Try[String] = Try(producer.produce(jwt))

  /**
   * Converts the Silhouette claims instance to a jose4j claims instance.
   *
   * @param claims The Silhouette claims instance.
   * @return The jose4j claims instance.
   */
  implicit private def toJose4j(claims: silhouette.jwt.Claims): JwtClaims = {
    val result = new JwtClaims()
    claims.issuer.foreach(result.setIssuer)
    claims.subject.foreach(result.setSubject)
    claims.audience.foreach(v => result.setAudience(v.asJava))
    claims.expirationTime.foreach(v => result.setExpirationTime(NumericDate.fromSeconds(v.getEpochSecond)))
    claims.notBefore.foreach(v => result.setNotBefore(NumericDate.fromSeconds(v.getEpochSecond)))
    claims.issuedAt.foreach(v => result.setIssuedAt(NumericDate.fromSeconds(v.getEpochSecond)))
    claims.jwtID.foreach(result.setJwtId)
    transformCustomClaims(claims.custom).asScala.foreach {
      case (k, v) =>
        if (silhouette.jwt.ReservedClaims.contains(k)) {
          throw new JwtException(OverrideReservedClaim.format(k, silhouette.jwt.ReservedClaims.mkString(", ")))
        }
        result.setClaim(k, v)
    }
    result
  }

  /**
   * Transforms recursively the custom claims.
   *
   * @param claims The custom claims to transform.
   * @return A map containing custom claims.
   */
  private def transformCustomClaims(claims: JObject): java.util.Map[String, Object] = {
    def toJava(value: JValue): Object = value match {
      case JNull       => None.orNull
      case v: JString  => v.value
      case v: JNumber  => BigDecimal(v.value)
      case v: JBoolean => Boolean.box(v.get)
      case v: JArray   => v.value.map(toJava).asJava
      case v: JObject  => transformCustomClaims(v)
    }

    claims.value.map { case (name, value) => name -> toJava(value) }.asJava
  }
}

/**
 * The companion object.
 */
object Writes {

  /**
   * The error messages.
   */
  val OverrideReservedClaim: String = "Try to overriding a reserved claim `%s`; list of reserved claims: %s"
}
