package controllers

import authorization.{ JWTVerifierProvider, MockJWTVerifierProvider, MockTokenSignerProvider }
import ch.datascience.graph.elements.Vertex
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
import org.apache.tinkerpop.gremlin.structure.Vertex
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
  import scala.collection.JavaConverters._

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

  // get some negative tests on an empty graph
  "The bucket exploration controller" should {
    "return all buckets" in {
      implicit val ec = ExecutionContext.global
      val result: Future[Result] = explorerController.bucketList().apply( fakerequest )

      val content = contentAsJson( result ).as[Seq[PersistedVertex]]
      val contentBucketIds = for ( item <- content ) yield ( item.id )

      val buckets = g.V().has( "resource:bucket_name" ).asScala.toList
      val graphBucketIds = for ( item <- buckets ) yield ( item.id() )

      ( contentBucketIds.toSet == graphBucketIds.toSet ) mustBe true
    }
  }

  "The bucket metadata exploration controller" should {
    "return all metadata of a bucket" in {
      val buckets = g.V().has( "resource:bucket_name" ).asScala.toList
      val graphBucketIds = for ( item <- buckets ) yield ( item.id() )

      val bucketId = graphBucketIds( 1 ).toString().toLong

      val result = explorerController.bucketMetadata( bucketId ).apply( fakerequest )
      val content = contentAsJson( result ).as[PersistedVertex]

      val bucketName = content.properties.get( NamespaceAndName( "resource", "bucket_name" ) ).orNull.values.head.self
      val bucketBackend = content.properties.get( NamespaceAndName( "resource", "bucket_backend" ) ).orNull.values.head.self
      val bucketBackedId = content.properties.get( NamespaceAndName( "resource", "bucket_backend_id" ) ).orNull.values.head.self

      val graphBucketName = g.V( graphBucketIds( 1 ) ).values[String]( "resource:bucket_name" ).asScala.toList.head
      val graphBucketBackend = g.V( graphBucketIds( 1 ) ).values[String]( "resource:bucket_backend" ).asScala.toList.head
      val graphBucketBackendId = g.V( graphBucketIds( 1 ) ).values[String]( "resource:bucket_backend_id" ).asScala.toList.head

      ( bucketName == graphBucketName ) mustBe true
      ( bucketBackend == graphBucketBackend ) mustBe true
      ( bucketBackedId == graphBucketBackendId ) mustBe true
    }
  }

  "The file exploration controller" should {
    "return all files in a bucket" in {
      // file location not bucket -> find bucket connected to file location
      val buckets = g.V().has( "resource:bucket_name" ).asScala.toList
      val graphBucketIds = for ( item <- buckets ) yield ( item.id() )

      val bucketId = graphBucketIds( 1 ).toString.toLong

      val result = explorerController.fileList( bucketId ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]
      val fileNames = for ( file <- content ) yield ( file.properties.get( NamespaceAndName( "resource", "file_name" ) ).orNull.values.head.self )

      val graphFiles = g.V( graphBucketIds( 1 ) ).in( "resource:stored_in" ).in( "resource:has_location" ).has( "type", "resource:file" ).asScala.toList
      val graphFileNames = for ( file <- graphFiles ) yield ( file.value[String]( "resource:file_name" ) )
      ( content.length == graphFiles.length ) mustBe true
      ( fileNames.toList == graphFileNames ) mustBe true

    }
  }

  "The file metadata exploration controller" should {
    "return all metadata of a file" in {

      val graphFiles = g.V().in( "resource:stored_in" ).in( "resource:has_location" ).has( "type", "resource:file" ).asScala.toList
      val graphFileId = ( for ( file <- graphFiles ) yield ( file.id() ) ).head

      val result = explorerController.fileMetadata( graphFileId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[Map[String, PersistedVertex]]

      val contentFile = content.get( "data" ).orNull
      val fileName = contentFile.properties.get( NamespaceAndName( "resource", "file_name" ) ).orNull.values.head.self

      val graphFile = g.V( graphFileId ).values[String]().asScala.toList

      ( graphFile.contains( fileName ) ) mustBe true

      val contentBucket = content.get( "bucket" ).orNull
      val bucketName = contentBucket.properties.get( NamespaceAndName( "resource", "bucket_name" ) ).orNull.values.head.self
      val bucketBackend = contentBucket.properties.get( NamespaceAndName( "resource", "bucket_backend" ) ).orNull.values.head.self

      val graphFileBucket = g.V( graphFileId ).out( "resource:has_location" ).out( "resource:stored_in" ).has( "type", "resource:bucket" ).values[String]().asScala.toList

      ( graphFileBucket.contains( bucketName ) ) mustBe true
      ( graphFileBucket.contains( bucketBackend ) ) mustBe true
    }
  }

  "The file meta data from path exploration controller" should {
    "return all metadata of a file " in {
      val fileId = 0
      val path = ""
      val result = explorerController.fileMetadatafromPath( fileId, path )
    }
  }
}
