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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

/**
 * Created by 3C111 on 26.09.2017
 */

class HelperSpec extends PlaySpec with MockitoSugar {

  "The Object matcher " should {
    "return a string for a String" in {
      val s = ObjectMatcher.objectToString( "string" )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Long" in {
      val l = 3422.toLong.asInstanceOf[java.lang.Long]
      val s = ObjectMatcher.objectToString( l )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Character" in {
      val c = 'a'.asInstanceOf[java.lang.Character]
      val s = ObjectMatcher.objectToString( c )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Boolean" in {
      val b = "true".toBoolean.asInstanceOf[java.lang.Boolean]
      val s = ObjectMatcher.objectToString( b )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Byte" in {
      val b = "127".toByte.asInstanceOf[java.lang.Byte]
      val s = ObjectMatcher.objectToString( b )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Integer" in {
      val i = 127.asInstanceOf[java.lang.Integer]
      val s = ObjectMatcher.objectToString( i )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Float" in {
      val f = "127".toFloat.asInstanceOf[java.lang.Float]
      val s = ObjectMatcher.objectToString( f )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Double" in {
      val d = "127".toDouble.asInstanceOf[java.lang.Double]
      val s = ObjectMatcher.objectToString( d )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java UUID" in {
      val u = UUID.randomUUID()
      val s = ObjectMatcher.objectToString( u )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher" should {
    "throw an error if the Object is a list" in {
      val l = List( 1, 2, 3 )
      a[UnsupportedOperationException] must be thrownBy ObjectMatcher.objectToString( l )

    }
  }

  "The Object matcher" should {
    "return a string if a value with string type is given" in {
      val input = "abc"
      val itype = "type"
      val s2 = ObjectMatcher.stringToGivenType( input, itype )
      s2 mustBe a[java.lang.String]
    }
  }

  "The Object matcher" should {
    "return a long if a value with long type is given" in {
      val input = "4510"
      val itype = 12344.toLong
      val l = ObjectMatcher.stringToGivenType( input, itype )
      l mustBe a[java.lang.Long]
    }
  }

  "The Object matcher" should {
    "return a char if a value with char type is given" in {
      val input = "a"
      val itype = 'a'
      val c = ObjectMatcher.stringToGivenType( input, itype )
      c mustBe a[java.lang.Character]
    }
  }

  "The Object matcher" should {
    "return a boolean if a value with boolean type is given" in {
      val input = "false"
      val itype = true
      val b = ObjectMatcher.stringToGivenType( input, itype )
      b mustBe a[java.lang.Boolean]
    }
  }

  "The Object matcher" should {
    "return a byte if a value with byte type is given" in {
      val input = "0"
      val itype = "127".toByte.asInstanceOf[java.lang.Byte]
      val b = ObjectMatcher.stringToGivenType( input, itype )
      b mustBe a[java.lang.Byte]
    }
  }

  "The Object matcher" should {
    "return a short if a value with short type is given" in {
      val input = "0"
      val itype = "127".toShort
      val s = ObjectMatcher.stringToGivenType( input, itype )
      s mustBe a[java.lang.Short]
    }
  }

  "The Object matcher" should {
    "return an int if a value with int type is given" in {
      val input = "0"
      val itype = "127".toInt
      val i = ObjectMatcher.stringToGivenType( input, itype )
      i mustBe a[java.lang.Integer]
    }
  }

  "The Object matcher" should {
    "return an float if a value with float type is given" in {
      val input = "0"
      val itype = "127".toFloat
      val f = ObjectMatcher.stringToGivenType( input, itype )
      f mustBe a[java.lang.Float]
    }
  }

  "The Object matcher" should {
    "return an double if a value with double type is given" in {
      val input = "0"
      val itype = "127".toDouble
      val d = ObjectMatcher.stringToGivenType( input, itype )
      d mustBe a[java.lang.Double]
    }
  }

  "The Object matcher" should {
    "return an uuid if a value with uuid type is given" in {
      val input = UUID.randomUUID().toString
      val itype = UUID.randomUUID()
      val d = ObjectMatcher.stringToGivenType( input, itype )
      d mustBe a[java.util.UUID]
    }
  }
  "The Object matcher" should {
    "return fail if a non supported type is given" in {
      val input = "123"
      val itype = List()
      a[UnsupportedOperationException] must be thrownBy ObjectMatcher.stringToGivenType( input, itype )
    }
  }
}
