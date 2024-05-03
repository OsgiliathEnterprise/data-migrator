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
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphRequester;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.jpa.JpaEntityProcessor;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

/**
 * Metamodel edge representing a relationship between two tables.
 */
public class FieldEdge<M extends MetamodelVertex> extends DefaultEdge {

    /**
     * Entity field representing to call in order to get the relationship.
     */
    private final Field metamodelField;
    /**
     * JPA entity helper bean.
     */
    private final JpaEntityProcessor jpaEntityHelper;
    private final MetamodelGraphRequester<M> metamodelGraphRequester;

    /**
     * Constructor.
     *
     * @param jpaEntityHelper JPA entity helper.
     * @param field           entity field representing to call in order to get the relationship.
     */
    public FieldEdge(JpaEntityProcessor jpaEntityHelper, Field field, MetamodelGraphRequester<M> metamodelGraphRequester) {
        this.metamodelField = field;
        this.jpaEntityHelper = jpaEntityHelper;
        this.metamodelGraphRequester = metamodelGraphRequester;
    }

    /**
     * Gets the JPA metamodel field.
     *
     * @return the JPA metamodel field.
     */
    public Field getMetamodelField() {
        return metamodelField;
    }

    /**
     * Gets the field name.
     *
     * @return the field name.
     */
    public String getFieldName() {
        return metamodelField.getName();
    }

    /**
     * Gets the source vertex (entity definition).
     *
     * @return the source vertex.
     */
    @Override
    public M getSource() {
        return (M) super.getSource();
    }

    /**
     * Gets the target vertex (entity definition).
     *
     * @return the target vertex.
     */
    @Override
    public M getTarget() {
        return (M) super.getTarget();
    }

    /**
     * returns the getter method of the entity's relationship.
     *
     * @return the getter method of the entity's relationship.
     */
    public Method relationshipGetter() {
        return jpaEntityHelper.getterMethod(((JpaMetamodelVertex) this.getSource()).getEntityClass(), this.getMetamodelField());
    }

    /**
     * Get the type of the relationship (one to one, one to many, many to one, many to many).
     *
     * @return the type of the relationship.
     */
    public RelationshipType getRelationshipType() {
        Method getterMethod = relationshipGetter();
        return this.getSource().relationshipType(getterMethod);
    }

    /**
     * Sets a relationship between two entities.
     *
     * @param sourceMetamodelVertex the source entity definition.
     * @param sourceEntity          the source entity.
     * @param targetEntity          the target entity.
     * @param graph                 the metamodel graph.
     */
    public void setEdgeBetweenEntities(M sourceMetamodelVertex, ModelElement sourceEntity, ModelElement targetEntity, Graph<M, FieldEdge<M>> graph) {
        RelationshipType relationshipType = getRelationshipType();
        M targetVertex = graph.getEdgeTarget(this);
        switch (relationshipType) {
            case ONE_TO_ONE -> {
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, this, targetEntity.getRawElement());
                metamodelGraphRequester.getInverseFieldEdge(this, targetVertex, graph).ifPresent(inverseFieldEdge ->
                        targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, sourceEntity.getRawElement())
                );
            }
            case ONE_TO_MANY -> {
                Collection set = (Collection) sourceEntity.getEdgeRawValue(this);
                set.add(targetEntity.getRawElement());
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, this, set);
                metamodelGraphRequester.getInverseFieldEdge(this, targetVertex, graph).ifPresent(inverseFieldEdge ->
                        targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, sourceEntity.getRawElement())
                );
            }
            case MANY_TO_ONE -> {
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, this, targetEntity.getRawElement());
                metamodelGraphRequester.getInverseFieldEdge(this, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    Collection inverseCollection = (Collection) targetEntity.getEdgeRawValue(inverseFieldEdge);
                    if (!Persistence.getPersistenceUtil().isLoaded(targetEntity, inverseFieldEdge.getFieldName())) {
                        inverseCollection = new HashSet(0);
                    }
                    inverseCollection.add(sourceEntity.getRawElement());
                    targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, inverseCollection);
                });
            }
            case MANY_TO_MANY -> {
                Collection set = (Collection) sourceEntity.getEdgeRawValue(this);
                set.add(targetEntity.getRawElement());
                sourceEntity.setEdgeRawValue(sourceMetamodelVertex, this, set);
                metamodelGraphRequester.getInverseFieldEdge(this, targetVertex, graph).ifPresent(inverseFieldEdge -> {
                    Collection inverseCollection = (Collection) targetEntity.getEdgeRawValue(inverseFieldEdge);
                    if (!Persistence.getPersistenceUtil().isLoaded(targetEntity, inverseFieldEdge.getFieldName())) {
                        inverseCollection = new HashSet(0);
                    }
                    inverseCollection.add(sourceEntity.getRawElement());
                    targetEntity.setEdgeRawValue(targetVertex, inverseFieldEdge, inverseCollection);
                });
            }
        }
    }
}
