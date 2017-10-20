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
import ch.datascience.graph.Constants
import ch.datascience.graph.elements.persisted.PersistedVertex
import ch.datascience.graph.elements.persisted.json.{ PersistedEdgeFormat, PersistedVertexFormat }
import ch.datascience.service.security.ProfileFilterAction
import ch.datascience.service.utils.persistence.graph.{ GraphExecutionContextProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.reader.{ EdgeReader, VertexReader }
import ch.datascience.service.utils.{ ControllerWithBodyParseJson, ControllerWithGraphTraversal }
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.Logger
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * Created by 3C111 on 04.09.2017
 */

@Singleton
class ProjectExplorerController @Inject() (
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

  def retrieveProjectByUserName( userId: Option[String] ) = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    val user = userId.getOrElse( request.userId )

    val n = 100
    Logger.debug( "Request to retrieve at most " + n + " project nodes for user " + user )
    val g = graphTraversalSource
    val t = g.V().has( "resource:owner", user ).has( Constants.TypeKey, "project:project" ).limit( n )

    val future: Future[Seq[PersistedVertex]] = graphExecutionContext.execute {
      Future.sequence( t.toIterable.map( v =>
        vertexReader.read( v ) ).toSeq )
    }
    future.map( s => Ok( Json.toJson( s ) ) )

  }

  def retrieveProjects = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    val n = 100
    Logger.debug( "Request to retrieve at most " + n + " project nodes  " )
    val g = graphTraversalSource
    val t = g.V().has( Constants.TypeKey, "project:project" ).limit( n )

    val future: Future[Seq[PersistedVertex]] = graphExecutionContext.execute {
      Future.sequence( t.toIterable.map( v =>
        vertexReader.read( v ) ).toSeq )
    }
    future.map( s => Ok( Json.toJson( s ) ) )

  }

  def retrieveProjectMetadata( id: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    Logger.debug( "Request to retrieve project metadata for project node with id " + id )
    val g = graphTraversalSource
    val t = g.V( Long.box( id ) ).has( Constants.TypeKey, "project:project" )

    val future: Future[PersistedVertex] = graphExecutionContext.execute {
      vertexReader.read( t.next() )

    }
    future.map( s => Ok( Json.toJson( s ) ) )

  }

  // Retrieve resources linked to project, if no resource specified all nodes are given with the is_part_of edge towards the project
  def retrieveProjectResources( id: Long, resource: Option[String] ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    Logger.debug( "Request to retrieve resource " + resource.getOrElse( "all" ) + "for project with id " + id )

    val availableResources = Set( "file", "bucket", "context", "execution" )

    val g = graphTraversalSource

    val t: GraphTraversal[Vertex, Vertex] = resource match {
      case None => {
        Logger.debug( "Requested all resources" )
        g.V( Long.box( id ) ).inE( "project:is_part_of" ).otherV()
      }
      case Some( x ) =>
        if ( availableResources.contains( x ) ) {
          Logger.debug( "Requested resource " + x )
          g.V( Long.box( id ) ).inE( "project:is_part_of" ).otherV().has( Constants.TypeKey, stringToKey( x ) )

        }
        else throw new UnsupportedOperationException( "Type not supported" )
    }

    val future: Future[List[PersistedVertex]] = graphExecutionContext.execute {
      Future.sequence( t.toIterable.map( v =>
        vertexReader.read( v ) ).toList )
    }

    future.map {
      case x :: xs => Ok( Json.toJson( x :: xs ) )
      case _       => NotFound
    }
  }

  def stringToKey( resource: String ): String = {
    resource.toLowerCase match {
      case "file"      => "resource:file"
      case "bucket"    => "resource:bucket"
      case "context"   => "deployer:context"
      case "execution" => "deployer:execution"
      case _           => throw new UnsupportedOperationException( "Type not supported" )
    }
  }

  private[this] implicit lazy val persistedVertexFormat = PersistedVertexFormat
  private[this] implicit lazy val persistedEdgeFormat = PersistedEdgeFormat
}
