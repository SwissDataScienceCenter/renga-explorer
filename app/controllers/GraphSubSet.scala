/*
 * Copyright 2017 - Swiss Data Science Center (SDSC)
 * A partnership between École Polytechnique Fédérale de Lausanne (EPFL) and
 * Eidgenössische Technische Hochschule Zürich (ETHZ).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import ch.datascience.graph.elements.persisted.{ PersistedEdge, PersistedVertex }
import ch.datascience.graph.elements.persisted.json.{ PersistedEdgeFormat, PersistedVertexFormat }
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class GraphSubSet( node1: PersistedVertex, edge: PersistedEdge, node2: PersistedVertex )

object GraphSubSet {

  implicit lazy val persistedVertexFormat = PersistedVertexFormat
  implicit lazy val persistedEdgeFormat = PersistedEdgeFormat
  val graphSubSetReads: Reads[GraphSubSet] = (
    ( JsPath \ "node1" ).read[PersistedVertex] and
    ( JsPath \ "edge" ).read[PersistedEdge] and
    ( JsPath \ "node2" ).read[PersistedVertex]
  )( GraphSubSet.apply _ )

  val graphSubSetWrites: Writes[GraphSubSet] = (
    ( JsPath \ "node1" ).write[PersistedVertex] and
    ( JsPath \ "edge" ).write[PersistedEdge] and
    ( JsPath \ "node2" ).write[PersistedVertex]
  )( unlift( GraphSubSet.unapply ) )

  implicit val locationFormat: Format[GraphSubSet] =
    Format( graphSubSetReads, graphSubSetWrites )
}
