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
import ch.datascience.graph.elements.persisted.{ PersistedEdge, PersistedVertex }
import ch.datascience.graph.elements.persisted.json.{ PersistedEdgeFormat, PersistedVertexFormat }
import ch.datascience.service.security.ProfileFilterAction
import ch.datascience.service.utils.persistence.graph.{ GraphExecutionContextProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.reader.{ EdgeReader, VertexReader }
import ch.datascience.service.utils.{ ControllerWithBodyParseJson, ControllerWithGraphTraversal }
import helpers.ListConversions.ensureList
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop.gremlin.structure.{ Edge, Vertex }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ Format, Json }
import play.api.libs.ws.WSClient
import play.api.Logger
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
 * Created by 3C111 on 01.09.2017
 */

@Singleton
class LineageExplorerController @Inject() (
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

  lazy val logger: Logger = Logger( "application.LineageExplorerController" )

  def lineageFromContext( id: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    logger.debug( "Find Lineage from deployer with node with id " + id )

    val g = graphTraversalSource
    val t = g.V( Long.box( id ) ).repeat( __.bothE( "deployer:launch", "resource:create", "resource:write", "resource:read" ).dedup().as( "edge" ).otherV().as( "node" ) ).emit().simplePath().select[java.lang.Object]( "edge", "node" )

    val seq = graphExecutionContext.execute {

      for {
        entry <- t.asScala.toList
        s = entry.asScala.toMap
        edges = ensureList[Edge]( s( "edge" ) )
        vertices = ensureList[Vertex]( s( "node" ) )
        ( edge, vertex ) <- edges zip vertices
      } yield ( edge, vertex )

    }

    Future.traverse( seq.toSet ) {
      case ( edge, vertex ) =>
        for {
          e <- edgeReader.read( edge )
          v <- vertexReader.read( vertex )

        } yield ( e, v )
    }.map( _.map { tuple: ( PersistedEdge, PersistedVertex ) => Map( "edge" -> Json.toJson( tuple._1 ), "vertex" -> Json.toJson( tuple._2 ) ) } ).map( s => Ok( Json.toJson( s ) ) )

  }

  def lineageFromFile( id: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    logger.debug( "Find Lineage from filenode with id " + id )

    val g = graphTraversalSource
    val t = g.V( Long.box( id ) ).inE( "resource:version_of" ).otherV().as( "node" ).repeat( __.bothE( "resource:create", "resource:write", "resource:read", "deployer:launch" ).dedup().as( "edge" ).otherV().as( "node" ) ).emit().simplePath().select[java.lang.Object]( "edge", "node" )
    val seq = graphExecutionContext.execute {

      for {
        entry <- t.asScala.toList
        s = entry.asScala.toMap
        edges = ensureList[Edge]( s( "edge" ) )
        vertices = ensureList[Vertex]( s( "node" ) )
        ( edge, vertex ) <- edges zip vertices
      } yield ( edge, vertex )

    }

    Future.traverse( seq.toSet ) {
      case ( edge, vertex ) =>
        for {
          e <- edgeReader.read( edge )
          v <- vertexReader.read( vertex )

        } yield ( e, v )
    }.map( _.map { tuple: ( PersistedEdge, PersistedVertex ) => Map( "edge" -> Json.toJson( tuple._1 ), "vertex" -> Json.toJson( tuple._2 ) ) } ).map( s => Ok( Json.toJson( s ) ) )
  }

  def retrieveProjectLineage( id: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    logger.debug( "Request to retrieve project lineage for project node with id " + id )

    val g = graphTraversalSource
    val t = g.V( Long.box( id ) ).repeat( __.bothE( "deployer:launch", "project:is_part_of", "project:used_by" ).dedup().as( "edge" ).otherV().as( "node" ) ).emit().simplePath().select[java.lang.Object]( "edge", "node" )

    val seq = graphExecutionContext.execute {

      for {
        entry <- t.asScala.toList
        s = entry.asScala.toMap
        edges = ensureList[Edge]( s( "edge" ) )
        vertices = ensureList[Vertex]( s( "node" ) )
        ( edge, vertex ) <- edges zip vertices
      } yield ( edge, vertex )

    }

    Future.traverse( seq.toSet ) {
      case ( edge, vertex ) =>
        for {
          e <- edgeReader.read( edge )
          v <- vertexReader.read( vertex )

        } yield ( e, v )
    }.map( _.map { tuple: ( PersistedEdge, PersistedVertex ) => Map( "edge" -> Json.toJson( tuple._1 ), "vertex" -> Json.toJson( tuple._2 ) ) } ).map( s => Ok( Json.toJson( s ) ) )
  }

  private[this] implicit lazy val persistedVertexFormat: Format[PersistedVertex] = PersistedVertexFormat
  private[this] implicit lazy val persistedEdgeFormat: Format[PersistedEdge] = PersistedEdgeFormat
}
