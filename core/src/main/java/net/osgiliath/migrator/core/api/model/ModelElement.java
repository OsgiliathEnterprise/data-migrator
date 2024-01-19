package net.osgiliath.migrator.core.api.model;

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
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A vertex in the model graph.
 */
public class ModelElement {

    /**
     * The entity.
     */
    private Object entity;
    /**
     * JPA entity helper.
     */
    private final JpaEntityHelper jpaEntityHelper;

    /**
     * Constructor.
     *
     * @param entity          The entity.
     * @param jpaEntityHelper JPA entity helper.
     */
    public ModelElement(Object entity, JpaEntityHelper jpaEntityHelper) {
        this.entity = entity;
        this.jpaEntityHelper = jpaEntityHelper;
    }


    /**
     * Returns the entity that this vertex represents.
     *
     * @return Returns the entity that this vertex represents.
     */
    public Object getEntity() {
        return entity;
    }

    /**
     * Sets the entity that this vertex represents.
     *
     * @param entity The entity that this vertex represents.
     */
    public void setEntity(Object entity) {
        this.entity = entity;
    }

    /**
     * Sets a value on the underlying element
     *
     * @param entityClass The entity class (can be null)
     * @param fieldName   The field name
     * @param value       The value to set
     */
    public void setFieldRawValue(MetamodelVertex entityClass, String fieldName, Object value) {
        if (entityClass != null) {
            jpaEntityHelper.setFieldValue(((JpaMetamodelVertex) entityClass).getEntityClass(), entity, fieldName, value);
        } else {
            jpaEntityHelper.setFieldValue(entity.getClass(), entity, fieldName, value);
        }
    }

    public void setEdgeRawValue(MetamodelVertex metamodelVertex, FieldEdge field, Object value) {
        if (metamodelVertex != null) {
            jpaEntityHelper.setFieldValue(((JpaMetamodelVertex) metamodelVertex).getEntityClass(), entity, field.getFieldName(), value);
        } else {
            jpaEntityHelper.setFieldValue(entity.getClass(), entity, field.getFieldName(), value);
        }
    }

    public void setEdgeValue(FieldEdge field, ModelElement value) {
        if (field.getSource() != null) {
            jpaEntityHelper.setFieldValue(((JpaMetamodelVertex) field.getSource()).getEntityClass(), entity, field.getFieldName(), value.getEntity());
        } else {
            jpaEntityHelper.setFieldValue(entity.getClass(), entity, field.getFieldName(), value.getEntity());
        }
    }

    public void setEdgeValues(FieldEdge field, Collection<ModelElement> value) {
        if (field.getSource() != null) {
            jpaEntityHelper.setFieldValue(((JpaMetamodelVertex) field.getSource()).getEntityClass(), entity, field.getFieldName(), value.stream().map(ModelElement::getEntity).collect(Collectors.toSet()));
        } else {
            jpaEntityHelper.setFieldValue(entity.getClass(), entity, field.getFieldName(), value.stream().map(ModelElement::getEntity).collect(Collectors.toSet()));
        }
    }

    public String toString() {
        return entity.toString();
    }

    /**
     * Returns the Raw value(s) corresponding to the entity referenced by the fieldEdge
     *
     * @param fieldEdge the edge to get the target vertices from
     * @return Returns the ModelElement(s) corresponding to the entity referenced by the fieldEdge
     */
    public Object getEdgeRawValue(FieldEdge fieldEdge) {
        Method getterMethod = fieldEdge.relationshipGetter();
        try {
            return getterMethod.invoke(this.getEntity());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Raw value(s) corresponding to the entity referenced by the fieldEdge
     *
     * @param sourceMetamodelVertex
     * @return Returns the ModelElement(s) corresponding to the entity referenced by the fieldEdge
     */
    public Object getFieldRawValue(MetamodelVertex sourceMetamodelVertex, String fieldName) {
        return jpaEntityHelper.getFieldValue(((JpaMetamodelVertex) sourceMetamodelVertex).getEntityClass(), entity, fieldName);
    }

    /**
     * get the target vertex or vertices corresponding to the entity referenced by the fieldEdge
     *
     * @param fieldEdge  the edge to get the target vertices from
     * @param modelGraph the model graph
     * @return the target Vertex or Vertices corresponding to the entities referenced by the fieldEdge
     */
    public Optional<Object> getEdgeValueFromModelElementRelationShip(FieldEdge fieldEdge, GraphTraversalSource modelGraph) {
        Method getterMethod = fieldEdge.relationshipGetter();
        MetamodelVertex targetVertex = fieldEdge.getTarget();
        try {
            Object res = getterMethod.invoke(this.getEntity());
            if (res instanceof Collection) {
                return Optional.of(((Collection) res).stream()
                        .flatMap(entity -> jpaEntityHelper.getId(((JpaMetamodelVertex) targetVertex).getEntityClass(), entity).stream())
                        .flatMap(id -> modelGraph.V().hasLabel(targetVertex.getTypeName())
                                .has(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID, id).toStream()).collect(Collectors.toSet()));
            } else if (res != null) {
                return jpaEntityHelper.getId(((JpaMetamodelVertex) targetVertex).getEntityClass(), res)
                        .map(id -> modelGraph.V().hasLabel(targetVertex.getTypeName())
                                .has(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID, id).next());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public Optional<Object> getId(MetamodelVertex metamodelVertex) {
        if (metamodelVertex != null) {
            return jpaEntityHelper.getId(((JpaMetamodelVertex) metamodelVertex).getEntityClass(), getEntity());
        } else {
            return jpaEntityHelper.getId(this.getClass(), getEntity());
        }
    }
}
