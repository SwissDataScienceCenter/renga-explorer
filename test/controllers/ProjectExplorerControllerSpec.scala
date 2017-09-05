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
import helpers.ImportJSONProjectGraph
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

/**
 * Created by 3C111 on 04.09.2017
 */

class ProjectExplorerControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with BeforeAndAfter {

  // Set the stage
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides( bind[JWTVerifierProvider].to[MockJWTVerifierProvider] )
    .overrides( bind[JanusGraphProvider].to[MockJanusGraphProvider] )
    .overrides( bind[Scope].to[MockScope] )
    .build()

  val projectController: ProjectExplorerController = app.injector.instanceOf[ProjectExplorerController]

  val graph = app.injector.instanceOf[JanusGraphProvider].get
  val g = app.injector.instanceOf[JanusGraphTraversalSourceProvider].get

  val tokenSignerProvider = app.injector.instanceOf[MockTokenSignerProvider]
  val tokenBuilder = JWT.create()
  val token = tokenBuilder.sign( tokenSignerProvider.get )
  val fakerequest = FakeRequest().withToken( token )

  implicit val reads: Reads[PersistedVertex] = PersistedVertexFormat

  before {
    ImportJSONProjectGraph.populateGraph( graph )
  }

  after {
    graph.traversal().V().drop().iterate()
  }

  implicit val ec = ExecutionContext.global

  "The retrieve projects controller" should {
    "return all projects in the graph" in {
      val graphFiles = g.V().has( Constants.TypeKey, "project:project" ).asScala.toList

      val result = projectController.retrieveProjects().apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      ( content.length == graphFiles.length ) mustBe true

    }
  }

  "The retrieve projects by userId controller" should {
    "return all projects of a specific user" in {
      val user = g.V().has( "resource:owner" ).values[String]( "resource:owner" ).limit( 1 ).asScala.toList.head

      val graphList = g.V().has( Constants.TypeKey, "project:project" ).has( "resource:owner", user ).asScala.toList

      val result = projectController.retrieveProjectByUserName( Option( user ) ).apply( fakerequest )
      val content = contentAsJson( result ).as[Seq[PersistedVertex]]

      ( content.length == graphList.length ) mustBe true
    }
  }
  "The project metadata query" should {
    "return the metadata of a projectnode" in {
      val projectId = g.V().has( Constants.TypeKey, "project:project" ).asScala.toList.head.id()

      val graphProjectNode = g.V( projectId ).asScala.toList.head
      val graphProjectName = graphProjectNode.value[String]( "project:project_name" )
      val graphProjectOwner = graphProjectNode.value[String]( "resource:owner" )

      val result = projectController.retrieveProjectMetadata( projectId.toString.toLong ).apply( fakerequest )
      val content = contentAsJson( result ).as[PersistedVertex]

      val contentProjectName = content.properties.get( NamespaceAndName( "project", "project_name" ) ).orNull.values.head.self
      val contentProjectOwner = content.properties.get( NamespaceAndName( "resource", "owner" ) ).orNull.values.head.self

      ( contentProjectName == graphProjectName ) mustBe true
      ( graphProjectOwner == contentProjectOwner ) mustBe true
    }
  }
}
