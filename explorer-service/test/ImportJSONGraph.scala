import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.janusgraph.core.JanusGraphFactory

object ImportJSONGraph {

  val graph = JanusGraphFactory.build().set("storage.backend", "inmemory").open()
  val inputStream = getClass.getResourceAsStream("/resources/test-graph-storage.json")
  graph.io(IoCore.graphson()).reader().create().readGraph(inputStream, graph)
  val g = graph.traversal()
}
