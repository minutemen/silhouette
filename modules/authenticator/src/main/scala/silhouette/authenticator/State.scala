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
package silhouette.authenticator

import silhouette.Identity

/**
 * Represents the authentication state.
 *
 * The authentication process can go through multiple steps. There can be one step that extracts the authenticator
 * from source. Another step could validate this extracted authenticator. Then we must find an identity for our
 * authenticator. And at last we could apply an authorization process to the identity.
 *
 * The goal is it to authenticate an identity and to return a success state, but any of these steps could also fail.
 * This is what any derived state class defines.
 *
 * @tparam I The type of the identity.
 */
sealed trait State[I <: Identity]

/**
 * Represents a state where a failure occurred in the authentication process.
 *
 * @param cause An exception that indicates the cause.
 * @tparam I The type of the identity.
 */
final case class Failure[I <: Identity](cause: Throwable) extends State[I]

/**
 * Represents a state where no authenticator was found.
 *
 * @tparam I The type of the identity.
 */
final case class MissingAuthenticator[I <: Identity]() extends State[I]

/**
 * Represents a state where in invalid authenticator was found.
 *
 * @param authenticator The found authenticator.
 * @tparam I The type of the identity.
 */
final case class InvalidAuthenticator[I <: Identity](authenticator: Authenticator) extends State[I]

/**
 * Represents a state where the authenticator but no identity for the authenticator was found.
 *
 * @param authenticator The found authenticator.
 * @tparam I The type of the identity.
 */
final case class MissingIdentity[I <: Identity](authenticator: Authenticator) extends State[I]

/**
 * Represents a state where an identity is authenticated but not authorized.
 *
 * @param authenticator The found authenticator.
 * @param identity      The found identity.
 * @tparam I The type of the identity.
 */
final case class NotAuthorized[I <: Identity](authenticator: Authenticator, identity: I) extends State[I]

/**
 * Represents a state where an identity is authenticated and authorized.
 *
 * @param authenticator The found authenticator.
 * @param identity      The found identity.
 * @tparam I The type of the identity.
 */
final case class Authenticated[I <: Identity](authenticator: Authenticator, identity: I) extends State[I]
