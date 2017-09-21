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
      val s = ObjectMatcher.matcher( "string" )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Long" in {
      val l = 3422.toLong.asInstanceOf[java.lang.Long]
      val s = ObjectMatcher.matcher( l )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Character" in {
      val c = 'a'.asInstanceOf[java.lang.Character]
      val s = ObjectMatcher.matcher( c )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Boolean" in {
      val b = "true".toBoolean.asInstanceOf[java.lang.Boolean]
      val s = ObjectMatcher.matcher( b )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Byte" in {
      val b = "127".toByte.asInstanceOf[java.lang.Byte]
      val s = ObjectMatcher.matcher( b )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Integer" in {
      val i = 127.asInstanceOf[java.lang.Integer]
      val s = ObjectMatcher.matcher( i )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Float" in {
      val f = "127".toFloat.asInstanceOf[java.lang.Float]
      val s = ObjectMatcher.matcher( f )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java Double" in {
      val d = "127".toDouble.asInstanceOf[java.lang.Double]
      val s = ObjectMatcher.matcher( d )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher " should {
    "return a string for a java UUID" in {
      val u = UUID.randomUUID().asInstanceOf[java.util.UUID]
      val s = ObjectMatcher.matcher( u )
      s mustBe a[java.lang.String]
    }
  }

  "The Object matcher" should {
    "throw an error if the Object is a list" in {
      val l = List( 1, 2, 3 )
      val s = ObjectMatcher.matcher( l )
      s mustBe "Failed"
    }
  }
}
