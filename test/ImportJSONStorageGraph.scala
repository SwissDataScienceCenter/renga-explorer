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

package helpers

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.io.IoCore

object ImportJSONStorageGraph {

  def populateGraph( graph: Graph ): Unit = {
    val inputStream = getClass.getResourceAsStream( "/test-graph-storage.json" )
    graph.io( IoCore.graphson() ).reader().create().readGraph( inputStream, graph )
  }
}

object ImportJSONLineageGraph {

  def populateGraph( graph: Graph ): Unit = {
    val inputStream = getClass.getResourceAsStream( "/test-lineage.json" )
    graph.io( IoCore.graphson() ).reader().create().readGraph( inputStream, graph )
  }
}

object ImportJSONProjectGraph {

  def populateGraph( graph: Graph ): Unit = {
    val inputStream = getClass.getResourceAsStream( "/test-project.json" )
    graph.io( IoCore.graphson() ).reader().create().readGraph( inputStream, graph )
  }
}

object ImportJSONStorageNoBucketsGraph {

  def populateGraph( graph: Graph ): Unit = {
    val inputStream = getClass.getResourceAsStream( "/test-nobuckets.json" )
    graph.io( IoCore.graphson() ).reader().create().readGraph( inputStream, graph )
  }
}

object ImportJSONStorageNoFilesGraph {

  def populateGraph( graph: Graph ): Unit = {
    val inputStream = getClass.getResourceAsStream( "/test-nofiles.json" )
    graph.io( IoCore.graphson() ).reader().create().readGraph( inputStream, graph )
  }
}
