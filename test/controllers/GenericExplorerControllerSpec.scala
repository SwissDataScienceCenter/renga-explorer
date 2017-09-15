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
import ch.datascience.graph.elements.persisted.PersistedVertex
import ch.datascience.graph.elements.persisted.json._
import ch.datascience.service.utils.persistence.graph.{ JanusGraphProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.scope.Scope
import ch.datascience.test.security.FakeRequestWithToken._
import ch.datascience.test.utils.persistence.graph.MockJanusGraphProvider
import ch.datascience.test.utils.persistence.scope.MockScope
import com.auth0.jwt.JWT
import helpers.{ ImportJSONStorageGraph, ImportJSONStorageNoBucketsGraph }
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
    ImportJSONStorageGraph.populateGraph( graph )
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
  "The generic node metadata controller" should {
    "return an empty list if the node does not exist" in {

      val nodeId = 4
      val result = genericController.retrieveNodeMetaData( nodeId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result )

      ( content.toString() == "null" ) mustBe true

    }
  }

  "The property search controller" should {
    "return the nodes of a given property if they exist" in {
      graph.traversal().V().drop().iterate()
      ImportJSONStorageGraph.populateGraph( graph )

      val prop = "importance"
      val t = g.V().has( prop ).asScala.toList

      val result = genericController.retrieveNodeProperty( prop ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedVertex]]

      content.length == t.length mustBe true

    }
  }

  "The property search controller" should {
    "be able to return a list of 1" in {
      graph.traversal().V().drop().iterate()
      ImportJSONStorageGraph.populateGraph( graph )

      val prop = "test"
      val t = g.V().has( prop ).asScala.toList

      val result = genericController.retrieveNodeProperty( prop ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedVertex]]

      content.length == t.length mustBe true

    }
  }

  "The property search controller" should {
    "return an empty list" in {
      val prop = "month"

      val result = genericController.retrieveNodeProperty( prop ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedVertex]]

      content.length mustBe 0
    }
  }
}

