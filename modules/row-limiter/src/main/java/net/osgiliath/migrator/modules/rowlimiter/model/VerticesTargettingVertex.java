package net.osgiliath.migrator.modules.rowlimiter.model;

/*-
 * #%L
 * row-limiter
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

import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.List;

public class VerticesTargettingVertex {

    private final List<Vertex> sourceVertices;
    private final Vertex targetVertex;

    public VerticesTargettingVertex(Object leafElement, List<Vertex> sourceVertices, Vertex targetVertex) {

        this.sourceVertices = sourceVertices;
        this.targetVertex = targetVertex;
    }

    public List<Vertex> getSourceVertices() {
        return sourceVertices;
    }

    public Vertex getTargetVertex() {
        return targetVertex;
    }
}
