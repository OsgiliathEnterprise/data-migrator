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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Set;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;

@Component
public class TinkerpopModelGraphBuilder implements ModelGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(TinkerpopModelGraphBuilder.class);
    private final GraphTraversalSourceProvider graphTraversalSource;
    private final ModelVertexCustomizer modelVertexCustomizer;
    private final VertexResolver vertexResolver;
    private final ModelVertexInformationRetriever modelVertexInformationRetriever;
    private final ModelGraphEdgeBuilder modelGraphEdgeBuilder;
    private final PlatformTransactionManager sourcePlatformTxManager;

    public TinkerpopModelGraphBuilder(GraphTraversalSourceProvider graphTraversalSource, ModelVertexCustomizer modelVertexCustomizer, VertexResolver vertexResolver, ModelVertexInformationRetriever modelVertexInformationRetriever, ModelGraphEdgeBuilder modelGraphEdgeBuilder, @Qualifier(SOURCE_TRANSACTION_MANAGER) PlatformTransactionManager sourcePlatformTxManager) {
        this.graphTraversalSource = graphTraversalSource;
        this.modelVertexCustomizer = modelVertexCustomizer;
        this.vertexResolver = vertexResolver;
        this.modelVertexInformationRetriever = modelVertexInformationRetriever;
        this.modelGraphEdgeBuilder = modelGraphEdgeBuilder;
        this.sourcePlatformTxManager = sourcePlatformTxManager;
    }

    // @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    public GraphTraversalSource modelGraphFromMetamodelGraph(org.jgrapht.Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        log.info("Creating model Vertices");
        GraphTraversalSource gTS = this.graphTraversalSource.getGraph();
        TransactionTemplate tpl = new TransactionTemplate(sourcePlatformTxManager);
        tpl.setReadOnly(true);
        tpl.executeWithoutResult(status -> { // TODO refine
            createVertices(entityMetamodelGraph, gTS);
            createEdges(entityMetamodelGraph, gTS);
        });
        return gTS;
    }

    private void createVertices(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, GraphTraversalSource graphTraversalSource) {
        log.info("Creating model Vertex");
        /*TransactionTemplate tpl = new TransactionTemplate(sourcePlatformTxManager);
        tpl.setReadOnly(true);
        tpl.executeWithoutResult(status ->*/
        createVertices(entityMetamodelGraph.vertexSet(), graphTraversalSource);
        //);
    }

    void createEdges(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, GraphTraversalSource graphTraversalSource) {
        log.info("Creating model edges");
        /*TransactionTemplate tpl = new TransactionTemplate(sourcePlatformTxManager);
        tpl.setReadOnly(true);
        tpl.executeWithoutResult(status ->*/
        modelGraphEdgeBuilder.createEdges(graphTraversalSource, entityMetamodelGraph);
        //);
    }

    private void createVertices(Set<MetamodelVertex> metamodelVertices, GraphTraversalSource modelGraph) {
        GraphTraversal metamodelVertexAndModelElementAndModelElementIds = metamodelVertices.stream()
                .flatMap(mv -> modelVertexInformationRetriever.getMetamodelVertexAndModelElementAndModelElementIdStreamForMetamodelVertex(mv))
                .reduce(modelGraph.inject(0), (GraphTraversal traversal, MetamodelVertexAndModelElementAndModelElementId elt) -> {
                            String name = elt.metamodelVertex().getTypeName();
                            traversal = traversal.addV(name);
                            traversal = addVertexProperties(traversal, elt);
                            return traversal;
                        }, GraphTraversal::combine
                );
        metamodelVertexAndModelElementAndModelElementIds.iterate();
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
