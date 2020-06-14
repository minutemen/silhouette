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

import scala.language.implicitConversions

/**
 * An extended [[scala.Seq]] implementation with additional functionality.
 */
class RichSeq[A](seq: Seq[A]) {

  /**
   * Group by function that preserves the order of the initial sequence.
   *
   * @see https://stackoverflow.com/questions/14434160/why-does-groupby-in-scala-change-the-ordering-of-a-lists-items
   * @see https://stackoverflow.com/questions/9594431/scala-groupby-preserving-insertion-order
   *
   * @param f A function that evaluates which value should be used for the grouping.
   * @tparam K The type of the value that should be used for grouping.
   * @return A sequence containing the grouped items.
   */
  def groupByPreserveOrder[K](f: A => K): Seq[(K, List[A])] =
    seq.foldRight(List[(K, List[A])]())((item: A, res: List[(K, List[A])]) =>
      res match {
        case Nil                               => List((f(item), List(item)))
        case (k, kLst) :: tail if k == f(item) => (k, item :: kLst) :: tail
        case _                                 => (f(item), List(item)) :: res
      }
    )
}

/**
 * The companion object.
 */
object RichSeq {
  implicit def seqToRichSeq[A](s: Seq[A]): RichSeq[A] = new RichSeq(s)
}
