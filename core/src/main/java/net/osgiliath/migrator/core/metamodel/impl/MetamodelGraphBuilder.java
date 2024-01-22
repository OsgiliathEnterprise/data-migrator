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
import net.osgiliath.migrator.core.metamodel.impl.model.MetamodelVertexAndOutboundEdge;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MetamodelGraphBuilder {
    private static final Logger log = LoggerFactory.getLogger(MetamodelGraphBuilder.class);
    private final MetamodelVertexFactory metamodelVertexFactory;

    public MetamodelGraphBuilder(MetamodelVertexFactory metamodelVertexFactory) {
        this.metamodelVertexFactory = metamodelVertexFactory;
    }

    public Graph<MetamodelVertex, FieldEdge> metamodelGraphFromEntityMetamodel(Collection<Class<?>> metamodelClasses) {
        log.warn("Starting graph processing of the metamodel");
        Graph<MetamodelVertex, FieldEdge> graph = GraphTypeBuilder.<MetamodelVertex, FieldEdge>directed().allowingMultipleEdges(true)
                .allowingSelfLoops(true).edgeClass(FieldEdge.class).weighted(false).buildGraph();
        Collection<MetamodelVertex> vertex = metamodelClassesToEntityVertexAdapter(metamodelClasses);
        vertex.stream().filter(v -> v.isEntity()).forEach(graph::addVertex);
        addVertexEdgesFromMetamodel(graph);
        return graph;
    }

    private void addVertexEdgesFromMetamodel(Graph<MetamodelVertex, FieldEdge> graph) {
        graph.vertexSet().stream()
                .flatMap(v -> v.computeOutboundEdges(graph).stream()
                        .map(e -> new MetamodelVertexAndOutboundEdge(v, e)))
                .forEach(me ->
                        graph.addEdge(me.getSourceVertex(), me.getTargetVertex(), me.getFieldEdge()));
    }

    private Set<MetamodelVertex> metamodelClassesToEntityVertexAdapter(Collection<Class<?>> metamodelClasses) {
        return metamodelClasses.stream()
                .map(metamodelVertexFactory::createMetamodelVertex)
                .collect(Collectors.toSet());
    }
}
