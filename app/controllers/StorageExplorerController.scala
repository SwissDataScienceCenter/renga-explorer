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
import ch.datascience.graph.naming.NamespaceAndName
import ch.datascience.service.security.ProfileFilterAction
import ch.datascience.service.utils.persistence.graph.{ GraphExecutionContextProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.reader.{ EdgeReader, VertexReader }
import ch.datascience.service.utils.{ ControllerWithBodyParseJson, ControllerWithGraphTraversal }
import org.apache.tinkerpop.gremlin.process.traversal.{ Order, P }
import org.apache.tinkerpop.gremlin.structure.Vertex
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.Logger
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * Created by jeberle on 25.04.17.
 */

/**
 * Continued by 3C111 on 22.08.2017
 */

@Singleton
class StorageExplorerController @Inject() (
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

  lazy val logger: Logger = Logger( "application.StorageExplorerController" )

  def retrieveFilesDate( date1: Long, date2: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>

    logger.debug( "Requests to retrieve all files between " + date1 + " and " + date2 )
    val g = graphTraversalSource
    val t = g.V().has( "system:creation_time", P.between( date1, date2 ) )

    val future: Future[Seq[PersistedVertex]] = graphExecutionContext.execute {
      Future.sequence( t.toIterable.map( v =>
        vertexReader.read( v ) ).toSeq )
    }
    future.map( s => Ok( Json.toJson( s ) ) )

  }

  def bucketList: Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    logger.debug( "Request to retrieve all buckets in the graph" )
    val g = graphTraversalSource
    val t = g.V().has( Constants.TypeKey, "resource:bucket" )

    val future: Future[Seq[PersistedVertex]] = graphExecutionContext.execute {
      Future.sequence( t.toIterable.map( v =>
        vertexReader.read( v ) ).toSeq )
    }
    future.map( s => Ok( Json.toJson( s ) ) )

  }

  def fileList( id: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    logger.debug( "Request to retrieve all files in bucket with id " + id )

    // Check if bucket exists, to distinguish between an existing bucket without files and a node not existing or not being a bucket
    val g = graphTraversalSource
    val check_bucket = g.V( Long.box( id ) ).has( "type", "resource:bucket" )

    if ( check_bucket.isEmpty ) {
      logger.debug( "Node with id " + id + " is not a bucket, returning NotFound" )
      Future( NotFound )
    }

    else {
      logger.debug( "Returning file list for bucket with id " + id )
      val t = g.V( Long.box( id ) ).in( "resource:stored_in" ).in( "resource:has_location" ).has( Constants.TypeKey, "resource:file" )

      val future: Future[Seq[PersistedVertex]] = graphExecutionContext.execute {
        //empty list if no files, 404 if bucket not exists
        Future.sequence( t.toIterable.map( v =>
          vertexReader.read( v ) ).toSeq )
      }
      future.map( s => Ok( Json.toJson( s ) ) )
    }
  }

  /**
   * Here the id is the bucket id and the path the filename
   */
  def fileMetadatafromPath( id: Long, path: String ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    logger.debug( "Request to retrieve file metadata from bucket with " + id + " with path " + path )
    val g = graphTraversalSource
    val t = g.V().has( "resource:file_name", path ).as( "data" ).out( "resource:has_location" ).out( "resource:stored_in" ).V( Long.box( id ) ).as( "bucket" ).select[Vertex]( "data", "bucket" )

    Future.sequence( graphExecutionContext.execute {
      if ( t.hasNext ) {
        logger.debug( "Returning meta data for file with path " + path + " and bucket with id " + id )
        import scala.collection.JavaConverters._
        val jmap: Map[String, Vertex] = t.next().asScala.toMap
        for {
          ( key, value ) <- jmap
        } yield for {
          vertex <- vertexReader.read( value )
        } yield key -> vertex
      }
      else {
        logger.debug( "No file with path " + path + " or bucket with id " + id + " found" )
        Seq.empty
      }
    } ).map( a => a.toList match {
      case x :: xs => Ok( Json.toJson( ( x :: xs ).toMap ) )
      case _       => NotFound
    } )
  }

  /*
  Returns [Map[String, PersistedVertex]] with keys = "data", "bucket"
   */

  def fileMetadata( id: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    logger.debug( "Request to retrieve file metadata from file with id " + id )
    val g = graphTraversalSource
    val t = g.V( Long.box( id ) ).as( "data" ).out( "resource:has_location" ).out( "resource:stored_in" ).as( "bucket" ).select[Vertex]( "data", "bucket" )

    Future.sequence( graphExecutionContext.execute {
      if ( t.hasNext ) {
        logger.debug( "Returning metadata for file with id " + id )
        import scala.collection.JavaConverters._
        val jmap: Map[String, Vertex] = t.next().asScala.toMap
        for {
          ( key, value ) <- jmap
        } yield for {
          vertex <- vertexReader.read( value )
        } yield key -> vertex
      }
      else {
        // The file not having any meta data option should not exist, since it has at least a label saying it is in fact a file
        logger.debug( "Node with id " + id + " is not a file or does not exist, returning NotFound" )
        Seq.empty
      }

    } ).map( a => a.toList match {
      case x :: xs => Ok( Json.toJson( ( x :: xs ).toMap ) )
      case _       => NotFound
    } )
  }

  def bucketMetadata( id: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    logger.debug( "Request to retrieve bucket metadata from bucket with " + id )
    val g = graphTraversalSource
    val t = g.V( Long.box( id ) ).has( Constants.TypeKey, "resource:bucket" )

    val future: Future[Option[PersistedVertex]] = graphExecutionContext.execute {
      if ( t.hasNext ) {
        val vertex = t.next()
        vertexReader.read( vertex ).map( Some.apply )
      }
      else
        Future.successful( None )
    }

    future.map {
      case Some( vertex ) =>
        if ( vertex.types.contains( NamespaceAndName( "resource:bucket" ) ) ) {
          logger.debug( "Returning metadata for bucket with id " + id )
          Ok( Json.toJson( vertex )( PersistedVertexFormat ) )
        }

        else {
          logger.debug( "Node with id" + id + " ??? " )
          NotAcceptable // It is not possible to get here imo, this might need to be removed
        }

      case None =>
        logger.debug( "Node with id " + id + " not found or not a bucket, returning NotFound" )
        NotFound
    }
  }

  def retrievefileVersions( id: Long ): Action[AnyContent] = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    logger.debug( "Request to retrieve all file versions from file with id " + id )
    val g = graphTraversalSource
    val check_file = g.V( Long.box( id ) ).has( "type", "resource:file" )

    if ( check_file.isEmpty ) {
      logger.debug( "Node with id " + id + " is not a file or does not exist, returning NotFound" )
      Future( NotFound )
    }
    else {
      logger.debug( "Returning file versions for file with id " + id )
      val t = g.V( Long.box( id ) ).inE( "resource:version_of" ).outV().order().by( "system:creation_time", Order.decr )

      val future: Future[Seq[PersistedVertex]] = graphExecutionContext.execute {
        Future.sequence( t.toIterable.map( v =>
          vertexReader.read( v ) ).toSeq )
      }
      future.map( s => Ok( Json.toJson( s ) ) )
    }
  }

  def retrieveByUserName( userId: String ) = ProfileFilterAction( jwtVerifier.get ).async { implicit request =>
    val n = 100
    logger.debug( "Request to retrieve at most " + n + " nodes for user " + userId )
    val g = graphTraversalSource
    val t = g.V().has( "resource:owner", userId ).limit( n )

    val future: Future[Seq[PersistedVertex]] = graphExecutionContext.execute {
      Future.sequence( t.toIterable.map( v =>
        vertexReader.read( v ) ).toSeq )
    }
    future.map( s => Ok( Json.toJson( s ) ) )

  }

  private[this] implicit lazy val persistedVertexFormat = PersistedVertexFormat
  private[this] implicit lazy val persistedEdgeFormat = PersistedEdgeFormat
}
