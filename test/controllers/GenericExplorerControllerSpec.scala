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
import ch.datascience.graph.elements.persisted.{ PersistedEdge, PersistedVertex }
import ch.datascience.graph.elements.persisted.json._
import ch.datascience.service.utils.persistence.graph.{ JanusGraphProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.scope.Scope
import ch.datascience.test.security.FakeRequestWithToken._
import ch.datascience.test.utils.persistence.graph.MockJanusGraphProvider
import ch.datascience.test.utils.persistence.scope.MockScope
import com.auth0.jwt.JWT
import helpers.{ ImportJSONGraph, ObjectMatcher }
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

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
  implicit val ec = ExecutionContext.global
  val genericController: GenericExplorerController = app.injector.instanceOf[GenericExplorerController]

  val graph = app.injector.instanceOf[JanusGraphProvider].get
  val g = app.injector.instanceOf[JanusGraphTraversalSourceProvider].get

  val tokenSignerProvider = app.injector.instanceOf[MockTokenSignerProvider]
  val tokenBuilder = JWT.create()
  val token = tokenBuilder.sign( tokenSignerProvider.get )
  val fakerequest = FakeRequest().withToken( token )

  private[this] implicit lazy val persistedVertexFormat = PersistedVertexFormat
  private[this] implicit lazy val persistedEdgeFormat = PersistedEdgeFormat

  before {
    ImportJSONGraph.storageGraph( graph )
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

      val prop = "importance"
      val t = g.V().has( prop ).asScala.toList.head

      val nodeId = t.id
      val nodeMetaData = g.V( nodeId )

      val result = genericController.retrieveNodeMetaData( nodeId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[PersistedVertex]

      content.properties.isEmpty mustBe false

      // TODO invent a test that is applicable to all cases

    }
  }
  "The generic node metadata controller" should {
    "return a 404 if the node does not exist" in {

      val nodeId = 4
      val result = genericController.retrieveNodeMetaData( nodeId.toString.toLong ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )
      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }

    }
  }

  "The edge retrieval of a node controller" should {
    "return a list of in and outgoing edges if the node with id exists" in {

 
      val nodeId = g.V().has( Constants.TypeKey, "resource:file_version" ).asScala.toList.head.id
      val t = g.V( nodeId ).bothE().asScala.toList
      val t_ids = for ( i <- t ) yield i.id.toString

      val result = genericController.retrieveNodeEdges( nodeId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedEdge]]
      val content_ids = for ( i <- content ) yield i.id

      content.length mustBe t.length
      content_ids.toSet mustBe t_ids.toSet
    }
  }

  "The edge retrieval of a node controller" should {
    "return a 404 if a node with id exists but has no edges (somehow)" in {

      val nodeId = g.V().has( "single", "node" ).asScala.toList.head.id

      val result = genericController.retrieveNodeEdges( nodeId.toString.toLong ).apply( fakerequest )

      val resultStatus = result.map( x => x.header.status )

      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The edge retrieval of a node controller" should {
    "return a 404 NotFound if a node does not exist in the graph" in {

      val nodeId = g.V().has( "single", "node" ).asScala.toList.head.id

      val result = genericController.retrieveNodeEdges( nodeId.toString.toLong ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )

      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }


  "The property search controller" should {
    "return the nodes of a given property if they exist" in {

      val prop = "importance"
      val t = g.V().has( prop ).asScala.toList

      val result = genericController.retrieveNodesWithProperty( prop ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedVertex]]

      content.length mustBe t.length
    }
  }

  "The property search controller" should {
    "be able to return a list of 1" in {

      val prop = "test"
      val t = g.V().has( prop ).asScala.toList

      val result = genericController.retrieveNodesWithProperty( prop ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedVertex]]

      content.length mustBe t.length
    }
  }

  "The property search controller" should {
    "be able to return a NotFound if no nodes were found" in {

      val prop = "coffee"

      val result = genericController.retrieveNodesWithProperty( prop ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )

      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The value search controller" should {
    "return all values for a property in a list" in {

      val prop = "importance"
      val t = g.V().values[String]( prop ).asScala.toList

      val result = genericController.getValuesForProperty( prop ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[String]]

      content.toSet mustBe t.toSet
    }
  }

  "The value search controller" should {
    "return a list of non-string values (long)" in {
      val prop = "system:creation_time"
      val t = g.V().values[Any]( prop ).asScala.toList
      val stringified_t = for ( i <- t ) yield i.toString()

      val result = genericController.getValuesForProperty( prop ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[String]]

      content.length mustBe t.length
      content.toSet mustBe stringified_t.toSet
    }
  }

  "The value search controller" should {
    "return a list of non-string values (char)" in {
      val prop = "testchar"
      val t = g.V().values[Any]( prop ).asScala.toList
      val stringified_t = for ( i <- t ) yield i.toString()

      val result = genericController.getValuesForProperty( prop ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[String]]

      content.length mustBe t.length
      content.toSet mustBe stringified_t.toSet
    }
  }

  "The value search controller" should {
    "return an empty list of the property does not exist" in {

      val prop = "coffee"

      val result = genericController.getValuesForProperty( prop ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[String]]

      content.length mustBe 0
    }
  }

  "The property value controller " should {
    "return all nodes who have specific value for the property" in {

      val prop = "importance"
      val value = "high"
      val t = g.V().has( prop, value ).asScala.toList

      val result = genericController.retrieveNodePropertyAndValue( prop, value ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedVertex]]
      content.length mustBe t.length

      val tids = t.map( x => x.id() )
      val contentids = content.map( t => t.id )
      contentids.toSet mustBe tids.toSet
    }
  }

  "The property value controller " should {
    "return a 404 if that property is not found" in {

      val prop = "importance"
      val value = "urgent"

      val result = genericController.retrieveNodePropertyAndValue( prop, value ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )

      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }

  "The property value controller " should {
    "return a a 404 if a value for that property is not found" in {
      val prop = "coffee"
      val value = "strong"

      val result = genericController.retrieveNodePropertyAndValue( prop, value ).apply( fakerequest )
      val resultStatus = result.map( x => x.header.status )

      for ( status <- resultStatus ) {
        status.toString mustBe "404"
      }
    }
  }
  "The property value controller " should {
    "return a proper list if the value is not a string" in {
      val prop = "system:creation_time"
      val value = "1504099639855"

      val t1 = g.V().values[java.lang.Object]( prop ).asScala.toList.head
      val v2 = ObjectMatcher.stringToGivenType( value, t1 )

      val t = g.V().has( prop, v2 ).asScala.toList

      val result = genericController.retrieveNodePropertyAndValue( prop, value ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedVertex]]

      content.length mustBe t.length

      val tids = t.map( x => x.id() )
      val contentids = content.map( t => t.id )
      contentids.toSet mustBe tids.toSet

    }
  }

  "The property value controller" should {
    "return a proper list if the value searched for is a char" in {
      val prop = "testchar"
      val value = "a"

      val t1 = g.V().values[java.lang.Object]( prop ).asScala.toList.head
      val v2 = ObjectMatcher.stringToGivenType( value, t1 )

      val t = g.V().has( prop, v2 ).asScala.toList

      val result = genericController.retrieveNodePropertyAndValue( prop, value ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedVertex]]

      content.length mustBe t.length

      val tids = t.map( x => x.id() )
      val contentids = content.map( t => t.id )
      contentids.toSet mustBe tids.toSet
    }
  }

  "The property value controller " should {
    "return a proper list if the value is a string with numbers" in {
      val prop = "city"
      val value = "1234"
      val t = g.V().has( prop, value ).asScala.toList

      val result = genericController.retrieveNodePropertyAndValue( prop, value ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[PersistedVertex]]

      content.length mustBe t.length

      val tids = t.map( x => x.id() )
      val contentids = content.map( t => t.id )
      contentids.toSet mustBe tids.toSet
    }
  }
}

