package helpers

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.io.IoCore

object ImportJSONGraph {

  def populateGraph( graph: Graph ): Unit = {
    val inputStream = getClass.getResourceAsStream( "/test-graph-storage.json" )
    graph.io( IoCore.graphson() ).reader().create().readGraph( inputStream, graph )
  }
}
