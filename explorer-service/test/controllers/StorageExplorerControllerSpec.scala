package controllers

import akka.stream.ActorMaterializer
import authorization.{ JWTVerifierProvider, MockJWTVerifierProvider }
import ch.datascience.service.utils.persistence.graph.{ JanusGraphProvider, MockJanusGraphProvider }
import helpers.ImportJSONGraph
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test._

class StorageExplorerControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with BeforeAndAfter {
  // Set the stage
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides( bind[JWTVerifierProvider].to[MockJWTVerifierProvider] )
    .overrides( bind[JanusGraphProvider].to[MockJanusGraphProvider] )
    .build()
  val explorerController: StorageExplorerController = app.injector.instanceOf[StorageExplorerController]

  val graph = app.injector.instanceOf[JanusGraphProvider].get
  val g = graph.traversal()

  val fakerequest = FakeRequest().withBody( "" )

  before {
    ImportJSONGraph.populateGraph( graph )
  }

  after {
    g.V().drop().iterate()
  }

  "The traversal " should {
    "not be empty" in { g.V().valueMap().hasNext mustBe true }

  }
  // get some negative tests on an empty graph
  "The bucket exploration controller" should {
    "return all buckets" in {

      val action = explorerController.bucketList.apply( fakerequest ).run()
      for { result <- action } print( result )

      val buckets = g.V().has( "resource:bucket_name" )

    }
  }

  "The bucket metadata exploration controller" should {
    "return all metadata of a file" in {

      val bucket_id = ( "1502777038524" ).toLong //get from graph
      val action = explorerController.bucketMetadata( bucket_id ).apply( fakerequest ).run()
    }
  }

  "The file exploration controller" should {
    "return all files in a bucket" in {
      val bucket_id = ( "1502777038524" ).toLong //get from graph
      val action = explorerController.fileList( bucket_id ).apply( fakerequest ).run()

    }
  }

  "The file metadata exploration controller" should {
    "return all metadata of a file" in {
      val file_id = ( "1502777038524" ).toLong //get from graph
      val action = explorerController.fileMetadata( file_id ).apply( fakerequest ).run()

    }
  }

  //val c = jsonGraph.g.V().valueMap().toList()

}

/* import scala.collection.JavaConverters._
 val t = g.V()
 val s1 = t.toStream.iterator().asScala
 for (v <- s1) {
   println(v)
 }
*/
