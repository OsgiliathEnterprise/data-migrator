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

public class FieldEdge extends DefaultEdge {
    private final Field metamodelField;

    public FieldEdge(Field metamodelField) {
        this.metamodelField = metamodelField;
    }

    public Field getMetamodelField() {
        return metamodelField;
    }

    public String getFieldName() {
        return metamodelField.getName();
    }
}
