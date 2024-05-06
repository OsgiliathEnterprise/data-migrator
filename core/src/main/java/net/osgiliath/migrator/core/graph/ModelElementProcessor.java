package net.osgiliath.migrator.core.graph;

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

import jakarta.persistence.Persistence;
import net.osgiliath.migrator.core.api.metamodel.RelationshipType;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.exception.ErrorCallingRawElementMethodException;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.jgrapht.Graph;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

@Service
public class ModelElementProcessor {

    private final RawElementProcessor rawElementProcessor;
    private final MetamodelRequester metamodelRequester;

    public ModelElementProcessor(RawElementProcessor rawElementProcessor, MetamodelRequester metamodelRequester) {

        this.rawElementProcessor = rawElementProcessor;
        this.metamodelRequester = metamodelRequester;
    }

    public void removeEdgeValueFromModelElementRelationShip(ModelElement sourceModelElement, FieldEdge<MetamodelVertex> fieldEdge, ModelElement targetModelElement) {
        Object targetValue = getEdgeRawValue(fieldEdge, sourceModelElement);
        if (targetValue instanceof Collection targetValues) {
            targetValues.remove(targetModelElement.rawElement());
            setEdgeRawValue(fieldEdge, sourceModelElement, targetValue);
        } else {
            setEdgeRawValue(fieldEdge, sourceModelElement, null);
        }
        Method getterMethod = metamodelRequester.relationshipGetter(fieldEdge);
        Optional<Field> inverseFieldOpt = rawElementProcessor.inverseRelationshipField(getterMethod, fieldEdge.getTarget());
        inverseFieldOpt.ifPresent(
                inverseField -> {
                    Object inverseValue = rawElementProcessor.getFieldValue(fieldEdge.getTarget(), targetModelElement.rawElement(), inverseField.getName());
                    if (inverseValue instanceof Collection inverseValues) {
                        inverseValues.remove(sourceModelElement.rawElement());
                        rawElementProcessor.setFieldValue(fieldEdge.getTarget(), targetModelElement.rawElement(), inverseField.getName(), inverseValues);
                    } else {
                        rawElementProcessor.setFieldValue(fieldEdge.getTarget(), targetModelElement.rawElement(), inverseField.getName(), null);
                    }
                }
        );
    }

    /**
     * Sets a relationship between two entities.
     *
     * @param fieldEdge    the metamodel edge to update
     * @param sourceEntity the source entity.
     * @param targetEntity the target entity.
     * @param graph        the metamodel graph.
     */
    public void addRawElementsRelationshipForEdge(FieldEdge<MetamodelVertex> fieldEdge, ModelElement sourceEntity, ModelElement targetEntity, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> graph) {
        RelationshipType relationshipType = metamodelRequester.getRelationshipType(fieldEdge);
        MetamodelVertex targetVertex = fieldEdge.getTarget();
        switch (relationshipType) {
            case ONE_TO_ONE -> {
                setEdgeRawValue(fieldEdge, sourceEntity, targetEntity.rawElement());
                metamodelRequester.getInverseFieldEdge(fieldEdge, targetVertex, graph).ifPresent(inverseFieldEdge ->
                        setEdgeRawValue(inverseFieldEdge, targetEntity, sourceEntity.rawElement())
                );
            }
            case ONE_TO_MANY -> {
                Collection set = (Collection) getEdgeRawValue(fieldEdge, sourceEntity);
                set.add(targetEntity.rawElement());
                setEdgeRawValue(fieldEdge, sourceEntity, set);
                metamodelRequester.getInverseFieldEdge(fieldEdge, targetVertex, graph).ifPresent(inverseFieldEdge ->
                        setEdgeRawValue(inverseFieldEdge, targetEntity, sourceEntity.rawElement())
                );
            }
            case MANY_TO_ONE -> {
                setEdgeRawValue(fieldEdge, sourceEntity, targetEntity.rawElement());
                addEntityToInverseToManyTargetEntity(fieldEdge, sourceEntity, targetEntity, graph, targetVertex);
            }
            case MANY_TO_MANY -> {
                Collection set = (Collection) getEdgeRawValue(fieldEdge, sourceEntity);
                set.add(targetEntity.rawElement());
                setEdgeRawValue(fieldEdge, sourceEntity, set);
                addEntityToInverseToManyTargetEntity(fieldEdge, sourceEntity, targetEntity, graph, targetVertex);
            }
        }
    }

