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
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

/**
 * A vertex in the model graph.
 */
public class ModelElement {

    private static final Logger log = LoggerFactory.getLogger(ModelElement.class);
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
    public Object getRawElement() {
        return entity;
    }

    /**
     * Sets the entity that this vertex represents.
     *
     * @param entity The entity that this vertex represents.
     */
    public void setRawElement(Object entity) {
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
            return getterMethod.invoke(this.getRawElement());
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

    public Optional<Object> getId(MetamodelVertex metamodelVertex) {
        if (metamodelVertex != null) {
            return jpaEntityHelper.getId(((JpaMetamodelVertex) metamodelVertex).getEntityClass(), getRawElement());
        } else {
            return jpaEntityHelper.getId(this.getClass(), getRawElement());
        }
    }

    public void removeEdgeValueFromModelElementRelationShip(FieldEdge fieldEdge, ModelElement targetModelElement, Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        Object targetValue = getEdgeRawValue(fieldEdge);
        if (targetValue instanceof Collection) {
            Collection targetValues = (Collection) targetValue;
            targetValues.remove(targetModelElement.getRawElement());
            setEdgeRawValue(fieldEdge.getSource(), fieldEdge, targetValue);
        } else {
            setEdgeRawValue(fieldEdge.getSource(), fieldEdge, null);
        }
        Method getterMethod = fieldEdge.relationshipGetter();
        Optional<Field> inverseFieldOpt = jpaEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex) fieldEdge.getTarget()).getEntityClass());
        inverseFieldOpt.ifPresent(
                inverseField -> {
                    Object inverseValue = jpaEntityHelper.getFieldValue(((JpaMetamodelVertex) fieldEdge.getTarget()).getEntityClass(), targetModelElement.getRawElement(), inverseField.getName());
                    if (inverseValue instanceof Collection) {
                        Collection inverseValues = (Collection) inverseValue;
                        inverseValues.remove(this.getRawElement());
                        jpaEntityHelper.setFieldValue(((JpaMetamodelVertex) fieldEdge.getTarget()).getEntityClass(), targetModelElement.getRawElement(), inverseField.getName(), inverseValues);
                    } else {
                        jpaEntityHelper.setFieldValue(((JpaMetamodelVertex) fieldEdge.getTarget()).getEntityClass(), targetModelElement.getRawElement(), inverseField.getName(), null);
                    }
                }
        );

    }
}
