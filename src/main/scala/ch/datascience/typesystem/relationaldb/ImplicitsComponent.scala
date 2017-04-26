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

package ch.datascience.typesystem.relationaldb

import java.time.Instant

import ch.datascience.typesystem.model.{Cardinality, DataType, EntityState, EntityType}
import org.janusgraph.core.Multiplicity

/**
  * Created by johann on 13/04/17.
  */
trait ImplicitsComponent { this: JdbcProfileComponent =>

  import profile.api._

  implicit val entityTypeColumnType: BaseColumnType[EntityType] =
    MappedColumnType.base[EntityType, String](_.name, EntityType.valueOf)

  implicit val dataTypeColumnType: BaseColumnType[DataType] =
    MappedColumnType.base[DataType, String](_.name, DataType.valueOf)

  implicit val cardinalityColumnType: BaseColumnType[Cardinality] =
    MappedColumnType.base[Cardinality, String](_.name, Cardinality.valueOf)

  implicit val multiplicityColumnType: BaseColumnType[Multiplicity] =
    MappedColumnType.base[Multiplicity, String](_.name(), Multiplicity.valueOf)

  implicit val entityStateColumnType: BaseColumnType[EntityState] =
    MappedColumnType.base[EntityState, String](_.name, EntityState.valueOf)

  implicit val customTimestampColumnType: BaseColumnType[Instant] =
    MappedColumnType.base[Instant, Long](_.toEpochMilli, Instant.ofEpochMilli)

}
