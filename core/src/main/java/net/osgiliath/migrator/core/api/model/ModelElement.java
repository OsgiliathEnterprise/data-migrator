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
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

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
    private final RawElementProcessor rawEntityHelper;

    /**
     * Constructor.
     *
     * @param entity          The entity.
     * @param rawEntityHelper JPA entity helper.
     */
    public ModelElement(Object entity, RawElementProcessor rawEntityHelper) {
        this.entity = entity;
        this.rawEntityHelper = rawEntityHelper;
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
     * Returns the Raw value(s) corresponding to the entity referenced by the outboundEdge
     *
     * @param sourceMetamodelVertex
     * @return Returns the ModelElement(s) corresponding to the entity referenced by the outboundEdge
     */
    public Object getFieldRawValue(MetamodelVertex sourceMetamodelVertex, String fieldName) {
        return rawEntityHelper.getFieldValue(((JpaMetamodelVertex) sourceMetamodelVertex).getEntityClass(), entity, fieldName);
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
            rawEntityHelper.setFieldValue(((JpaMetamodelVertex) entityClass).getEntityClass(), entity, fieldName, value);
        } else {
            rawEntityHelper.setFieldValue(entity.getClass(), entity, fieldName, value);
        }
    }

    public void setEdgeRawValue(MetamodelVertex metamodelVertex, FieldEdge field, Object value) {
        if (metamodelVertex != null) {
            rawEntityHelper.setFieldValue(((JpaMetamodelVertex) metamodelVertex).getEntityClass(), entity, field.getFieldName(), value);
        } else {
            rawEntityHelper.setFieldValue(entity.getClass(), entity, field.getFieldName(), value);
        }
    }

    /**
     * Returns the Raw value(s) corresponding to the entity referenced by the outboundEdge
     *
     * @param fieldEdge the edge to get the target vertices from
     * @return Returns the ModelElement(s) corresponding to the entity referenced by the outboundEdge
     */
    public Object getEdgeRawValue(FieldEdge fieldEdge) {
        Method getterMethod = fieldEdge.relationshipGetter();
        try {
            return getterMethod.invoke(this.getRawElement());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Object> getId(MetamodelVertex metamodelVertex) {
        if (metamodelVertex != null) {
            return rawEntityHelper.getId(((JpaMetamodelVertex) metamodelVertex).getEntityClass(), getRawElement());
        } else {
            return rawEntityHelper.getId(this.getClass(), getRawElement());
        }
    }

    public String toString() {
        return entity.toString();
    }
}
