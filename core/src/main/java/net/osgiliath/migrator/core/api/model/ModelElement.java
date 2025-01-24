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

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;

/**
 * A vertex in the model graph.
 */
public class ModelElement {
    private final MetamodelVertex vertex;
    private Object rawElement;

    /**
     * @param vertex     the metamodel element.
     * @param rawElement the underlying element.
     */
    public ModelElement(MetamodelVertex vertex, Object rawElement) {
        this.vertex = vertex;
        this.rawElement = rawElement;
    }

    public MetamodelVertex vertex() {
        return vertex;
    }

    public Object rawElement() {
        return this.rawElement;
    }

    public void setRawElement(Object rawElement) {
        this.rawElement = rawElement;
    }
}
