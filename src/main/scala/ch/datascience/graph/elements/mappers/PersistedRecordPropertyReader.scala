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

package ch.datascience.graph.elements.mappers

import ch.datascience.graph.elements.BoxedValue
import ch.datascience.graph.elements.persistence.Path
import ch.datascience.graph.elements.persistence.impl.ImplPersistedRecordProperty
import org.apache.tinkerpop.gremlin.structure.{Property => GraphProperty}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by johann on 19/05/17.
  */
class PersistedRecordPropertyReader[Key : StringReader](val parent: Path)(implicit kvr: KeyValueReader[Key, BoxedValue])
  extends Reader[GraphProperty[java.lang.Object], ImplPersistedRecordProperty[Key, BoxedValue]] {

  def read(property: GraphProperty[java.lang.Object])(implicit ec: ExecutionContext): Future[ImplPersistedRecordProperty[Key, BoxedValue]] = {
    for {
      key <- implicitly[StringReader[Key]].read(property.key())
      value <- kvr.read(key -> property.value())
    } yield {
      ImplPersistedRecordProperty(parent, key, value)
    }
  }

}
