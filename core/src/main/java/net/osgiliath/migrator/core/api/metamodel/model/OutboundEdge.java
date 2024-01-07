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
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import jakarta.persistence.Persistence;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class OutboundEdge {

    private final FieldEdge fieldEdge;
    private final MetamodelVertex targetVertex;
    private final JpaEntityHelper hibernateEntityHelper;

    public OutboundEdge(FieldEdge fieldEdge, MetamodelVertex targetVertex, JpaEntityHelper hibernateEntityHelper) {
        this.fieldEdge = fieldEdge;
        this.targetVertex = targetVertex;
        this.hibernateEntityHelper = hibernateEntityHelper;
    }

    public MetamodelVertex getTargetVertex() {
        return targetVertex;
    }

    public FieldEdge getFieldEdge() {
        return fieldEdge;
    }

    public void setEdgeBetweenEntities(MetamodelVertex sourceMetamodelVertex, Object sourceEntity, Object targetEntity) throws InvocationTargetException, IllegalAccessException {
        Field field = fieldEdge.getMetamodelField();
        Method getterMethod = hibernateEntityHelper.getterMethod(((JpaMetamodelVertex)sourceMetamodelVertex).getEntityClass(), field);
        RelationshipType relationshipType = hibernateEntityHelper.relationshipType(getterMethod);
        switch (relationshipType) {
            case ONE_TO_ONE -> {
                hibernateEntityHelper.setFieldValue(((JpaMetamodelVertex)sourceMetamodelVertex).getEntityClass(), sourceEntity, field, targetEntity);
                hibernateEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex)targetVertex).getEntityClass()).ifPresent(inverseField -> {
                    hibernateEntityHelper.setFieldValue(((JpaMetamodelVertex)targetVertex).getEntityClass(), targetEntity, inverseField, sourceEntity);
                });
            }
            case ONE_TO_MANY -> {
                Set<Object> set = (Set<Object>) getterMethod.invoke(sourceEntity);
                set.add(targetEntity);
                hibernateEntityHelper.setFieldValue(((JpaMetamodelVertex)sourceMetamodelVertex).getEntityClass(), sourceEntity, field, set);
                hibernateEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex)targetVertex).getEntityClass()).ifPresent(inverseField ->
                    hibernateEntityHelper.setFieldValue(((JpaMetamodelVertex)targetVertex).getEntityClass(), targetEntity, inverseField, sourceEntity));
            }
            case MANY_TO_ONE -> {
                hibernateEntityHelper.setFieldValue(((JpaMetamodelVertex)sourceMetamodelVertex).getEntityClass(), sourceEntity, field, targetEntity);
                Optional<Field> inverseField1 = hibernateEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex)targetVertex).getEntityClass());
                inverseField1.ifPresent(inverseField -> {
                    Method inverseGetterMethod = hibernateEntityHelper.getterMethod(((JpaMetamodelVertex)targetVertex).getEntityClass(), inverseField);
                    if (inverseGetterMethod != null) {
                        Set inverseCollection = null;
                        try {
                            inverseCollection = (Set) inverseGetterMethod.invoke(targetEntity);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                        if (inverseCollection != null) {
                            if (!Persistence.getPersistenceUtil().isLoaded(targetEntity,inverseField.getName())) {
                                inverseCollection = new HashSet(0);
                            }
                            inverseCollection.add(sourceEntity);
                            hibernateEntityHelper.setFieldValue(((JpaMetamodelVertex)targetVertex).getEntityClass(), targetEntity, inverseField, inverseCollection);
                        }
                    }
                });
            }
            case MANY_TO_MANY -> {
                Set<Object> set = (Set<Object>) getterMethod.invoke(sourceEntity);
                set.add(targetEntity);
                hibernateEntityHelper.setFieldValue(((JpaMetamodelVertex)sourceMetamodelVertex).getEntityClass(), sourceEntity, field, set);
                Optional<Field> inverseField1 = hibernateEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex)targetVertex).getEntityClass());
                inverseField1.ifPresent(inverseField -> {
                    Method inverseGetterMethod = hibernateEntityHelper.getterMethod(((JpaMetamodelVertex)targetVertex).getEntityClass(), inverseField);
                    if (inverseGetterMethod != null) {
                        Set inverseCollection = null;
                        try {
                            inverseCollection = (Set) inverseGetterMethod.invoke(targetEntity);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                        if (inverseCollection != null) {
                            if (!Persistence.getPersistenceUtil().isLoaded(targetEntity,inverseField.getName())) {
                                inverseCollection = new HashSet(0);
                            }
                            inverseCollection.add(sourceEntity);
                            hibernateEntityHelper.setFieldValue(((JpaMetamodelVertex)targetVertex).getEntityClass(), targetEntity, inverseField, inverseCollection);
                        }
                    }
                });
            }
        }
    }
}
