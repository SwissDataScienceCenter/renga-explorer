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
import ch.datascience.service.utils.persistence.graph.{ JanusGraphProvider, JanusGraphTraversalSourceProvider }
import ch.datascience.service.utils.persistence.scope.Scope
import ch.datascience.test.security.FakeRequestWithToken._
import ch.datascience.test.utils.persistence.graph.MockJanusGraphProvider
import ch.datascience.test.utils.persistence.scope.MockScope
import com.auth0.jwt.JWT
import helpers.ImportJSONGraph
import helpers.ListConversions.ensureList
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{ JsObject, Reads }
import play.api.test.FakeRequest
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop.gremlin.structure.{ Edge, Vertex }
import play.api.test.Helpers._

import scala.collection.JavaConverters._

class LineageExplorerControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with BeforeAndAfter {

  // Set the stage
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides( bind[JWTVerifierProvider].to[MockJWTVerifierProvider] )
    .overrides( bind[JanusGraphProvider].to[MockJanusGraphProvider] )
    .overrides( bind[Scope].to[MockScope] )
    .build()

  val lineageController: LineageExplorerController = app.injector.instanceOf[LineageExplorerController]

  val graph = app.injector.instanceOf[JanusGraphProvider].get
  val g = app.injector.instanceOf[JanusGraphTraversalSourceProvider].get

  val tokenSignerProvider = app.injector.instanceOf[MockTokenSignerProvider]
  val tokenBuilder = JWT.create()
  val token = tokenBuilder.sign( tokenSignerProvider.get )
  val fakerequest = FakeRequest().withToken( token )

  implicit val reads: Reads[PersistedVertex] = PersistedVertexFormat

  before {
    ImportJSONGraph.lineageGraph( graph )
  }

  after {
    graph.traversal().V().drop().iterate()
  }

  private[this] implicit lazy val persistedEdgeFormat = PersistedEdgeFormat

  "The lineage from context controller" should {
    "return the full lineage tree from a context node" in {
      val deployerid = g.V().has( Constants.TypeKey, "deployer:context" ).asScala.toList.head.id
      val nodes = g.V( deployerid ).repeat( __.bothE( "deployer:launch", "resource:create", "resource:write", "resource:read" ).dedup().as( "edge" ).otherV().as( "node" ) ).emit().simplePath().select[java.lang.Object]( "edge", "node" )

      val c = ( for ( x <- nodes.asScala.toList ) yield x.asScala.toMap.get( "edge" ).toList ).flatten[Object]
      val s = c.flatMap( ensureList ).toSet

      val result = lineageController.lineageFromContext( deployerid.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[JsObject]]

      content.length mustBe s.toList.length

    }
  }
  "The lineage from context controller" should {
    "return an empty list if the id of the node is not a deployernode" in {
      val result = lineageController.lineageFromContext( 0L ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[JsObject]]

      content.length mustBe 0

    }
  }

  "The lineage from file controller" should {
    "return the lineage from a file " in {

      val fileId = g.V().has( Constants.TypeKey, "resource:file" ).asScala.toList.head.id
      val nodes = g.V( fileId ).inE( "resource:version_of" ).otherV().as( "node" ).repeat( __.bothE( "resource:create", "resource:write", "resource:read", "deployer:launch" ).dedup().as( "edge" ).otherV().as( "node" ) ).emit().simplePath().select[java.lang.Object]( "edge", "node" ).asScala.toList

      val t = ( for ( x <- nodes ) yield x.asScala.toMap.get( "edge" ).toList ).flatten[Object]
      val s = t.flatMap( ensureList ).toSet

      val result = lineageController.lineageFromFile( fileId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[JsObject]]

      content.length mustBe s.toList.length
    }
  }

  "The lineage from file controller" should {
    "return an empty list if the filenode does not exist" in {
      val result = lineageController.lineageFromContext( 0L ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[JsObject]]

      content.length mustBe 0

    }
  }

  "The project lineage query" should {
    "return all associated files and buckets, and execution and context nodes" in {
      graph.traversal().V().drop().iterate()
      ImportJSONGraph.projectGraph( graph )

      val projectId = g.V().has( Constants.TypeKey, "project:project" ).asScala.toList.head.id()

      val t = g.V( projectId ).repeat( __.bothE( "deployer:launch", "project:is_part_of" ).dedup().as( "edge" ).otherV().as( "node" ) ).emit().simplePath().select[java.lang.Object]( "edge", "node" )

      val result = lineageController.retrieveProjectLineage( projectId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[List[JsObject]]
      content.isEmpty mustBe false
    }
  }
}
