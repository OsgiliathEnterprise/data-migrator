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

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.configuration.ModelVertexCustomizer;
import net.osgiliath.migrator.core.configuration.beans.GraphTraversalSourceProvider;
import net.osgiliath.migrator.core.graph.model.MetamodelVertexAndModelElementAndModelElementId;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;

@Component
public class ModelGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(ModelGraphBuilder.class);
    public static final String MODEL_GRAPH_VERTEX_ENTITY_ID = "rawid";
    public static final String MODEL_GRAPH_VERTEX_METAMODEL_VERTEX = "metamodelVertex";
    public static final String MODEL_GRAPH_VERTEX_ENTITY = "entity";
    private final GraphTraversalSourceProvider graphTraversalSource;
    private final ModelVertexCustomizer modelVertexCustomizer;
    private final VertexResolver vertexResolver;
    private final ModelVertexInformationRetriever modelVertexInformationRetriever;
    private final ModelGraphEdgeBuilder modelGraphEdgeBuilder;

    public ModelGraphBuilder(GraphTraversalSourceProvider graphTraversalSource, ModelVertexCustomizer modelVertexCustomizer, VertexResolver vertexResolver, ModelVertexInformationRetriever modelVertexInformationRetriever, ModelGraphEdgeBuilder modelGraphEdgeBuilder) {
        this.graphTraversalSource = graphTraversalSource;
        this.modelVertexCustomizer = modelVertexCustomizer;
        this.vertexResolver = vertexResolver;
        this.modelVertexInformationRetriever = modelVertexInformationRetriever;
        this.modelGraphEdgeBuilder = modelGraphEdgeBuilder;
    }

    @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true) // TODO refine
    public GraphTraversalSource modelGraphFromMetamodelGraph(org.jgrapht.Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        log.info("Creating model vertex");
        GraphTraversalSource gTS = this.graphTraversalSource.getGraph();
        createVertices(entityMetamodelGraph, gTS);
        createEdges(entityMetamodelGraph, gTS);
        return gTS;
    }

    private void createVertices(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, GraphTraversalSource graphTraversalSource) {
        log.info("Creating model Vertex");
        createVertices(entityMetamodelGraph.vertexSet(), graphTraversalSource);
    }

    void createEdges(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, GraphTraversalSource graphTraversalSource) {
        log.info("Creating model edges");
        modelGraphEdgeBuilder.createEdges(graphTraversalSource, entityMetamodelGraph);
    }

    private void createVertices(Set<MetamodelVertex> metamodelVertices, GraphTraversalSource modelGraph) {
        Collection<MetamodelVertexAndModelElementAndModelElementId> metamodelVertexAndModelElementAndModelElementIds = metamodelVertices.stream()
                .flatMap(mv -> modelVertexInformationRetriever.getMetamodelVertexAndModelElementAndModelElementIdStreamForMetamodelVertex(mv).stream()).collect(Collectors.toSet());
        Iterator<MetamodelVertexAndModelElementAndModelElementId> it = metamodelVertexAndModelElementAndModelElementIds.iterator();
        if (it.hasNext()) {
            MetamodelVertexAndModelElementAndModelElementId elt = it.next();
            String name = elt.metamodelVertex().getTypeName();
            GraphTraversal traversal = modelGraph
                    .addV(name);
            traversal = addVertexProperties(traversal, elt);
            while (it.hasNext()) {
                MetamodelVertexAndModelElementAndModelElementId eln = it.next();
                traversal.addV(eln.metamodelVertex().getTypeName());
                traversal = addVertexProperties(traversal, eln);
            }
            traversal.iterate();
        }
    }

    private GraphTraversal addVertexProperties(GraphTraversal traversal, MetamodelVertexAndModelElementAndModelElementId eln) {
        traversal = vertexResolver.setVertexModelElementId(traversal, eln.id());
        traversal = vertexResolver.setMetamodelVertex(traversal, eln.metamodelVertex());
        traversal = vertexResolver.setModelElement(traversal, eln.modelElement());
        for (Map.Entry<String, Object> entry : modelVertexCustomizer.getAdditionalModelVertexProperties(eln.metamodelVertex()).entrySet()) {
            traversal = traversal.property(entry.getKey(), entry.getValue());
        }
        return traversal;
    }
}
