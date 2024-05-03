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

import net.osgiliath.migrator.core.api.metamodel.RelationshipType;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;
import net.osgiliath.migrator.core.metamodel.impl.model.FieldAndTargetType;
import net.osgiliath.migrator.core.rawelement.jpa.JpaEntityProcessor;
import org.jgrapht.Graph;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JPA implementation of a metamodel vertex.
 */
public class JpaMetamodelVertex implements MetamodelVertex {

    /**
     * The JPA metamodel class.
     */
    private final Class<?> metamodelClass;

    /**
     * The entity class.
     */
    private final Class<?> entityClass;
    /**
     * JPA entity helper.
     */
    private final JpaEntityProcessor jpaEntityHelper;
    /**
     * JPA metamodel vertex factory.
     */
    private final JpaMetamodelVertexFactory jpaMetamodelVertexFactory;

    /**
     * Constructor.
     *
     * @param metamodelClass            The JPA metamodel class.
     * @param entityClass               The entity class.
     * @param jpaEntityHelper           JPA entity helper.
     * @param jpaMetamodelVertexFactory JPA metamodel vertex factory.
     */
    public JpaMetamodelVertex(Class<?> metamodelClass, Class<?> entityClass, JpaEntityProcessor jpaEntityHelper, JpaMetamodelVertexFactory jpaMetamodelVertexFactory) {
        this.metamodelClass = metamodelClass;
        this.entityClass = entityClass;
        this.jpaEntityHelper = jpaEntityHelper;
        this.jpaMetamodelVertexFactory = jpaMetamodelVertexFactory;
    }

    /**
     * Get the entity class.
     *
     * @return The entity class.
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * Get the metamodel class.
     *
     * @return The metamodel class.
     */
    private Class<?> getMetamodelClass() {
        return metamodelClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<FieldEdge> getOutboundFieldEdges(Graph<MetamodelVertex, FieldEdge> graph) {
        return graph.outgoingEdgesOf(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<OutboundEdge> computeOutboundEdges(Graph<MetamodelVertex, FieldEdge> graph) {
        return Stream.of(getMetamodelClass().getDeclaredFields())
                .flatMap(f -> targetTypeOfMetamodelField(f)
                        .map(targetType -> new FieldAndTargetType(f, targetType)).stream())
                .flatMap(t ->
                        graph.vertexSet().stream().filter(candidateVertex -> ((JpaMetamodelVertex) candidateVertex).getEntityClass().equals(t.targetType()))
                                .filter(targetMetamodelVertex -> !jpaEntityHelper.isDerived(getEntityClass(), t.field().getName()))
                                .map(targetMetamodelVertex ->
                                        jpaMetamodelVertexFactory.createOutboundEdge(jpaMetamodelVertexFactory.createFieldEdge(t.field()), targetMetamodelVertex)
                                )
                ).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEntity() {
        return getEntityClass().isAnnotationPresent(jakarta.persistence.Entity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getAdditionalModelVertexProperties(Object entity) {
        return new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RelationshipType relationshipType(Method getterMethod) {
        return jpaEntityHelper.relationshipType(getterMethod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FieldEdge> getInverseFieldEdge(FieldEdge fieldEdge, MetamodelVertex targetVertex, Graph<MetamodelVertex, FieldEdge> graph) {
        Method getterMethod = fieldEdge.relationshipGetter();
        return jpaEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex) targetVertex).getEntityClass()).flatMap(
                f -> targetVertex.getOutboundFieldEdges(graph).stream().filter(e -> e.getFieldName().equals(f.getName())).findAny()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeName() {
        return getEntityClass().getSimpleName();
    }

    /**
     * Get the Java Type of the target of a field.
     */
    private Optional<Type> targetTypeOfMetamodelField(Field f) {
        Type t = f.getGenericType();
        if (t instanceof ParameterizedType pt) {
            Type[] types = pt.getActualTypeArguments();
            if (types.length == 2) {
                return Optional.of(types[1]);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ClassVertex{" +
                "metamodelClass=" + metamodelClass.getName() +
                ", entityClass=" + entityClass.getName() +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    public Optional<String> getPrimaryKeyField() {
        return jpaEntityHelper.getPrimaryKeyFieldName(this.getEntityClass());
    }
}
