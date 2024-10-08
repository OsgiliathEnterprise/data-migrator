package net.osgiliath.migrator.core.metamodel.impl;

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

import net.osgiliath.migrator.core.api.metamodel.MetamodelVertexFactory;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;
import net.osgiliath.migrator.core.metamodel.impl.model.MetamodelVertexAndOutboundEdge;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

public abstract class MetamodelGraphBuilder<M extends MetamodelVertex> {
    private static final Logger log = LoggerFactory.getLogger(MetamodelGraphBuilder.class);
    private final MetamodelVertexFactory<M> metamodelVertexFactory;

    protected MetamodelGraphBuilder(MetamodelVertexFactory metamodelVertexFactory) {
        this.metamodelVertexFactory = metamodelVertexFactory;
    }

    public Graph<M, FieldEdge<M>> metamodelGraphFromRawElementClasses(Collection<Class<?>> metamodelClasses) {
        log.warn("Starting graph processing of the metamodel");
        Graph graph = GraphTypeBuilder.directed().allowingMultipleEdges(true)
                .allowingSelfLoops(true).vertexClass(MetamodelVertex.class).edgeClass(FieldEdge.class).weighted(false).buildGraph();
        Stream<M> vertex = metamodelVertexFromRawMetamodelClass(metamodelClasses);
        vertex.filter(this::isEntity).forEach(graph::addVertex);
        addVertexEdgesFromMetamodel(graph);
        return graph;
    }

    private void addVertexEdgesFromMetamodel(Graph<M, FieldEdge<M>> graph) {
        graph.vertexSet().stream()
                .flatMap(v -> computeOutboundEdges(v, graph)
                        .map(e -> new MetamodelVertexAndOutboundEdge<>(v, e)))
                .forEach(me ->
                        graph.addEdge(me.sourceVertex(), me.targetVertex(), me.fieldEdge()));
    }

    private Stream<M> metamodelVertexFromRawMetamodelClass(Collection<Class<?>> metamodelClasses) {
        return metamodelClasses.stream()
                .map(metamodelVertexFactory::createMetamodelVertex);
    }

    /**
     * Compute the outbound edges of this vertex from the raw element relationships.
     *
     * @param sourceVertex vertex to compute edge from
     * @param graph        The metamodel graph.
     * @return The outbound edges of this vertex.
     */
    protected abstract Stream<OutboundEdge<M>> computeOutboundEdges(M sourceVertex, Graph<M, FieldEdge<M>> graph);

    protected abstract boolean isEntity(M metamodelVertex);

    public Collection<Graph<MetamodelVertex, FieldEdge<MetamodelVertex>>> clusterGraphs(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> fullEntityMetamodelGraph) {
        // TODO improve this method to cluster graph that are disjoints, not only isolated vertex
        Collection<Graph<MetamodelVertex, FieldEdge<MetamodelVertex>>> ret = new HashSet<>();
        Collection<MetamodelVertex> isolatedVertices = new HashSet<>();
        for (MetamodelVertex vtx : fullEntityMetamodelGraph.vertexSet()) {
            if (fullEntityMetamodelGraph.degreeOf(vtx) == 0) {
                isolatedVertices.add(vtx);
            }
        }
        fullEntityMetamodelGraph.removeAllVertices(isolatedVertices);
        ret.add(fullEntityMetamodelGraph);
        for (MetamodelVertex vtx : isolatedVertices) {
            Graph graph = GraphTypeBuilder.directed().allowingMultipleEdges(true)
                    .allowingSelfLoops(true).vertexClass(MetamodelVertex.class).edgeClass(FieldEdge.class).weighted(false).buildGraph();
            graph.addVertex(vtx);
            ret.add(graph);
        }
        return ret;
    }
}
