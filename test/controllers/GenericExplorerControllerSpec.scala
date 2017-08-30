package controllers

import authorization.{ JWTVerifierProvider, MockJWTVerifierProvider, MockTokenSignerProvider }
import ch.datascience.graph.elements.persisted.PersistedVertex
import ch.datascience.graph.elements.persisted.json._
import ch.datascience.service.utils.persistence.graph.{ JanusGraphProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.scope.Scope
import ch.datascience.test.security.FakeRequestWithToken._
import ch.datascience.test.utils.persistence.graph.MockJanusGraphProvider
import ch.datascience.test.utils.persistence.scope.MockScope
import com.auth0.jwt.JWT
import helpers.ImportJSONGraph
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

/**
 * Continued by 3C111 on 29.08.2017
 */

class GenericExplorerControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with BeforeAndAfter {

  // Set the stage
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides( bind[JWTVerifierProvider].to[MockJWTVerifierProvider] )
    .overrides( bind[JanusGraphProvider].to[MockJanusGraphProvider] )
    .overrides( bind[Scope].to[MockScope] )
    .build()

  val genericController: GenericExplorerController = app.injector.instanceOf[GenericExplorerController]

  val graph = app.injector.instanceOf[JanusGraphProvider].get
  val g = app.injector.instanceOf[JanusGraphTraversalSourceProvider].get

  val tokenSignerProvider = app.injector.instanceOf[MockTokenSignerProvider]
  val tokenBuilder = JWT.create()
  val token = tokenBuilder.sign( tokenSignerProvider.get )
  val fakerequest = FakeRequest().withToken( token )

  implicit val reads: Reads[PersistedVertex] = PersistedVertexFormat

  before {
    ImportJSONGraph.populateGraph( graph )
  }

  after {
    graph.traversal().V().drop().iterate()
  }

  "The return graph subset controller" should {
    "return a subset of nodes and edges from the graph" in {
      val result = genericController.retrieveGraphSubset().apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[GraphSubSet]]
      val firstSubset = content.head

      firstSubset.node1.properties.values.isEmpty mustBe false
      firstSubset.edge.id.nonEmpty mustBe true
      firstSubset.node2.properties.values.isEmpty mustBe false
    }
  }

  "The generic node metadata controller " should {
    "return a specific nodes metadata" in {

      val nodeId = g.V().asScala.toList.head.id
      val nodeMetaData = g.V( nodeId )
      val result = genericController.retrieveNodeMetaData( nodeId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[PersistedVertex]

      content.properties.isEmpty mustBe false

      // TODO invent a test that is applicable to all cases

    }
  }
}
