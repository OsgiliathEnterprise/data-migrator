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

import org.jgrapht.graph.DefaultEdge;

import java.lang.reflect.Field;

/**
 * Metamodel edge representing a relationship between two tables.
 *
 * @param <M> the MetamodelVertex kind.
 */
public class FieldEdge<M extends MetamodelVertex> extends DefaultEdge {

    /**
     * Entity field representing to call in order to get the relationship.
     */
    private final transient Field rawElementField;

    /**
     * Constructor.
     *
     * @param rawElementField entity field representing to call in order to get the relationship.
     */
    public FieldEdge(Field rawElementField) {
        this.rawElementField = rawElementField;
    }

    /**
     * Gets the JPA metamodel field.
     *
     * @return the JPA metamodel field.
     */
    public Field getMetamodelField() {
        return rawElementField;
    }

    /**
     * Gets the field name.
     *
     * @return the field name.
     */
    public String getFieldName() {
        return rawElementField.getName();
    }

    @Override
    public M getSource() {
        return (M) super.getSource();
    }

    @Override
    public M getTarget() {
        return (M) super.getTarget();
    }

}
