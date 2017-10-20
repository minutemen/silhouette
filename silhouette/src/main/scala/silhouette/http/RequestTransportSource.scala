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

import silhouette.util.Source

/**
 * A source that retrieves a value from a request transport.
 *
 * @param requestTransport The request transport to retrieve the value from.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the request.
 */
case class RequestTransportSource[R](requestTransport: RequestTransport)(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Source[Option[String]] {

  /**
   * Gets the source.
   *
   * @return The source.
   */
  override def read: Option[String] = requestTransport.retrieve(requestPipeline)
}
