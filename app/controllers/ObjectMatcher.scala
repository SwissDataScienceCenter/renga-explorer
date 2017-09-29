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

import java.util.UUID

/**
 * Created by 3C111 on 26.09.2017
 */

object ObjectMatcher {

  def objectToString( x: Object ): String = {
    x match {

      case item: java.lang.String    => item
      case item: java.lang.Long      => item.toString
      case item: java.lang.Character => item.toString
      case item: java.lang.Boolean   => item.toString
      case item: java.lang.Byte      => item.toString
      case item: java.lang.Short     => item.toString
      case item: java.lang.Integer   => item.toString
      case item: java.lang.Float     => item.toString
      case item: java.lang.Double    => item.toString
      case item: java.util.UUID      => item.toString
      case _                         => throw new UnsupportedOperationException( "Type not supported" )
    }
  }

  def stringToGivenType( value: String, c: Any ): Any = {
    c match {

      case item: java.lang.String    => value
      case item: java.lang.Long      => value.toLong
      case item: java.lang.Character => value.head
      case item: java.lang.Boolean   => value.toBoolean
      case item: java.lang.Byte      => value.toByte
      case item: java.lang.Short     => value.toShort
      case item: java.lang.Integer   => value.toInt
      case item: java.lang.Float     => value.toFloat
      case item: java.lang.Double    => value.toDouble
      case item: java.util.UUID      => UUID.fromString( value )
      case _                         => throw new UnsupportedOperationException( "Type not supported" )
    }

  }
}