    private void addEntityToInverseToManyTargetEntity(FieldEdge<MetamodelVertex> fieldEdge, ModelElement sourceEntity, ModelElement targetEntity, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> graph, MetamodelVertex targetVertex) {
        metamodelRequester.getInverseFieldEdge(fieldEdge, targetVertex, graph).ifPresent(inverseFieldEdge -> {
            Collection inverseCollection = (Collection) getEdgeRawValue(inverseFieldEdge, targetEntity);
            if (!Persistence.getPersistenceUtil().isLoaded(targetEntity, inverseFieldEdge.getFieldName())) {
                inverseCollection = HashSet.newHashSet(0);
            }
            inverseCollection.add(sourceEntity.rawElement());
            setEdgeRawValue(inverseFieldEdge, targetEntity, inverseCollection);
        });
    }

    /**
     * Returns the Raw value(s) corresponding to the entity referenced by the outboundEdge
     *
     * @param sourceMetamodelVertex the Metamodel Vertex
     * @return Returns the ModelElement(s) corresponding to the entity referenced by the outboundEdge
     */
    public Object getFieldRawValue(MetamodelVertex sourceMetamodelVertex, String fieldName, ModelElement modelElement) {
        return rawElementProcessor.getFieldValue(sourceMetamodelVertex, modelElement.rawElement(), fieldName);
    }


    /**
     * Sets a value on the underlying element
     *
     * @param entityClass The entity class (can be null)
     * @param fieldName   The field name
     * @param value       The value to set
     */
    public void setFieldRawValue(MetamodelVertex entityClass, String fieldName, ModelElement modelElement, Object value) {
        if (entityClass != null) {
            rawElementProcessor.setFieldValue(entityClass, modelElement.rawElement(), fieldName, value);
        } else {
            rawElementProcessor.setFieldValue(modelElement.rawElement().getClass(), modelElement.rawElement(), fieldName, value);
        }
    }

    public void setEdgeRawValue(FieldEdge<MetamodelVertex> field, ModelElement modelElement, Object value) {
        if (field.getSource() != null) {
            rawElementProcessor.setFieldValue(field.getSource(), modelElement.rawElement(), field.getFieldName(), value);
        } else {
            rawElementProcessor.setFieldValue(modelElement.rawElement().getClass(), modelElement.rawElement(), field.getFieldName(), value);
        }
    }

    /**
     * Returns the Raw value(s) corresponding to the entity referenced by the outboundEdge
     *
     * @param fieldEdge the edge to get the target vertices from
     * @return Returns the ModelElement(s) corresponding to the entity referenced by the outboundEdge
     */
    public Object getEdgeRawValue(FieldEdge<MetamodelVertex> fieldEdge, ModelElement modelElement) {
        Method getterMethod = metamodelRequester.relationshipGetter(fieldEdge);
        try {
            return getterMethod.invoke(modelElement.rawElement());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ErrorCallingRawElementMethodException(e);
        }
    }

    public Optional<Object> getId(MetamodelVertex metamodelVertex, ModelElement modelElement) {
        if (metamodelVertex != null) {
            return rawElementProcessor.getId(metamodelVertex, modelElement.rawElement());
        } else {
            return rawElementProcessor.getId(this.getClass(), modelElement.rawElement());
        }
    }

    public void resetModelElementEdge(FieldEdge<MetamodelVertex> metamodelEdge, ModelElement sourceModelElement) {
        if (metamodelRequester.isMany(metamodelRequester.getRelationshipType(metamodelEdge))) {
            setEdgeRawValue(metamodelEdge, sourceModelElement, new HashSet<>());
        } else {
            setEdgeRawValue(metamodelEdge, sourceModelElement, null);
        }
    }
}
