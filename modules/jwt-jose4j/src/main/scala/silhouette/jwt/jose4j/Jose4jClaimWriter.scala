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

import io.circe.{ Json, JsonNumber, JsonObject }
import org.jose4j.jwt.{ JwtClaims, NumericDate }
import silhouette.jwt.jose4j.Jose4jClaimWriter._
import silhouette.jwt.{ JwtClaimWriter, JwtException }

import scala.jdk.CollectionConverters._

/**
 * JWT transformer based on the [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library.
 *
 * The library supports the JWS/JWE compact serializations with the complete suite of JOSE algorithms.
 *
 * @param producer The JWT producer.
 */
final case class Jose4jClaimWriter(producer: Jose4jProducer) extends JwtClaimWriter {

  /**
   * Transforms a JWT claims object into a JWT as string.
   *
   * @param jwt The JWT claims object to transform.
   * @return The JWT string representation or an error if the JWT claims object couldn't be transformed.
   */
  override def apply(jwt: silhouette.jwt.Claims): Either[Throwable, String] = {
    for {
      claims <- toJose4j(jwt)
      jwt <- producer.produce(claims)
    } yield {
      jwt
    }
  }

  /**
   * Converts the Silhouette claims instance to a jose4j claims instance.
   *
   * @param claims The Silhouette claims instance.
   * @return The jose4j claims instance on the right or an error on the left.
   */
  private def toJose4j(claims: silhouette.jwt.Claims): Either[Throwable, JwtClaims] = {
    val result = new JwtClaims()
    claims.issuer.foreach(result.setIssuer)
    claims.subject.foreach(result.setSubject)
    claims.audience.foreach(v => result.setAudience(v.asJava))
    claims.expirationTime.foreach(v => result.setExpirationTime(NumericDate.fromSeconds(v.getEpochSecond)))
    claims.notBefore.foreach(v => result.setNotBefore(NumericDate.fromSeconds(v.getEpochSecond)))
    claims.issuedAt.foreach(v => result.setIssuedAt(NumericDate.fromSeconds(v.getEpochSecond)))
    claims.jwtID.foreach(result.setJwtId)

    val (reservedClaims, customClaims) = transformCustomClaims(claims.custom).asScala.partition {
      case (k, _) =>
        silhouette.jwt.ReservedClaims.contains(k)
    }

    reservedClaims.headOption match {
      case Some((key, _)) =>
        Left(new JwtException(OverrideReservedClaim.format(key, silhouette.jwt.ReservedClaims.mkString(", "))))
      case None =>
        customClaims.foreach { case (k, v) => result.setClaim(k, v) }
        Right(result)
    }
  }

  /**
   * Transforms recursively the custom claims.
   *
   * @param claims The custom claims to transform.
   * @return A map containing custom claims.
   */
  private def transformCustomClaims(claims: JsonObject): java.util.Map[String, Object] = {
    def toJava(value: Json): Object = value.fold[Object](
      jsonNull = None.orNull,
      jsonBoolean = (x: Boolean) => Boolean.box(x),
      jsonNumber = (x: JsonNumber) => x.toBigDecimal.getOrElse(Integer.valueOf(0)),
      jsonString = (x: String) => x,
      jsonArray = (x: Vector[Json]) => x.map(toJava).asJava,
      jsonObject = (x: JsonObject) => transformCustomClaims(x)
    )

    claims.toMap.map { case (name, value) => name -> toJava(value) }.asJava
  }
}

/**
 * The companion object.
 */
object Jose4jClaimWriter {

  /**
   * The error messages.
   */
  val OverrideReservedClaim: String = "Try to overriding a reserved claim `%s`; list of reserved claims: %s"
}
