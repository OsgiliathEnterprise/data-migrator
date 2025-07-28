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
import net.osgiliath.migrator.core.graph.model.*;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;
import static net.osgiliath.migrator.core.graph.ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID;

@Component
public class TinkerpopModelGraphEdgeBuilder implements ModelGraphEdgeBuilder {

    private static final Logger log = LoggerFactory.getLogger(TinkerpopModelGraphEdgeBuilder.class);
    private final MetamodelRequester metamodelGraphRequester;
    private final VertexResolver vertexResolver;
    private final PlatformTransactionManager sourcePlatformTxManager;
    private final RawElementProcessor elementHelper;
    private final ModelElementProcessor modelElementProcessor;

    public TinkerpopModelGraphEdgeBuilder(RawElementProcessor rawElementProcessor, MetamodelRequester metamodelGraphRequester, VertexResolver vertexResolver, ModelElementProcessor modelElementProcessor, @Qualifier(SOURCE_TRANSACTION_MANAGER) PlatformTransactionManager sourcePlatformTxManager) {
        this.elementHelper = rawElementProcessor;
        this.modelElementProcessor = modelElementProcessor;
        this.metamodelGraphRequester = metamodelGraphRequester;
        this.vertexResolver = vertexResolver;
        this.sourcePlatformTxManager = sourcePlatformTxManager;
    }

    // @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    @Override
    public void createEdges(GraphTraversalSource modelGraph, org.jgrapht.Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        TransactionTemplate tpl = new TransactionTemplate(sourcePlatformTxManager);
        tpl.setReadOnly(true);
        GraphTraversal sourceVertexEdgeAndTargetVertexList = tpl.execute(status ->  // TODO refine
                computeEdgesOfVertices(modelGraph, entityMetamodelGraph)
                        .reduce(modelGraph.inject(0), (
                                        GraphTraversal t1, SourceVertexFieldEdgeAndTargetVertex elt
                                ) ->
                                        t1.V(elt
                                                        .sourceVertex())
                                                .addE(elt.edge().getFieldName())
                                                .to(elt.targetVertex())
                                                .property(MODEL_GRAPH_EDGE_METAMODEL_FIELD, elt.edge().getMetamodelField().getName())
                                , GraphTraversal::concat)
        );
        sourceVertexEdgeAndTargetVertexList.iterate();
    }

    private Stream<SourceVertexFieldEdgeAndTargetVertex> computeEdgesOfVertices(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        // ATTENTION : modelGraph.V().toStream() peut charger tous les sommets en mémoire selon l'implémentation.
        // Privilégiez une itération paresseuse ou paginée si le graphe est volumineux.
        return modelGraph.V().toStream().flatMap(v -> {
                    MetamodelVertex metamodelVertex = vertexResolver.getMetamodelVertex(v);
                    log.info("looking for edges for vertex of type {} with id {}", metamodelVertex.getTypeName(), vertexResolver.getVertexModelElementId(v));
                    Collection<FieldEdge<MetamodelVertex>> edges = metamodelGraphRequester.getOutboundFieldEdges(metamodelVertex, entityMetamodelGraph);
                    return edges.stream().map(edge ->
                            new FieldEdgeTargetVertices(edge, relatedVerticesOfOutgoingEdgeFromModelElementRelationship(v, edge, modelGraph))
                    ).map(edgeAndTargetVertex ->
                            new SourceVertexFieldEdgeAndTargetVertices(v, edgeAndTargetVertex));
                })
                .flatMap(edgeAndTargetVertex -> edgeAndTargetVertex.targetVertices().map(targetVertex -> new SourceVertexFieldEdgeAndTargetVertex(edgeAndTargetVertex, targetVertex)));
    }

    private Stream<Vertex> relatedVerticesOfOutgoingEdgeFromModelElementRelationship(Vertex modelVertex, FieldEdge<MetamodelVertex> edge, GraphTraversalSource modelGraph) {
        log.debug("Looking for related vertices for edge {}", edge);
        Optional<EdgeTargetVertexOrVertices> targetModelElementsOpt = getEdgeValueFromVertexGraph(modelVertex, edge, modelGraph);
        return targetModelElementsOpt.map(targetModelElements -> switch (targetModelElements) {
                    case ManyEdgeTarget(Stream<Vertex> target) -> target;
                    case UnitaryEdgeTarget(Vertex target) -> Stream.of(target);
                }
        ).orElseGet(Stream::empty);
    }

    /**
     * get the target vertex or vertices corresponding to the entity referenced by the outboundEdge
     *
     * @param fieldEdge  the edge to get the target vertices from
     * @param modelGraph the model graph
     * @return the target Vertex or Vertices corresponding to the entities referenced by the outboundEdge
     */
    @SuppressWarnings("java:S3864")
    Optional<EdgeTargetVertexOrVertices> getEdgeValueFromVertexGraph(Vertex sourceVertex, FieldEdge<MetamodelVertex> fieldEdge, GraphTraversalSource modelGraph) {
        MetamodelVertex targetVertex = fieldEdge.getTarget();
        log.debug("Getting Edge value from model element relationship. Relationship getter: {}, target of the edge: {}",
                fieldEdge.getFieldName(),
                fieldEdge.getTarget().getTypeName());
        Object res = modelElementProcessor.getEdgeRawValue(fieldEdge, vertexResolver.getModelElement(sourceVertex));
        if (res instanceof Collection r) {
            log.trace("Target of the edge is a collection");
            Stream<Vertex> targets = r.stream()
                    .flatMap(ent -> elementHelper.getId(targetVertex, ent).stream())
                    .map(vertexResolver::getWrappedRawId)
                    .peek(id -> log.debug("Trying to seek for an existing vertex element with id: {} from the original collection", id))
                    .flatMap(id -> modelGraph.V().hasLabel(targetVertex.getTypeName())
                            .has(MODEL_GRAPH_VERTEX_ENTITY_ID, id).toStream());
            return Optional.of(new ManyEdgeTarget(targets));
        } else if (res != null) {
            log.trace("Target of the edge is a single element");
            return elementHelper.getId(targetVertex, res)
                    .map(vertexResolver::getWrappedRawId)
                    .map(id -> {
                        log.debug("Trying to seek for an existing vertex element with id: {}", id);
                        return modelGraph.V().hasLabel(targetVertex.getTypeName())
                                .has(MODEL_GRAPH_VERTEX_ENTITY_ID, id).next();
                    }).map(UnitaryEdgeTarget::new);
        }
        return Optional.empty();
    }
}


