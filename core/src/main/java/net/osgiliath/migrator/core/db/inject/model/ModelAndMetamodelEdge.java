package net.osgiliath.migrator.core.db.inject.model;

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

import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;
import org.apache.tinkerpop.gremlin.structure.Edge;

public class ModelAndMetamodelEdge {
    private final Edge edge;
    private final OutboundEdge metamodelEdge;

    public ModelAndMetamodelEdge(Edge modelEdge, OutboundEdge metamodelEdge) {
        this.edge = modelEdge;
        this.metamodelEdge = metamodelEdge;
    }

    public Edge getModelEdge() {
        return edge;
    }

    public OutboundEdge getMetamodelEdge() {
        return metamodelEdge;
    }
}
