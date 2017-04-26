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

package models

import java.util.UUID

import ch.datascience.typesystem.model._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by johann on 25/04/17.
  */
package object json {

  implicit lazy val uuidReads: Reads[UUID] = UUIDMappers.uuidReads
  implicit lazy val uuidWrites: Writes[UUID] = UUIDMappers.uuidWrites
  lazy val notUUIDReads: Reads[String] = UUIDMappers.notUUidReads

  implicit lazy val dataTypeReads: Reads[DataType] = DataTypeMappers.dataTypeReads
  implicit lazy val dataTypeWrites: Writes[DataType] = DataTypeMappers.dataTypeWrites

  implicit lazy val cardinalityReads: Reads[Cardinality] = CardinalityMappers.cardinalityReads
  implicit lazy val cardinalityWrites: Writes[Cardinality] = CardinalityMappers.cardinalityWrites

  implicit lazy val graphDomainReads: Reads[GraphDomain] = GraphDomainMappers.graphDomainReads
  implicit lazy val graphDomainWrites: Writes[GraphDomain] = GraphDomainMappers.graphDomainWrites

  implicit lazy val propertyKeyWrites: Writes[PropertyKey] = PropertyKeyMappers.propertyKeyWrites

  implicit lazy val graphObjectReads: Reads[GraphObject] = GraphObjectMappers.graphObjectReads
  implicit lazy val graphObjectWrites: Writes[GraphObject] = GraphObjectMappers.graphObjectWrites


  lazy val namespaceReads: Reads[String] = JsPath.read[String](Reads.pattern("([^:]*)".r) <~ notUUIDReads)
  lazy val nameReads: Reads[String] = JsPath.read[String](Reads.pattern("([^:]+)".r) <~ notUUIDReads)

}
