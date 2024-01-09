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
import jakarta.persistence.Persistence;
import net.osgiliath.migrator.core.modelgraph.model.ModelElement;
import org.jgrapht.Graph;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;

public class OutboundEdge {

    private final FieldEdge fieldEdge;
    private final MetamodelVertex targetVertex;
    public OutboundEdge(FieldEdge fieldEdge, MetamodelVertex targetVertex) {
        this.fieldEdge = fieldEdge;
        this.targetVertex = targetVertex;
    }

    public MetamodelVertex getTargetVertex() {
        return targetVertex;
    }

    public FieldEdge getFieldEdge() {
        return fieldEdge;
    }

    public void setEdgeBetweenEntities(MetamodelVertex sourceMetamodelVertex, ModelElement sourceEntity, ModelElement targetEntity, Graph<MetamodelVertex, FieldEdge> graph) throws InvocationTargetException, IllegalAccessException {
        //Field field = fieldEdge.getMetamodelField();
        //Method getterMethod = sourceMetamodelVertex.relationshipGetter(fieldEdge);
        RelationshipType relationshipType = fieldEdge.getRelationshipType(sourceMetamodelVertex);
        switch (relationshipType) {
            case ONE_TO_ONE -> {
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, fieldEdge, targetEntity.getEntity());
                sourceMetamodelVertex.getInverseFieldEdge(fieldEdge, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, sourceEntity.getEntity());
                });
            }
            case ONE_TO_MANY -> {
                Collection set = (Collection) sourceEntity.getEdgeRawValue(sourceMetamodelVertex, fieldEdge);
                set.add(targetEntity.getEntity());
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, fieldEdge, set);
                sourceMetamodelVertex.getInverseFieldEdge(fieldEdge, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, sourceEntity.getEntity());
                });
            }
            case MANY_TO_ONE -> {
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, fieldEdge, targetEntity.getEntity());
                sourceMetamodelVertex.getInverseFieldEdge(fieldEdge, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    Collection inverseCollection = (Collection) targetEntity.getEdgeRawValue(targetVertex, inverseFieldEdge);
                    if (!Persistence.getPersistenceUtil().isLoaded(targetEntity,inverseFieldEdge.getFieldName())) {
                        inverseCollection = new HashSet(0);
                    }
                    inverseCollection.add(sourceEntity.getEntity());
                    targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, inverseCollection);
                });
            }
            case MANY_TO_MANY -> {
                Collection set = (Collection) sourceEntity.getEdgeRawValue(sourceMetamodelVertex, fieldEdge);
                set.add(targetEntity.getEntity());
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, fieldEdge, set);
                sourceMetamodelVertex.getInverseFieldEdge(fieldEdge, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    Collection inverseCollection = (Collection) targetEntity.getEdgeRawValue(targetVertex, inverseFieldEdge);
                    if (!Persistence.getPersistenceUtil().isLoaded(targetEntity,inverseFieldEdge.getFieldName())) {
                        inverseCollection = new HashSet(0);
                    }
                    inverseCollection.add(sourceEntity.getEntity());
                    targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, inverseCollection);
                });
            }
        }
    }
}
