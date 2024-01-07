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

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.modelgraph.model.SourceVertexEdgeAndTargetVertices;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class SourceVertexEdgeAndTargetVertex {
    private final SourceVertexEdgeAndTargetVertices edgeAndTargetVertex;
    private final Vertex targetVertex;

    public SourceVertexEdgeAndTargetVertex(SourceVertexEdgeAndTargetVertices edgeAndTargetVertex, Vertex targetVertex) {
        this.edgeAndTargetVertex = edgeAndTargetVertex;
        this.targetVertex = targetVertex;
    }

    public Vertex getSourceVertex() {
        return edgeAndTargetVertex.getSourceVertex();
    }

    public FieldEdge getEdge() {
        return edgeAndTargetVertex.getEdge();
    }

    public Vertex getTargetVertex() {
        return targetVertex;
    }
}
