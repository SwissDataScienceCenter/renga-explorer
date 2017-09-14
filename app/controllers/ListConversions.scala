package helpers

import scala.collection.JavaConverters._

object ListConversions {

  def ensureList[A]( obj: java.lang.Object ): Seq[A] = obj match {
    case list: java.util.List[_] => list.asScala.toSeq.map( _.asInstanceOf[A] )
    case _                       => Seq( obj.asInstanceOf[A] )
  }
}

