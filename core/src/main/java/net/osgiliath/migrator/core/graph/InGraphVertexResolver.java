package net.osgiliath.migrator.core.graph;

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
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import static net.osgiliath.migrator.core.graph.ModelGraphBuilder.*;

public class InGraphVertexResolver implements VertexResolver {
    @Override
    public MetamodelVertex getMetamodelVertex(Vertex vertex) {
        return vertex.value(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX);
    }

    @Override
    public GraphTraversal setMetamodelVertex(GraphTraversal traversal, MetamodelVertex metamodelVertex) {
        return traversal.property(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX, metamodelVertex);
    }

    @Override
    public ModelElement getModelElement(Vertex vertex) {
        return vertex.value(MODEL_GRAPH_VERTEX_ENTITY);
    }

    @Override
    public GraphTraversal setModelElement(GraphTraversal traversal, ModelElement modelElement) {
        return traversal.property(MODEL_GRAPH_VERTEX_ENTITY, modelElement);
    }

    @Override
    public GraphTraversal setVertexModelElementId(GraphTraversal traversal, Object id) {
        return traversal.property(MODEL_GRAPH_VERTEX_ENTITY_ID, id);
    }

    @Override
    public Object getVertexModelElementId(Vertex vtx) {
        return vtx.value(MODEL_GRAPH_VERTEX_ENTITY_ID);
    }

    @Override
    public Object getWrappedRawId(Object originalId) {
        return originalId;
    }

    @Override
    public void clear() {

    }

}
