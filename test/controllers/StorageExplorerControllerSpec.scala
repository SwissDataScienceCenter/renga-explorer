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

import authorization.{ JWTVerifierProvider, MockJWTVerifierProvider, MockTokenSignerProvider }
import ch.datascience.graph.Constants
import ch.datascience.graph.elements.persisted.PersistedVertex
import ch.datascience.graph.elements.persisted.json._
import ch.datascience.graph.naming.NamespaceAndName
import ch.datascience.service.utils.persistence.graph.{ JanusGraphProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.scope.Scope
import ch.datascience.test.security.FakeRequestWithToken._
import ch.datascience.test.utils.persistence.graph.MockJanusGraphProvider
import ch.datascience.test.utils.persistence.scope.MockScope
import com.auth0.jwt.JWT
import helpers.ImportJSONGraph
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Reads
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class StorageExplorerControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with BeforeAndAfter {

  // Set the stage
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides( bind[JWTVerifierProvider].to[MockJWTVerifierProvider] )
    .overrides( bind[JanusGraphProvider].to[MockJanusGraphProvider] )
    .overrides( bind[Scope].to[MockScope] )
    .build()

  val explorerController: StorageExplorerController = app.injector.instanceOf[StorageExplorerController]

  val graph = app.injector.instanceOf[JanusGraphProvider].get
  val g = app.injector.instanceOf[JanusGraphTraversalSourceProvider].get

  val tokenSignerProvider = app.injector.instanceOf[MockTokenSignerProvider]
  val tokenBuilder = JWT.create()
  val token = tokenBuilder.sign( tokenSignerProvider.get )
  val fakerequest = FakeRequest().withToken( token )

  implicit val reads: Reads[PersistedVertex] = PersistedVertexFormat

  before {
    ImportJSONGraph.storageGraph( graph )
  }

  after {
    graph.traversal().V().drop().iterate()
  }

  "The traversal " should {
    "not be empty" in {
      g.V().valueMap().hasNext mustBe true
    }
  }

  implicit val ec = ExecutionContext.global

  def getBucketsFromGraph() = {
    val buckets = g.V().has( "resource:bucket_name" ).asScala.toList
    for ( item <- buckets ) yield item.id()
  }

  // get some negative tests on an empty graph
  "The bucket exploration controller" should {
    "return all buckets" in {

      val result = explorerController.bucketList().apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      val contentBucketIds = for ( item <- content ) yield item.id

      val graphBucketIds = getBucketsFromGraph()

      contentBucketIds.toSet mustBe graphBucketIds.toSet
    }
  }

  "The bucket exploration controller" should {
    "return no buckets if there are none in the graph" in {
      graph.traversal().V().drop().iterate()
      ImportJSONGraph.noBucketsGraph( graph )

      val result = explorerController.bucketList().apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      content.length mustBe 0
    }
  }

  def graphBucketMeta( nodeId: String ) = {
    val graphBucketName = g.V( nodeId ).values[String]( "resource:bucket_name" ).asScala.toList.head
    val graphBucketBackend = g.V( nodeId ).values[String]( "resource:bucket_backend" ).asScala.toList.head
    val graphBucketBackendId = g.V( nodeId ).values[String]( "resource:bucket_backend_id" ).asScala.toList.head
    List( graphBucketName, graphBucketBackend, graphBucketBackendId )
  }

  "The bucket metadata exploration controller" should {
    "return all metadata of a bucket" in {

      val graphBucketId = getBucketsFromGraph().head

      val bucketId = graphBucketId.toString.toLong

      val result = explorerController.bucketMetadata( bucketId ).apply( fakerequest )
      val content = contentAsJson( result ).as[PersistedVertex]

      val bucketName = content.properties.get( NamespaceAndName( "resource", "bucket_name" ) ).orNull.values.head.self
      val bucketBackend = content.properties.get( NamespaceAndName( "resource", "bucket_backend" ) ).orNull.values.head.self
      val bucketBackedId = content.properties.get( NamespaceAndName( "resource", "bucket_backend_id" ) ).orNull.values.head.self

      val graphBucketMetaData = graphBucketMeta( graphBucketId.toString )

      bucketName mustBe graphBucketMetaData( 0 )
      bucketBackend mustBe graphBucketMetaData( 1 )
      bucketBackedId mustBe graphBucketMetaData( 2 )
    }
  }

  "The bucket metadata exploration controller" should {
    "return 404 NotFound if the node is not a bucket" in {
      val id = g.V().has( Constants.TypeKey, "resource:file" ).asScala.toList.head.id

      val result = explorerController.bucketMetadata( id.toString.toLong ).apply( fakerequest )

      val resultStatus = result.map( x => x.header.status )

      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The bucket metadata exploration controller" should {
    "return a 404 NotFound if the node does not exist" in {

      val result = explorerController.bucketMetadata( "32".toLong ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )

      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The file exploration controller" should {
    "return all files in a bucket" in {
      // file location not bucket -> find bucket connected to file location
      val graphBucketId = getBucketsFromGraph().head

      val bucketId = graphBucketId.toString.toLong

      val result = explorerController.fileList( bucketId, None ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]
      val fileNames = for ( file <- content ) yield file.properties.get( NamespaceAndName( "resource", "file_name" ) ).orNull.values.head.self

      val graphFiles = g.V( graphBucketId ).in( "resource:stored_in" ).in( "resource:has_location" ).has( Constants.TypeKey, "resource:file" ).asScala.toList
      val graphFileNames = for ( file <- graphFiles ) yield file.value[String]( "resource:file_name" )

      content.length mustBe graphFiles.length
      fileNames.toList mustBe graphFileNames
    }
  }

  "The file exploration controller" should {
    "return an empty list if there are no files in the bucket" in {
      graph.traversal().V().drop().iterate()
      ImportJSONGraph.noFilesGraph( graph )

      val graphBucketId = getBucketsFromGraph().head
      val bucketId = graphBucketId.toString.toLong

      val result = explorerController.fileList( bucketId, None ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      content.length mustBe 0
    }
  }

  "The file exploration controller" should {
    "a limited list of query parameter n is given" in {
      val id = g.V().has( Constants.TypeKey, "resource:file_version" ).asScala.toList.head.id

      val result = explorerController.fileList( id.toString.toLong, None ).apply( fakerequest )

      val resultStatus = result.map( x => x.header.status )
      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The file exploration controller" should {
    "should return 404 if the node does not exist" in {

      val result = explorerController.fileList( "5".toLong, None ).apply( fakerequest )

      val resultStatus = result.map( x => x.header.status )
      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The file exploration controller" should {
    "should not the amount of returned files if the query param is not given" in {
      val graphBucketId = getBucketsFromGraph().head

      val bucketId = graphBucketId.toString.toLong

      val result = explorerController.fileList( bucketId, None ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      val graphFiles = g.V( graphBucketId ).in( "resource:stored_in" ).in( "resource:has_location" ).has( Constants.TypeKey, "resource:file" ).asScala.toList

      content.length mustBe graphFiles.length
    }
  }

  "The file exploration controller" should {
    "should limit the amount of returned files if the query param is given " in {
      val graphBucketId = getBucketsFromGraph().head

      val bucketId = graphBucketId.toString.toLong

      val result = explorerController.fileList( bucketId, Some( 1 ) ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      content.length mustBe 1
    }
  }

  "The file version exploration controller" should {
    "return all versions of a file " in {

      val graphFiles = g.V().in( "resource:stored_in" ).in( "resource:has_location" ).has( Constants.TypeKey, "resource:file" ).asScala.toList
      val graphFileId = ( for ( file <- graphFiles ) yield file.id() ).head

      val graphVersions = g.V( graphFileId ).inE( "resource:version_of" ).outV().asScala.toList
      val graphVersionTimeStamps = for ( node <- graphVersions ) yield node.value[Long]( "system:creation_time" )

      val result = explorerController.retrievefileVersions( graphFileId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]
      val contentTimeStamps = for ( node <- content ) yield node.properties.get( NamespaceAndName( "system", "creation_time" ) ).orNull.values.head.self

      content.length mustBe graphVersions.length
      contentTimeStamps.toSet mustBe graphVersionTimeStamps.toSet
    }
  }

  "The file version controller" should {
    "return a NotFound if the requested node does not exist" in {

      val result = explorerController.retrievefileVersions( "3".toLong ).apply( fakerequest )

      val resultStatus = result.map( x => x.header.status )
      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The file version controller" should {
    "return a NotFound if the requested node is not a file" in {
      val id = g.V().has( Constants.TypeKey, "resource:bucket" ).asScala.toList.head.id
      val result = explorerController.retrievefileVersions( id.toString.toLong ).apply( fakerequest )

      val resultStatus = result.map( x => x.header.status )
      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  // Use these helper functions only if content is a map

  def getFileNameMeta( content: Map[String, PersistedVertex] ) = {
    val contentFile = content.get( "data" ).orNull
    contentFile.properties.get( NamespaceAndName( "resource", "file_name" ) ).orNull.values.head.self
  }

  def getBucketMeta( content: Map[String, PersistedVertex] ) = {
    val contentBucket = content.get( "bucket" ).orNull
    val bucketName = contentBucket.properties.get( NamespaceAndName( "resource", "bucket_name" ) ).orNull.values.head.self
    val bucketBackend = contentBucket.properties.get( NamespaceAndName( "resource", "bucket_backend" ) ).orNull.values.head.self
    List( bucketName, bucketBackend )
  }

  "The file metadata exploration controller" should {
    "return all metadata of a file" in {

      val graphFiles = g.V().in( "resource:stored_in" ).in( "resource:has_location" ).has( Constants.TypeKey, "resource:file" ).asScala.toList
      val graphFileId = ( for ( file <- graphFiles ) yield file.id() ).head

      val result = explorerController.fileMetadata( graphFileId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[Map[String, PersistedVertex]]

      val fileName = getFileNameMeta( content )
      val graphFileName = g.V( graphFileId ).values[String]( "resource:file_name" ).asScala.toList.head

      graphFileName mustBe fileName

      val bucketMetaValues = getBucketMeta( content )

      val graphFileBucketId = g.V( graphFileId ).out( "resource:has_location" ).out( "resource:stored_in" ).has( Constants.TypeKey, "resource:bucket" ).asScala.toList.head.id()
      val graphBucketMetaData = graphBucketMeta( graphFileBucketId.toString )

      graphBucketMetaData( 0 ) mustBe bucketMetaValues( 0 )
      graphBucketMetaData( 1 ) mustBe bucketMetaValues( 1 )
    }
  }

  "The file meta data exploration controller" should {
    "return 404 NotFound if the node is not a file " in {

      val id = g.V().has( Constants.TypeKey, "resource:bucket" ).asScala.toList.head.id
      val result = explorerController.fileMetadata( id.toString.toLong ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )
      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The file meta data exploration controller" should {
    "return 404 NotFound if the file does not exist " in {

      val result = explorerController.fileMetadata( "3".toLong ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )
      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The file meta data from path exploration controller" should {
    "return all metadata of a file " in {

      val graphFile = g.V().in( "resource:stored_in" ).in( "resource:has_location" ).has( Constants.TypeKey, "resource:file" ).asScala.toList.head
      val path = graphFile.values[String]( "resource:file_name" ).asScala.toList.head

      val bucketId = g.V().out( "resource:has_location" ).out( "resource:stored_in" ).asScala.toList.head
      val result = explorerController.fileMetadatafromPath( bucketId.id.toString.toLong, path ).apply( fakerequest )
      val content = contentAsJson( result ).as[Map[String, PersistedVertex]]

      val fileName = getFileNameMeta( content )
      val graphFileName = g.V().has( "resource:file_name", fileName ).values[String]( "resource:file_name" ).asScala.toList.head

      graphFileName mustBe fileName

      val bucketMetaValues = getBucketMeta( content )
      val graphBucketMetaData = graphBucketMeta( bucketId.id.toString )

      graphBucketMetaData( 0 ) mustBe bucketMetaValues( 0 )
      graphBucketMetaData( 1 ) mustBe bucketMetaValues( 1 )
    }
  }

  "The file meta data from path exploration controller" should {
    "return 404 NotFound if the filepath does not exist " in {

      val bucketId = g.V().out( "resource:has_location" ).out( "resource:stored_in" ).asScala.toList.head
      val result = explorerController.fileMetadatafromPath( bucketId.id.toString.toLong, "fake_path" ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )
      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The file meta data from path exploration controller" should {
    "return 404 NotFound if the bucket does not exist " in {

      val graphFile = g.V().in( "resource:stored_in" ).in( "resource:has_location" ).has( Constants.TypeKey, "resource:file" ).asScala.toList.head
      val path = graphFile.values[String]( "resource:file_name" ).asScala.toList.head

      val result = explorerController.fileMetadatafromPath( "3".toLong, path ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )
      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The date search controller " should {
    "return all nodes with a specific timestamp" in {
      val graphFile = g.V().has( Constants.TypeKey, "resource:file_version" ).asScala.toList
      val dates = graphFile.head.values[Long]( "system:creation_time" ).asScala.toList

      val date1 = dates.head - 1
      val date2 = dates.last + 1
      val result = explorerController.retrieveFilesDate( date1, date2 ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      val graph = g.V().has( "system:creation_time", P.between( date1, date2 ) ).asScala.toList

      graph.length mustBe content.length

      val contentTimestamps = for ( node <- content ) yield node.properties.get( NamespaceAndName( "system", "creation_time" ) ).orNull.values.head.self
      val graphTimestamps = for ( vertex <- graph ) yield vertex.value[Long]( "system:creation_time" )

      contentTimestamps.toSet mustBe graphTimestamps.toSet
    }
  }

  "The date search controller" should {
    "return an empty list if no nodes have the timestamp requested " in {
      val graphFile = g.V().has( Constants.TypeKey, "resource:file_version" ).asScala.toList
      val dates = graphFile.head.values[Long]( "system:creation_time" ).asScala.toList

      val date1 = dates.head + 100
      val date2 = dates.last + 101
      val result = explorerController.retrieveFilesDate( date1, date2 ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      content.length mustBe 0
    }
  }

  // TODO add following tests again if the dates are query params
  /*"The date search controller" should {
    "return set the first date to 0 if no date1 is given " in {
      val graphFile = g.V().has( Constants.TypeKey, "resource:file_version" ).asScala.toList
      val dates = graphFile.head.values[Long]( "system:creation_time" ).asScala.toList

      val date1 = 0
      val date2 = dates.last + 101
      val result = explorerController.retrieveFilesDate( None, Some( date2 ) ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      val graph = g.V().has( "system:creation_time", P.between( date1, date2 ) ).asScala.toList

      graph.length mustBe content.length

      val contentTimestamps = for ( node <- content ) yield node.properties.get( NamespaceAndName( "system", "creation_time" ) ).orNull.values.head.self
      val graphTimestamps = for ( vertex <- graph ) yield vertex.value[Long]( "system:creation_time" )

      contentTimestamps.toSet mustBe graphTimestamps.toSet
    }
  }

  "The date search controller" should {
    "return set the last date to now if no date2 is given " in {
      val graphFile = g.V().has( Constants.TypeKey, "resource:file_version" ).asScala.toList
      val dates = graphFile.head.values[Long]( "system:creation_time" ).asScala.toList

      val date1 = 100
      val date2 = System.currentTimeMillis
      val result = explorerController.retrieveFilesDate( Some( date1 ), None ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      val graph = g.V().has( "system:creation_time", P.between( date1, date2 ) ).asScala.toList

      graph.length mustBe content.length

      val contentTimestamps = for ( node <- content ) yield node.properties.get( NamespaceAndName( "system", "creation_time" ) ).orNull.values.head.self
      val graphTimestamps = for ( vertex <- graph ) yield vertex.value[Long]( "system:creation_time" )

      contentTimestamps.toSet mustBe graphTimestamps.toSet
    }
  }
*/
  "The user-search controller " should {
    "return all vertices owned by that user" in {

      val graphUserId = g.V().has( "resource:owner" ).asScala.toList.head.values[String]( "resource:owner" ).asScala.toList.head
      val t = g.V().has( "resource:owner", graphUserId ).asScala.toSeq

      val result = explorerController.retrieveByUserName( graphUserId ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      // Assuming our test graph is not larger than 100 nodes
      t.length mustBe content.length

      val content_owners = for ( vertex <- content ) yield vertex.properties.get( NamespaceAndName( "resource", "owner" ) ).orNull.values.head.self

      content_owners.toSet( graphUserId ) mustBe true
      content_owners.toSet.toList.length mustBe 1
    }
  }

  "The notebook controller " should {
    "return notebooks from a bucket if they are present" in {

      val notebookfile = g.V().has( "resource:file_name", "testnotebook.ipynb" ).asScala.toList.head //This is hardcoded, be careful when changing the test graph
      val bucketId = g.V( notebookfile.id ).out( "resource:has_location" ).out( "resource:stored_in" ).asScala.toList.head.id

      val result = explorerController.retrieveNotebooks( bucketId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]
      val filenames = for ( item <- content ) yield item.properties.get( NamespaceAndName( "resource", "file_name" ) ).orNull.values.head.self
      filenames.head mustBe "testnotebook.ipynb"
    }
  }

  "The notebook controller " should {
    "return r?? if there are notebooks in the bucket" in {

      val bucketId = g.V().has( "resource:bucket_name", "bucket_01" ).asScala.toList.head.id //This is hardcoded, be careful when changing the test graph
      val result = explorerController.retrieveNotebooks( bucketId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      content.length mustBe 0

    }
  }
}
