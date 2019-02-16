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
package silhouette

/**
 * Represents the authentication state.
 *
 * The authentication process can go through multiple steps. There can be one step that extracts the credentials
 * from a source. Another step could validate this extracted credentials. Then we must find an identity for our
 * credentials.
 *
 * The goal is it to authenticate an identity and to return a success state, but any of these steps could also fail.
 * This is what any derived state class defines.
 *
 * @tparam I The type of the identity.
 * @tparam C The type of the credentials.
 */
sealed trait AuthState[I <: Identity, C <: Credentials]

/**
 * Represents a state where no credentials were found.
 *
 * @tparam I The type of the identity.
 * @tparam C The type of the credentials.
 */
final case class MissingCredentials[I <: Identity, C <: Credentials]() extends AuthState[I, C]

/**
 * Represents a state where invalid credentials were found.
 *
 * @param credentials The found credentials.
 * @tparam I The type of the identity.
 * @tparam C The type of the credentials.
 */
final case class InvalidCredentials[I <: Identity, C <: Credentials](credentials: C) extends AuthState[I, C]

/**
 * Represents a state where the credentials but no identity were found.
 *
 * @param credentials The found credentials.
 * @param loginInfo   The login info for which an attempt was made to retrieve the identity.
 * @tparam I The type of the identity.
 * @tparam C The type of the credentials.
 */
final case class MissingIdentity[I <: Identity, C <: Credentials](credentials: C, loginInfo: LoginInfo)
  extends AuthState[I, C]

/**
 * Represents a state where an identity is authenticated and authorized.
 *
 * @param identity    The found identity.
 * @param credentials The found credentials.
 * @param loginInfo   The login info for which the identity was found.
 * @tparam I The type of the identity.
 * @tparam C The type of the credentials.
 */
final case class Authenticated[I <: Identity, C <: Credentials](
  identity: I,
  credentials: C,
  loginInfo: LoginInfo
) extends AuthState[I, C]
