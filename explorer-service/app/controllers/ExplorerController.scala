package controllers

import javax.inject.{Inject, Singleton}

import authorization.JWTVerifierProvider
import ch.datascience.graph.Constants
import ch.datascience.graph.elements.persisted.PersistedVertex
import ch.datascience.graph.elements.persisted.json.PersistedVertexFormat
import ch.datascience.graph.naming.NamespaceAndName
import ch.datascience.service.security.ProfileFilterAction
import ch.datascience.service.utils.persistence.graph.{GraphExecutionContextProvider, JanusGraphTraversalSourceProvider}
import ch.datascience.service.utils.persistence.reader.VertexReader
import ch.datascience.service.utils.{ControllerWithBodyParseJson, ControllerWithGraphTraversal}
import org.apache.tinkerpop.gremlin.structure.Vertex
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
  * Created by jeberle on 25.04.17.
  */
@Singleton
class ExplorerController @Inject() (
  config: play.api.Configuration,
  jwtVerifier: JWTVerifierProvider,
  wsclient: WSClient,
  implicit val graphExecutionContextProvider: GraphExecutionContextProvider,
  implicit val janusGraphTraversalSourceProvider: JanusGraphTraversalSourceProvider,
  implicit val vertexReader: VertexReader
) extends Controller
  with ControllerWithBodyParseJson
  with ControllerWithGraphTraversal {


  def bucketList: Action[AnyContent] = ProfileFilterAction(jwtVerifier.get).async { implicit request =>
    val g = graphTraversalSource
    val t = g.V().has(Constants.TypeKey, "resource:bucket")

    val future: Future[Seq[PersistedVertex]] = graphExecutionContext.execute {
      Future.sequence(t.toIterable.map(v =>
        vertexReader.read(v)
      ).toSeq)
    }
    future.map(s => Ok(Json.toJson(s)))

  }

  def fileList(id: Long): Action[AnyContent] = ProfileFilterAction(jwtVerifier.get).async { implicit request =>
    val g = graphTraversalSource
    val t = g.V(Long.box(id)).in("resource:stored_in").has(Constants.TypeKey, "resource:file")

    val future: Future[Seq[PersistedVertex]] = graphExecutionContext.execute {
      Future.sequence(t.toIterable.map(v =>
        vertexReader.read(v)
      ).toSeq)
    }
    future.map(s => Ok(Json.toJson(s)))

  }

  def fileMetadatafromPath(id: Long, path: String): Action[AnyContent] = ProfileFilterAction(jwtVerifier.get).async { implicit request =>
    val g = graphTraversalSource
    val t = g.V().has("resource:filename",path).as("data").out("resource:stored_in").V(Long.box(id)).as("bucket").select[Vertex]("data", "bucket")

    Future.sequence(graphExecutionContext.execute {
      if (t.hasNext) {
        import scala.collection.JavaConverters._
        val jmap: Map[String, Vertex] = t.next().asScala.toMap
        for {
          (key, value) <- jmap
        } yield for {
          vertex <- vertexReader.read(value)
        } yield key -> vertex
      }
      else
        Seq.empty
    }).map(i => Ok(Json.toJson(i.toMap)))
  }

  def fileMetadata(id: Long): Action[AnyContent] = ProfileFilterAction(jwtVerifier.get).async { implicit request =>
    val g = graphTraversalSource
    val t = g.V(Long.box(id)).as("data").out("resource:stored_in").as("bucket").select[Vertex]("data", "bucket")

    Future.sequence(graphExecutionContext.execute {
      if (t.hasNext) {
        import scala.collection.JavaConverters._
        val jmap: Map[String, Vertex] = t.next().asScala.toMap
        for {
          (key, value) <- jmap
        } yield for {
          vertex <- vertexReader.read(value)
        } yield key -> vertex
      }
      else
        Seq.empty
    }).map(i => Ok(Json.toJson(i.toMap)))
  }

  def bucketMetadata(id: Long): Action[AnyContent] = ProfileFilterAction(jwtVerifier.get).async { implicit request =>
    val g = graphTraversalSource
    val t = g.V(Long.box(id))

    val future: Future[Option[PersistedVertex]] = graphExecutionContext.execute {
      if (t.hasNext) {
        val vertex = t.next()
        vertexReader.read(vertex).map(Some.apply)
      }
      else
        Future.successful( None )
    }

    future.map {
      case Some(vertex) =>
        if (vertex.types.contains(NamespaceAndName("resource:bucket")))
          Ok(Json.toJson(vertex)(PersistedVertexFormat))
        else
          NotAcceptable // to differentiate from not found
      case None => NotFound
    }
  }

  private[this] implicit lazy val persistedVertexFormat = PersistedVertexFormat

}
