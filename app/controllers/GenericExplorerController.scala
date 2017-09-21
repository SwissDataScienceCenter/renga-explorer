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

import javax.inject.{ Inject, Singleton }

import authorization.JWTVerifierProvider
import ch.datascience.graph.elements.persisted.PersistedVertex
import ch.datascience.graph.elements.persisted.json._
import ch.datascience.service.security.ProfileFilterAction
import ch.datascience.service.utils.persistence.graph.{ GraphExecutionContextProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.reader.{ EdgeReader, VertexReader }
import ch.datascience.service.utils.{ ControllerWithBodyParseJson, ControllerWithGraphTraversal }
import helpers.ObjectMatcher
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.Logger
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
 * Created by 3C111 on 29.08.2017
 */

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
    Logger.debug( "Request to retrieve a graphsubset of " + n + " nodes" )
    val g = graphTraversalSource
    val t = g.V().as( "node1" ).outE().as( "edge" ).inV().as( "node2" ).select[java.lang.Object]( "node1", "edge", "node2" ).limit( n )

    val future: Future[Seq[GraphSubSet]] = graphExecutionContext.execute {

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

  def retrieveNodeMetaData( id: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    Logger.debug( "Request to retrieve data of node with id " + id )
    val g = graphTraversalSource
    val t = g.V( Long.box( id ) )

    val future: Future[Option[PersistedVertex]] = graphExecutionContext.execute {
      if ( t.hasNext ) {
        val v = t.next()
        vertexReader.read( v ).map( Some.apply )
      }
      else
        Future.successful( None )
    }
    future.map( i => Ok( Json.toJson( i ) ) )
  }

  //Search for nodes with a property in a graph
  def retrieveNodeProperty( property: String ) = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    Logger.debug( "Request to retrieve node(s) with property " + property )
    val g = graphTraversalSource
    val t = g.V().has( property )

    val future: Future[List[PersistedVertex]] = {
      if ( t.hasNext ) {
        Future.sequence(
          for ( vertex <- t.asScala.toList ) yield vertexReader.read( vertex )
        )
      }

      else
        Future.successful( List() )
    }
    future.map( i => Ok( Json.toJson( i ) ) )
  }

  def getValuesForProperty( property: String ) = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>

    Logger.debug( "Request to retrieve values for property " + property )

    val g = graphTraversalSource
    val t = g.V().values[java.lang.Object]( property )

    val future: Future[List[String]] = graphExecutionContext.execute {
      Future( for ( v <- t.asScala.toList ) yield {

        ObjectMatcher.matcher( v )
      } )
    }
    future.map( s => Ok( Json.toJson( s ) ) )
  }

  //Search for nodes with a property and value in a graph
  def retrieveNodePropertyAndValue( property: String, value: Any ) = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    Logger.debug( "Request to retrieve node(s) with property " + property + " and value " + value )
    val g = graphTraversalSource
    val t = g.V().has( property, value )

    val future: Future[List[PersistedVertex]] = {
      if ( t.hasNext ) {
        Future.sequence(
          for ( vertex <- t.asScala.toList ) yield vertexReader.read( vertex )
        )
      }

      else
        Future.successful( List() )
    }

    future.map( i => Ok( Json.toJson( i ) ) )
  }

  private[this] implicit lazy val persistedVertexFormat = PersistedVertexFormat
  private[this] implicit lazy val persistedEdgeFormat = PersistedEdgeFormat
}
