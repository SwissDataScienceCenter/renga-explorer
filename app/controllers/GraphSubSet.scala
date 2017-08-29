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
