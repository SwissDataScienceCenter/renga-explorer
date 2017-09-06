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
