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

import authorization.{ JWTVerifierProvider, MockJWTVerifierProvider }
import ch.datascience.service.utils.persistence.graph.{ JanusGraphProvider, MockJanusGraphProvider }
import helpers.ImportJSONGraph
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

class StorageExplorerControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {
  // before: population of the graph

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides( bind[JWTVerifierProvider].to[MockJWTVerifierProvider] )
    .overrides( bind[JanusGraphProvider].to[MockJanusGraphProvider] )
    .build()
  val explorerController: StorageExplorerController = app.injector.instanceOf[StorageExplorerController]
  // val traversalSource = jsonGraph.g

  val graph = app.injector.instanceOf[JanusGraphProvider].get
  ImportJSONGraph.populateGraph( graph )

  "The traversal " should {
    "not be empty" in {
      val g = graph.traversal()
      g.V().valueMap().hasNext mustBe true
    }

  }
  //val c = jsonGraph.g.V().valueMap().toList()
  //JanusGraphTraversalSourceProvider
  // graphTraversalSource

  // after: clear the graph

}

/* import scala.collection.JavaConverters._
 val s1 = t.toStream.iterator().asScala
 for (v <- s1) {
   println(v)
 }
*/ 
