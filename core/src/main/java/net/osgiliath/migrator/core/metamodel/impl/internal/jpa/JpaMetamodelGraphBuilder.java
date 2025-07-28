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
import net.osgiliath.migrator.core.rawelement.jpa.JpaRelationshipProcessor;
import org.jgrapht.Graph;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class JpaMetamodelGraphBuilder extends MetamodelGraphBuilder<JpaMetamodelVertex> {

    private final JpaRelationshipProcessor relationshipProcessor;
    private final MetamodelVertexFactory<JpaMetamodelVertex> metamodelVertexFactory;

    public JpaMetamodelGraphBuilder(MetamodelVertexFactory<JpaMetamodelVertex> metamodelVertexFactory, JpaRelationshipProcessor relationshipProcessor) {
        super(metamodelVertexFactory);
        this.relationshipProcessor = relationshipProcessor;
        this.metamodelVertexFactory = metamodelVertexFactory;
    }

    private record VertexMetamodelClassAndEntityClass(FieldAndTargetType fieldAndTargetType, Class<?> entityClass,
                                                      Graph<JpaMetamodelVertex, FieldEdge<JpaMetamodelVertex>> graph) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Stream<OutboundEdge<JpaMetamodelVertex>> computeOutboundEdges(JpaMetamodelVertex sourceVertex, Graph<JpaMetamodelVertex, FieldEdge<JpaMetamodelVertex>> graph) {
        record VertexMetamodelClassDeclaredFieldsAndEntityClass(Field[] fields, Class<?> entityClass,
                                                                Graph<JpaMetamodelVertex, FieldEdge<JpaMetamodelVertex>> graph) {
        }

        record VertexMetamodelClassDeclaredFieldAndEntityClass(Field field, Class<?> entityClass,
                                                               Graph<JpaMetamodelVertex, FieldEdge<JpaMetamodelVertex>> graph) {
        }

        record VertexMetamodelClassAndEntityClass(Class<?> metamodelClass, Class<?> entityClass,
                                                  Graph<JpaMetamodelVertex, FieldEdge<JpaMetamodelVertex>> graph) {
        }

        VertexMetamodelClassAndEntityClass streamInput = new VertexMetamodelClassAndEntityClass(sourceVertex.metamodelClass(), sourceVertex.entityClass(), graph);
        return Stream
                .of(streamInput)
                .map((VertexMetamodelClassAndEntityClass r) -> new VertexMetamodelClassDeclaredFieldsAndEntityClass(r.metamodelClass().getDeclaredFields(), r.entityClass(), r.graph()))
                .flatMap((VertexMetamodelClassDeclaredFieldsAndEntityClass r) -> Arrays.stream(r.fields()).map(f -> new VertexMetamodelClassDeclaredFieldAndEntityClass(f, r.entityClass(), r.graph())))
                .flatMap(f -> targetTypeOfMetamodelField(f.field(), f.entityClass(), f.graph()).stream())
                .flatMap(t -> t.graph().vertexSet().stream().filter(candidateVertex -> candidateVertex.entityClass().equals(t.fieldAndTargetType().targetType()))
                        .filter(targetMetamodelVertex -> !relationshipProcessor.isFkIgnored(t.entityClass(), t.fieldAndTargetType().field().getName()))
                        .filter(targetMetamodelVertex -> !relationshipProcessor.isDerived(t.entityClass(), t.fieldAndTargetType().field().getName()))
                        .map(targetMetamodelVertex ->
                                metamodelVertexFactory.createOutboundEdge(metamodelVertexFactory.createFieldEdge(t.fieldAndTargetType().field()), targetMetamodelVertex)
                        ));
    }

    @Override
    protected boolean isEntity(JpaMetamodelVertex metamodelVertex) {
        return metamodelVertex.entityClass().isAnnotationPresent(jakarta.persistence.Entity.class);
    }

    /**
     * Get the Java Type of the target of a field.
     */
    private Optional<VertexMetamodelClassAndEntityClass> targetTypeOfMetamodelField(Field f, Class<?> entityClass, Graph<JpaMetamodelVertex, FieldEdge<JpaMetamodelVertex>> graph) {
        Type t = f.getGenericType();
        if (t instanceof ParameterizedType pt) {
            Type[] types = pt.getActualTypeArguments();
            if (types.length == 2) {
                return Optional.of(new VertexMetamodelClassAndEntityClass(new FieldAndTargetType(f, types[1]), entityClass, graph));
            }
        }
        return Optional.empty();
    }
}
