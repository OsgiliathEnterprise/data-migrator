package net.osgiliath.migrator.core.metamodel.impl.internal.jpa;

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

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphRequester;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.jgrapht.Graph;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

@Component
public class JpaMetamodelGraphRequester extends MetamodelGraphRequester<JpaMetamodelVertex> {

    private final RawElementProcessor rawElementProcessor;

    public JpaMetamodelGraphRequester(RawElementProcessor rawElementProcessor) {
        this.rawElementProcessor = rawElementProcessor;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<FieldEdge<JpaMetamodelVertex>> getOutboundFieldEdges(JpaMetamodelVertex sourceVertex, Graph<JpaMetamodelVertex, FieldEdge<JpaMetamodelVertex>> graph) {
        return graph.outgoingEdgesOf(sourceVertex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FieldEdge<JpaMetamodelVertex>> getInverseFieldEdge(FieldEdge<JpaMetamodelVertex> fieldEdge, JpaMetamodelVertex targetVertex, Graph<JpaMetamodelVertex, FieldEdge<JpaMetamodelVertex>> graph) {
        Method getterMethod = fieldEdge.relationshipGetter();
        return rawElementProcessor.inverseRelationshipField(getterMethod, targetVertex.getEntityClass()).flatMap(
                f -> getOutboundFieldEdges(targetVertex, graph).stream().filter(e -> e.getFieldName().equals(f.getName())).findAny()
        );
    }

}
