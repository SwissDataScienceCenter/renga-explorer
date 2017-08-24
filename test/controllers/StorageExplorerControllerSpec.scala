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
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }

trait SpecSetup {
  //move stuff here
  val tokenBuilder = JWT.create()
}

class StorageExplorerControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with BeforeAndAfter with SpecSetup {

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
  val token = tokenBuilder.sign( tokenSignerProvider.get )

  val fakerequest = FakeRequest().withToken( token )

  implicit val reads: Reads[PersistedVertex] = PersistedVertexFormat

  before {
    ImportJSONGraph.populateGraph( graph )
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

  /*  "The return all nodes controller" should {
    "return all nodes in the graph" in {
      val result = explorerController.retrieveGraphSubset().apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      val graphNodes = g.V().asScala.toList

      ( content.length == graphNodes.length ) mustBe true
      ( content.toSet == graphNodes.toSet ) mustBe true
    }
  }

*/

  def getBucketsFromGraph() = {
    val buckets = g.V().has( "resource:bucket_name" ).asScala.toList
    for ( item <- buckets ) yield item.id()
  }

  // get some negative tests on an empty graph
  "The bucket exploration controller" should {
    "return all buckets" in {

      val result = explorerController.bucketList().apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      val contentBucketIds = for ( item <- content ) yield ( item.id )

      val graphBucketIds = getBucketsFromGraph()

      ( contentBucketIds.toSet == graphBucketIds.toSet ) mustBe true
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

      ( bucketName == graphBucketMetaData( 0 ) ) mustBe true
      ( bucketBackend == graphBucketMetaData( 1 ) ) mustBe true
      ( bucketBackedId == graphBucketMetaData( 2 ) ) mustBe true
    }
  }

  "The file exploration controller" should {
    "return all files in a bucket" in {
      // file location not bucket -> find bucket connected to file location
      val graphBucketId = getBucketsFromGraph().head

      val bucketId = graphBucketId.toString.toLong

      val result = explorerController.fileList( bucketId ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]
      val fileNames = for ( file <- content ) yield file.properties.get( NamespaceAndName( "resource", "file_name" ) ).orNull.values.head.self

      val graphFiles = g.V( graphBucketId ).in( "resource:stored_in" ).in( "resource:has_location" ).has( Constants.TypeKey, "resource:file" ).asScala.toList
      val graphFileNames = for ( file <- graphFiles ) yield file.value[String]( "resource:file_name" )
      ( content.length == graphFiles.length ) mustBe true
      ( fileNames.toList == graphFileNames ) mustBe true

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

      ( content.length == graphVersions.length ) mustBe true

      ( contentTimeStamps.toSet == graphVersionTimeStamps.toSet ) mustBe true

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

      ( graphFileName == fileName ) mustBe true

      val bucketMetaValues = getBucketMeta( content )

      val graphFileBucketId = g.V( graphFileId ).out( "resource:has_location" ).out( "resource:stored_in" ).has( Constants.TypeKey, "resource:bucket" ).asScala.toList.head.id()
      val graphBucketMetaData = graphBucketMeta( graphFileBucketId.toString )

      ( graphBucketMetaData( 0 ) == bucketMetaValues( 0 ) ) mustBe true
      ( graphBucketMetaData( 1 ) == bucketMetaValues( 1 ) ) mustBe true
    }
  }

  "The file meta data from path exploration controller" should {
    "return all metadata of a file " in {

      // find a file in the graph to use for tests
      val graphFile = g.V().in( "resource:stored_in" ).in( "resource:has_location" ).has( Constants.TypeKey, "resource:file" ).asScala.toList.head
      val path = graphFile.values[String]( "resource:file_name" ).asScala.toList.head

      // a file might be stored in several buckets but the function requires only 1
      val bucketId = g.V().out( "resource:has_location" ).out( "resource:stored_in" ).asScala.toList.head
      val result = explorerController.fileMetadatafromPath( bucketId.id.toString.toLong, path ).apply( fakerequest )
      val content = contentAsJson( result ).as[Map[String, PersistedVertex]]

      val fileName = getFileNameMeta( content )
      val graphFileName = g.V().has( "resource:file_name", "file_01.txt" ).values[String]( "resource:file_name" )

      val bucketMetaValues = getBucketMeta( content )

      val graphBucketMetaData = graphBucketMeta( bucketId.id.toString )

      ( graphBucketMetaData( 0 ) == bucketMetaValues( 0 ) ) mustBe true
      ( graphBucketMetaData( 1 ) == bucketMetaValues( 1 ) ) mustBe true

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

      val graph = g.V().has( "system:creation_time", P.between( date1, date2 ) ).values[String]().asScala.toList

      //( graph.length == content.length ) mustBe true
      //      ( graph.toSet == content.toSet ) mustBe true
      println( graph, content )
    }
  }

  "The graph metadata controller " should {
    "return the count of all edges and nodes" in {

    }
  }

}
