package net.osgiliath.migrator.core.api.metamodel.model;

/*-
 * #%L
 * data-migrator-core
 * %%
 * Copyright (C) 2024 Osgiliath Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import net.osgiliath.migrator.core.api.metamodel.RelationshipType;
import org.jgrapht.Graph;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface MetamodelVertex {

    Collection<FieldEdge> getOutboundFieldEdges(Graph<MetamodelVertex, FieldEdge> graph);

    String getTypeName();

    boolean isEntity();

    Method relationshipGetter(FieldEdge fieldEdge);

    /**
     * @return Additional properties to display in the model graph (Key: property name, Value: property value).
     */
    Map<String, Object> getAdditionalModelVertexProperties(Object entity);

    RelationshipType relationshipType(Method getterMethod);

    Optional<FieldEdge> getInverseFieldEdge(FieldEdge fieldEdge, MetamodelVertex targetVertex, Graph<MetamodelVertex, FieldEdge> graph);

    Collection<OutboundEdge> computeOutboundEdges(Graph<MetamodelVertex, FieldEdge> graph);
}
