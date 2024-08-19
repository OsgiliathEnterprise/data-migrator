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
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import net.osgiliath.migrator.core.rawelement.jpa.RelationshipProcessor;
import org.jgrapht.Graph;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

@Service
public class ModelElementProcessor {

    private final RawElementProcessor rawElementProcessor;
    private final MetamodelRequester metamodelRequester;
    private final RelationshipProcessor relationshipProcessor;

    public ModelElementProcessor(RawElementProcessor rawElementProcessor, MetamodelRequester metamodelRequester, RelationshipProcessor relationshipProcessor) {

        this.rawElementProcessor = rawElementProcessor;
        this.metamodelRequester = metamodelRequester;
        this.relationshipProcessor = relationshipProcessor;
    }

    public void removeEdgeValueFromModelElementRelationShip(ModelElement sourceModelElement, FieldEdge<MetamodelVertex> fieldEdge, ModelElement targetModelElement) {
        Object targetValue = relationshipProcessor.getEdgeRawValue(fieldEdge, sourceModelElement);
        if (targetValue instanceof Collection targetValues) {
            targetValues.remove(targetModelElement.rawElement());
            setEdgeRawValue(fieldEdge, sourceModelElement, targetValue);
        } else {
            setEdgeRawValue(fieldEdge, sourceModelElement, null);
        }
        Optional<Method> getterMethodOpt = metamodelRequester.relationshipGetter(fieldEdge);
        Optional<Field> inverseFieldOpt = getterMethodOpt.flatMap(getterMethod -> rawElementProcessor.inverseRelationshipField(getterMethod, fieldEdge.getTarget()));
        inverseFieldOpt.ifPresent(
                inverseField -> {
                    Object inverseValue = rawElementProcessor.getFieldValue(targetModelElement, inverseField.getName());
                    if (inverseValue instanceof Collection inverseValues) {
                        inverseValues.remove(sourceModelElement.rawElement());
                        rawElementProcessor.setFieldValue(targetModelElement, inverseField.getName(), inverseValues);
                    } else {
                        rawElementProcessor.setFieldValue(targetModelElement, inverseField.getName(), null);
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
        Optional<RelationshipType> relationshipTypeOpt = metamodelRequester.getRelationshipType(fieldEdge);
        MetamodelVertex targetVertex = fieldEdge.getTarget();
        relationshipTypeOpt.ifPresent(
                (RelationshipType relationshipType) -> {
                    switch (relationshipType) {
                        case ONE_TO_ONE -> {
                            setEdgeRawValue(fieldEdge, sourceEntity, targetEntity.rawElement());
                            metamodelRequester.getInverseFieldEdge(fieldEdge, targetVertex, graph).ifPresent(inverseFieldEdge ->
                                    setEdgeRawValue(inverseFieldEdge, targetEntity, sourceEntity.rawElement())
                            );
                        }
                        case ONE_TO_MANY -> {
                            Collection set = (Collection) relationshipProcessor.getEdgeRawValue(fieldEdge, sourceEntity);
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
                            Collection set = (Collection) relationshipProcessor.getEdgeRawValue(fieldEdge, sourceEntity);
                            set.add(targetEntity.rawElement());
                            setEdgeRawValue(fieldEdge, sourceEntity, set);
                            addEntityToInverseToManyTargetEntity(fieldEdge, sourceEntity, targetEntity, graph, targetVertex);
                        }
                    }
                }
        );
    }

    private void addEntityToInverseToManyTargetEntity(FieldEdge<MetamodelVertex> fieldEdge, ModelElement sourceEntity, ModelElement targetEntity, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> graph, MetamodelVertex targetVertex) {
        metamodelRequester.getInverseFieldEdge(fieldEdge, targetVertex, graph).ifPresent(inverseFieldEdge -> {
            Collection inverseCollection = (Collection) relationshipProcessor.getEdgeRawValue(inverseFieldEdge, targetEntity);
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
     * @return Returns the ModelElement(s) corresponding to the entity referenced by the outboundEdge
     */
    public Object getFieldRawValue(ModelElement modelElement, String fieldName) {
        return rawElementProcessor.getFieldValue(modelElement, fieldName);
    }


    /**
     * Sets a value on the underlying element
     *
     * @param fieldName The field name
     * @param value     The value to set
     */
    public void setFieldRawValue(ModelElement modelElement, String fieldName, Object value) {
        rawElementProcessor.setFieldValue(modelElement, fieldName, value);
    }

    public void setEdgeRawValue(FieldEdge<MetamodelVertex> field, ModelElement modelElement, Object value) {
        rawElementProcessor.setFieldValue(modelElement, field.getFieldName(), value);
    }

    public Optional<Object> getId(ModelElement modelElement) {
        return rawElementProcessor.getId(modelElement);
    }

    public void resetModelElementEdge(FieldEdge<MetamodelVertex> metamodelEdge, ModelElement sourceModelElement) {
        Optional<RelationshipType> relationshipTypeOpt = metamodelRequester.getRelationshipType(metamodelEdge);
        relationshipTypeOpt.ifPresent(
                relationshipType -> {
                    if (metamodelRequester.isMany(relationshipType)) {
                        setEdgeRawValue(metamodelEdge, sourceModelElement, new HashSet<>());
                    } else {
                        setEdgeRawValue(metamodelEdge, sourceModelElement, null);
                    }
                }
        );
    }
}
