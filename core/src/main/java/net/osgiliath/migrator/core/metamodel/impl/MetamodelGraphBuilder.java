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
        addVertexEdgesFromMetamodel(graph).forEach(elt -> log.debug("edges added"));
        return graph;
    }

    private Stream<Boolean> addVertexEdgesFromMetamodel(Graph<M, FieldEdge<M>> graph) {
        return graph.vertexSet().stream()
                .flatMap(v -> computeOutboundEdges(v, graph)
                        .map(e -> new MetamodelVertexAndOutboundEdge<>(v, e)))
                .map(me ->
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

    /**
     * Cluster the full entity metamodel graph into subgraphs.
     * Each subgraph contains vertices that are connected to each other.
     *
     * @param fullEntityMetamodelGraph the full entity metamodel graph
     * @return a collection of subgraphs
     */
    public Collection<Graph<MetamodelVertex, FieldEdge<MetamodelVertex>>> clusterGraphs(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> fullEntityMetamodelGraph) {
        Collection<Graph<MetamodelVertex, FieldEdge<MetamodelVertex>>> ret = new HashSet<>();
        Collection<MetamodelVertex> visited = new HashSet<>();
        for (MetamodelVertex vtx : fullEntityMetamodelGraph.vertexSet()) {
            Collection<MetamodelVertex> subgraph = new HashSet<>();
            if (!visited.contains(vtx)) {
                depthFirstSearch(fullEntityMetamodelGraph, vtx, subgraph, visited);
            }
            if (!subgraph.isEmpty()) {
                Graph graph = GraphTypeBuilder.directed().allowingMultipleEdges(true)
                        .allowingSelfLoops(true).vertexClass(MetamodelVertex.class).edgeClass(FieldEdge.class).weighted(false).buildGraph();
                subgraph.stream().forEach(graph::addVertex);
                // Add edges from the subgraph to the full graph
                subgraph.stream().forEach(
                        subgraphVertex -> {
                            fullEntityMetamodelGraph.incomingEdgesOf(subgraphVertex).stream().filter(incomingEdge -> !graph.containsEdge(incomingEdge))
                                    .forEach(incomingEdge -> graph.addEdge(incomingEdge.getSource(), subgraphVertex, new FieldEdge<>(incomingEdge.getMetamodelField())));
                            fullEntityMetamodelGraph.outgoingEdgesOf(subgraphVertex).stream().filter(outgoingEdge -> !graph.containsEdge(outgoingEdge))
                                    .forEach(outgoingEdge -> graph.addEdge(subgraphVertex, outgoingEdge.getTarget(), new FieldEdge<>(outgoingEdge.getMetamodelField())));
                        }
                );
                ret.add(graph);
            }
        }
        return ret;
    }

    /**
     * Depth first search to find all vertices connected to the given vertex.
     *
     * @param fullEntityMetamodelGraph the full entity metamodel graph
     * @param vertex                   the vertex to start from
     * @param subgraph                 the collection to add found vertices to
     * @param visited                  the collection of visited vertices from the full graph
     */
    private void depthFirstSearch(
            Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> fullEntityMetamodelGraph,
            MetamodelVertex vertex,
            Collection<MetamodelVertex> subgraph,
            Collection<MetamodelVertex> visited
    ) {
        visited.add(vertex);
        subgraph.add(vertex);
        Collection<MetamodelVertex> neighbours = new HashSet<>();
        Collection<FieldEdge<MetamodelVertex>> incomingEdges = fullEntityMetamodelGraph.incomingEdgesOf(vertex);
        incomingEdges.stream().map(FieldEdge::getSource).forEach(neighbours::add);
        Collection<FieldEdge<MetamodelVertex>> outgoingEdges = fullEntityMetamodelGraph.outgoingEdgesOf(vertex);
        outgoingEdges.stream().map(FieldEdge::getTarget).forEach(neighbours::add);
        neighbours.stream().filter(neighbour -> !visited.contains(neighbour)).forEach(neighbour -> depthFirstSearch(fullEntityMetamodelGraph, neighbour, subgraph, visited));
    }
}
