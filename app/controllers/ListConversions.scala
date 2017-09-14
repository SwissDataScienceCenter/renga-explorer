package helpers

import java.util

import scala.collection.JavaConverters._

object ListConversions {

  def ensureList[A]( obj: java.lang.Object ): Seq[A] = obj match {
    case list: java.util.List[_] => list.asScala.toSeq.map( _.asInstanceOf[A] )
    case _                       => Seq( obj.asInstanceOf[A] )

  }

  def flatMapToList( objectList: List[Object] ) = {
    objectList.flatMap {
      case i if i.isInstanceOf[util.List[Any]] => i.asInstanceOf[util.List[Any]].asScala.toList
      case i                                   => List( i.asInstanceOf[Any] )
    }
  }
}

