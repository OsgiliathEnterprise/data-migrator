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

import net.osgiliath.migrator.core.api.metamodel.MetamodelVertexFactory;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphBuilder;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.metamodel.impl.model.FieldAndTargetType;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.jgrapht.Graph;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JpaMetamodelGraphBuilder extends MetamodelGraphBuilder<JpaMetamodelVertex> {

    private final RawElementProcessor rawElementProcessor;
    private final MetamodelVertexFactory<JpaMetamodelVertex> metamodelVertexFactory;

    public JpaMetamodelGraphBuilder(MetamodelVertexFactory<JpaMetamodelVertex> metamodelVertexFactory, RawElementProcessor rawElementProcessor) {
        super(metamodelVertexFactory);
        this.rawElementProcessor = rawElementProcessor;
        this.metamodelVertexFactory = metamodelVertexFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<OutboundEdge<JpaMetamodelVertex>> computeOutboundEdges(JpaMetamodelVertex sourceVertex, Graph<JpaMetamodelVertex, FieldEdge<JpaMetamodelVertex>> graph) {
        return Stream.of(sourceVertex.getMetamodelClass().getDeclaredFields())
                .flatMap(f -> sourceVertex.targetTypeOfMetamodelField(f)
                        .map(targetType -> new FieldAndTargetType(f, targetType)).stream())
                .flatMap(t ->
                        graph.vertexSet().stream().filter(candidateVertex -> candidateVertex.getEntityClass().equals(t.targetType()))
                                .filter(targetMetamodelVertex -> !rawElementProcessor.isDerived(sourceVertex.getEntityClass(), t.field().getName()))
                                .map(targetMetamodelVertex ->
                                        metamodelVertexFactory.createOutboundEdge(metamodelVertexFactory.createFieldEdge(t.field()), targetMetamodelVertex)
                                )
                ).collect(Collectors.toSet());
    }

    @Override
    protected boolean isEntity(JpaMetamodelVertex metamodelVertex) {
        return metamodelVertex.getEntityClass().isAnnotationPresent(jakarta.persistence.Entity.class);
    }
}
