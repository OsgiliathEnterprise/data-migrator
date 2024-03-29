package net.osgiliath.migrator.core.modelgraph.model;

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
import net.osgiliath.migrator.core.api.model.ModelElement;

public class MetamodelVertexAndModelElementAndModelElementId {
    private final MetamodelVertex metamodelVertex;
    private final ModelElement modelElement;
    private final Object id;

    public MetamodelVertexAndModelElementAndModelElementId(MetamodelVertex metamodelVertex, ModelElement modelElement, Object id) {
        this.metamodelVertex = metamodelVertex;
        this.modelElement = modelElement;
        this.id = id;
    }

    public MetamodelVertex getMetamodelVertex() {
        return metamodelVertex;
    }

    public ModelElement getModelElement() {
        return modelElement;
    }

    public Object getId() {
        return id;
    }
}
