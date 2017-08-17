package controllers

import authorization.{ JWTVerifierProvider, MockJWTVerifierProvider, MockTokenSignerProvider }
import ch.datascience.service.utils.persistence.graph.{ JanusGraphProvider, JanusGraphTraversalSourceProvider, MockJanusGraphProvider }
import helpers.ImportJSONGraph
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.test.Helpers._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ Result }
import play.api.test._
import ch.datascience.service.security.FakeRequestWithToken._
import com.auth0.jwt.JWT

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
    .build()

  val explorerController: StorageExplorerController = app.injector.instanceOf[StorageExplorerController]

  val graph = app.injector.instanceOf[JanusGraphProvider].get
  val g = app.injector.instanceOf[JanusGraphTraversalSourceProvider].get

  val tokenSignerProvider = app.injector.instanceOf[MockTokenSignerProvider]
  val token = tokenBuilder.sign( tokenSignerProvider.get )

  val fakerequest = FakeRequest().withToken( token )

  before {
    ImportJSONGraph.populateGraph( graph )
  }

  after {
    graph.traversal().V().drop().iterate()
  }

  "The traversal " should {
    "not be empty" in { g.V().valueMap().hasNext mustBe true }

  }

  // get some negative tests on an empty graph
  "The bucket exploration controller" should {
    "return all buckets" in {
      implicit val ec = ExecutionContext.global
      val result: Future[Result] = explorerController.bucketList().apply( fakerequest )
      // for { r <- result } yield r
      //  val content = contentAsString( result )
      val buckets = g.V().has( "resource:bucket_name" )
      //  buckets.toList = [[v[28704], v[32800]]
    }
    /* }

     "The bucket metadata exploration controller" should {
       "return all metadata of a file" in {
         val bucket_id = ( "1502777038524" ).toLong //get from graph
         val action = explorerController.bucketMetadata( bucket_id ).apply( fakerequest )
       }
     }

     "The file exploration controller" should {
       "return all files in a bucket" in {
         // file location not bucket -> find bucket connected to file location
         val bucket_id = ( "1502777038524" ).toLong //get from graph
         val action = explorerController.fileList( bucket_id ).apply( fakerequest )

       }
     }

     "The file metadata exploration controller" should {
       "return all metadata of a file" in {
         val file_id = ( "1502777038524" ).toLong //get from graph
         val action = explorerController.fileMetadata( file_id ).apply( fakerequest )

       }*/
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
