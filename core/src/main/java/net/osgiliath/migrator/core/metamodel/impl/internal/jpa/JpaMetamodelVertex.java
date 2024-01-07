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

import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.FieldAndTargetType;
import org.jgrapht.Graph;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JpaMetamodelVertex implements MetamodelVertex {

    private final Class<?> metamodelClass;

    private final Class<?> entityClass;
    private final JpaEntityHelper jpaEntityHelper;
    private final JpaMetamodelVertexFactory jpaMetamodelVertexFactory;


    private Map<Graph, Collection<OutboundEdge>> outboundEdges = new HashMap<>();

    public JpaMetamodelVertex(Class<?> metamodelClass, Class<?> entityClass, JpaEntityHelper jpaEntityHelper, JpaMetamodelVertexFactory jpaMetamodelVertexFactory) {
        this.metamodelClass = metamodelClass;
        this.entityClass = entityClass;
        this.jpaEntityHelper = jpaEntityHelper;
        this.jpaMetamodelVertexFactory = jpaMetamodelVertexFactory;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    private Class<?> getMetamodelClass() {
        return metamodelClass;
    }

    public Collection<OutboundEdge> getOutboundEdges(Graph<MetamodelVertex, FieldEdge> graph) {
        if (!outboundEdges.containsKey(graph)) {
            outboundEdges.put(graph, computeOutboundEdges(graph));
        }
        return outboundEdges.get(graph);
    }

    private Collection<OutboundEdge> computeOutboundEdges(Graph<MetamodelVertex, FieldEdge> graph) {
        Collection<OutboundEdge> outboundEdges = new HashSet<>();
        Stream.of(getMetamodelClass().getDeclaredFields()).flatMap(f -> targetTypeOfMetamodelField(f).map(targetType -> new FieldAndTargetType(f, targetType)).stream())
            .flatMap(t ->
                graph.vertexSet().stream().filter(candidateVertex -> ((JpaMetamodelVertex)candidateVertex).getEntityClass().equals(t.getTargetType()))
                    .filter(targetMetamodelVertex -> !jpaEntityHelper.isDerived(getEntityClass(), t.getField().getName()))
                    .map(targetMetamodelVertex -> jpaMetamodelVertexFactory.createMetamodelEdge(new FieldEdge(t.getField()), targetMetamodelVertex))
            ).collect(Collectors.toCollection(() -> outboundEdges));
        return outboundEdges;
    }

    public boolean isEntity() {
        return getEntityClass().isAnnotationPresent(jakarta.persistence.Entity.class);
    }

    @Override
    public Method relationshipGetter(FieldEdge fieldEdge) {
        return jpaEntityHelper.getterMethod(getEntityClass(), fieldEdge.getMetamodelField());
    }

    @Override // TODO Should not be here, but in a model Vertex element wrapper
    public Object getId(Object entity) {
        return jpaEntityHelper.getId(getEntityClass(), entity);
    }

    public Object getFieldValue(Object entity, String attributeName) {
        return jpaEntityHelper.getFieldValue(getEntityClass(), entity, attributeName);
    }

    public void setFieldValue(Object entity, String attributeName, Object value) {
        jpaEntityHelper.setFieldValue(getEntityClass(), entity, attributeName, value);
    }

    @Override
    public Map<String, Object> getAdditionalModelVertexProperties(Object entity) {
        return new HashMap<>();
    }

    @Override
    public String getTypeName() {
        return getEntityClass().getSimpleName();
    }

    public Optional<Field> getMetamodelAttributeWithName(String fieldName) {
        return Arrays.stream(getMetamodelClass().getDeclaredFields()).filter(f -> f.getName().equals(fieldName)).findAny();
    }

    public Optional<Field> getEntityFieldWithName(String fieldName) {
        return Arrays.stream(getEntityClass().getDeclaredFields()).filter(f -> f.getName().equals(fieldName)).findAny();
    }

    private Optional<Type> targetTypeOfMetamodelField(Field f) {
        Type t  = f.getGenericType();
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            Type[] types = pt.getActualTypeArguments();
            if (types.length == 2) {
                return Optional.of(types[1]);
            }
        }
        return Optional.empty();
    }

        @Override
    public String toString() {
        return "ClassVertex{" +
            "metamodelClass=" + metamodelClass.getName() +
            ", entityClass=" + entityClass.getName() +
            '}';
    }
}
