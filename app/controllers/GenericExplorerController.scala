package controllers

import javax.inject.{ Inject, Singleton }

import authorization.JWTVerifierProvider
import ch.datascience.graph.Constants
import ch.datascience.graph.elements.persisted.{ PersistedVertex }
import ch.datascience.graph.elements.persisted.json.{ PersistedEdgeFormat, PersistedVertexFormat }
import ch.datascience.graph.naming.NamespaceAndName
import ch.datascience.service.security.ProfileFilterAction
import ch.datascience.service.utils.persistence.graph.{ GraphExecutionContextProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.reader.{ EdgeReader, VertexReader }
import ch.datascience.service.utils.{ ControllerWithBodyParseJson, ControllerWithGraphTraversal }
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class GenericExplorerController @Inject() (
    config:                                         play.api.Configuration,
    jwtVerifier:                                    JWTVerifierProvider,
    wsclient:                                       WSClient,
    implicit val graphExecutionContextProvider:     GraphExecutionContextProvider,
    implicit val janusGraphTraversalSourceProvider: JanusGraphTraversalSourceProvider,
    implicit val vertexReader:                      VertexReader,
    implicit val edgeReader:                        EdgeReader
) extends Controller
  with ControllerWithBodyParseJson
  with ControllerWithGraphTraversal {

  def retrieveGraphSubset: Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    // unless otherwise specified, the number of nodes are limited

    val n = 10
    val g = graphTraversalSource
    val t = g.V().as( "node1" ).outE().as( "edge" ).inV().as( "node2" ).select[java.lang.Object]( "node1", "edge", "node2" ).limit( n )

    val future: Future[Seq[GraphSubSet]] = graphExecutionContext.execute {
      import scala.collection.JavaConverters._
      Future.sequence(
        for {
          entry <- t.asScala.toSeq
          Seq( ( n1, fnode1 ), ( e, fedge ), ( n2, fnode2 ) ) = entry.asScala.toSeq
        } yield for {
          vertex1 <- vertexReader.read( fnode1.asInstanceOf[Vertex] )
          edge <- edgeReader.read( fedge.asInstanceOf[Edge] )
          vertex2 <- vertexReader.read( fnode2.asInstanceOf[Vertex] )
        } yield GraphSubSet( vertex1, edge, vertex2 )
      )
    }

    future.map( i => Ok( Json.toJson( i ) ) )
  }
  /*


    def retrieveGraphMetaData: Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
       val g = graphTraversalSource
       val t = g.V()

           val future: Future[Map[String, String]] = graphExecutionContext.execute {
         Future.sequence( ??? )
       }
       future.map( s => Ok( Json.toJson( s ) ) )

     }*/

  private[this] implicit lazy val persistedVertexFormat = PersistedVertexFormat
  private[this] implicit lazy val persistedEdgeFormat = PersistedEdgeFormat
}
