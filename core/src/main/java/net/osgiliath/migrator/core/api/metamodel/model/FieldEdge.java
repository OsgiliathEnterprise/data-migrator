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

import jakarta.persistence.Persistence;
import net.osgiliath.migrator.core.api.metamodel.RelationshipType;
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

/**
 * Metamodel edge representing a relationship between two tables.
 */
public class FieldEdge extends DefaultEdge {

    /**
     * Entity field representing to call in order to get the relationship.
     */
    private final Field metamodelField;
    /**
     * JPA entity helper bean.
     */
    private final JpaEntityHelper jpaEntityHelper;

    /**
     * Constructor.
     * @param jpaEntityHelper JPA entity helper.
     * @param field entity field representing to call in order to get the relationship.
     */
    public FieldEdge(JpaEntityHelper jpaEntityHelper, Field field) {
        this.metamodelField = field;
        this.jpaEntityHelper = jpaEntityHelper;
    }

    /**
     * Gets the JPA metamodel field.
     * @return the JPA metamodel field.
     */
    public Field getMetamodelField() {
        return metamodelField;
    }

    /**
     * Gets the field name.
     * @return the field name.
     */
    public String getFieldName() {
        return metamodelField.getName();
    }

    /**
     * Gets the source vertex (entity definition).
     * @return the source vertex.
     */
    public MetamodelVertex getSource() {
        return (MetamodelVertex) super.getSource();
    }

    /**
     * Gets the target vertex (entity definition).
     * @return the target vertex.
     */
    public MetamodelVertex getTarget() {
        return (MetamodelVertex) super.getTarget();
    }

    /**
     * returns the getter method of the entity's relationship.
     * @return the getter method of the entity's relationship.
     */
    public Method relationshipGetter() {
        return jpaEntityHelper.getterMethod(((JpaMetamodelVertex)this.getSource()).getEntityClass(), this.getMetamodelField());
    }

    /**
     * Get the type of the relationship (one to one, one to many, many to one, many to many).
     * @return the type of the relationship.
     */
    public RelationshipType getRelationshipType() {
        Method getterMethod = relationshipGetter();
        return this.getSource().relationshipType(getterMethod);
    }

    /**
     * Sets a relationship between two entities.
     * @param sourceMetamodelVertex the source entity definition.
     * @param sourceEntity the source entity.
     * @param targetEntity the target entity.
     * @param graph the metamodel graph.
     */
    public void setEdgeBetweenEntities(MetamodelVertex sourceMetamodelVertex, ModelElement sourceEntity, ModelElement targetEntity, Graph<MetamodelVertex, FieldEdge> graph) {
        RelationshipType relationshipType = getRelationshipType();
        MetamodelVertex targetVertex = graph.getEdgeTarget(this);
        switch (relationshipType) {
            case ONE_TO_ONE -> {
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, this, targetEntity.getEntity());
                sourceMetamodelVertex.getInverseFieldEdge(this, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, sourceEntity.getEntity());
                });
            }
            case ONE_TO_MANY -> {
                Collection set = (Collection) sourceEntity.getEdgeRawValue(this);
                set.add(targetEntity.getEntity());
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, this, set);
                sourceMetamodelVertex.getInverseFieldEdge(this, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, sourceEntity.getEntity());
                });
            }
            case MANY_TO_ONE -> {
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, this, targetEntity.getEntity());
                sourceMetamodelVertex.getInverseFieldEdge(this, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    Collection inverseCollection = (Collection) targetEntity.getEdgeRawValue(inverseFieldEdge);
                    if (!Persistence.getPersistenceUtil().isLoaded(targetEntity,inverseFieldEdge.getFieldName())) {
                        inverseCollection = new HashSet(0);
                    }
                    inverseCollection.add(sourceEntity.getEntity());
                    targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, inverseCollection);
                });
            }
            case MANY_TO_MANY -> {
                Collection set = (Collection) sourceEntity.getEdgeRawValue(this);
                set.add(targetEntity.getEntity());
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, this, set);
                sourceMetamodelVertex.getInverseFieldEdge(this, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    Collection inverseCollection = (Collection) targetEntity.getEdgeRawValue(inverseFieldEdge);
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
