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
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.configuration.ModelVertexCustomizer;
import net.osgiliath.migrator.core.configuration.beans.GraphTraversalSourceProvider;
import net.osgiliath.migrator.core.graph.model.*;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerVertex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;

@Component
public class ModelGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(ModelGraphBuilder.class);
    public static final String MODEL_GRAPH_VERTEX_ENTITY_ID = "id";
    public static final String MODEL_GRAPH_VERTEX_METAMODEL_VERTEX = "metamodelVertex";
    public static final String MODEL_GRAPH_VERTEX_ENTITY = "entity";
    public static final String MODEL_GRAPH_EDGE_METAMODEL_FIELD = "field";
    public static final String MODEL_GRAPH_EDGE_METAMODEL_FIELD_NAME = "field_name";
    private final RawElementProcessor elementHelper;
    private final EntityImporter entityImporter;
    private final GraphTraversalSourceProvider graphTraversalSource;
    private final MetamodelRequester metamodelGraphRequester;
    private final ModelElementProcessor modelElementProcessor;
    private final ModelVertexCustomizer modelVertexCustomizer;

    public ModelGraphBuilder(RawElementProcessor rawElementProcessor, EntityImporter entityImporter, GraphTraversalSourceProvider graphTraversalSource, MetamodelRequester metamodelGraphRequester, ModelElementProcessor modelElementProcessor, ModelVertexCustomizer modelVertexCustomizer) {
        this.elementHelper = rawElementProcessor;
        this.entityImporter = entityImporter;
        this.graphTraversalSource = graphTraversalSource;
        this.metamodelGraphRequester = metamodelGraphRequester;
        this.modelElementProcessor = modelElementProcessor;
        this.modelVertexCustomizer = modelVertexCustomizer;
    }

    @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
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
        createEdges(graphTraversalSource, entityMetamodelGraph);
    }

    private Stream<Vertex> relatedVerticesOfOutgoingEdgeFromModelElementRelationship(Vertex modelVertex, FieldEdge<MetamodelVertex> edge, GraphTraversalSource modelGraph) {
        log.debug("looking for related vertices for edge {}", edge);
        Optional<EdgeTargetVertexOrVertices> targetModelElementsOpt = getEdgeValueFromVertexGraph(modelVertex, edge, modelGraph);
        return targetModelElementsOpt.map(targetModelElements -> switch (targetModelElements) {
                    case ManyEdgeTarget(Stream<Vertex> target) -> target;
                    case UnitaryEdgeTarget(Vertex target) -> Stream.of(target);
                }
        ).orElseGet(Stream::empty);
    }

    private void createEdges(GraphTraversalSource modelGraph, org.jgrapht.Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        Stream<SourceVertexFieldEdgeAndTargetVertex> sourceVertexEdgeAndTargetVertexList = computeEdgesOfVertices(modelGraph, entityMetamodelGraph);
        sourceVertexEdgeAndTargetVertexList.forEach(sourceVertexEdgeAndTargetVertex ->
                sourceVertexEdgeAndTargetVertex
                        .sourceVertex()
                        .addEdge(sourceVertexEdgeAndTargetVertex.edge().getFieldName(), sourceVertexEdgeAndTargetVertex.targetVertex())
                        .property(MODEL_GRAPH_EDGE_METAMODEL_FIELD, sourceVertexEdgeAndTargetVertex.edge().getMetamodelField())
        );
    }

    private Stream<SourceVertexFieldEdgeAndTargetVertex> computeEdgesOfVertices(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        return modelGraph.V().toStream().flatMap(v -> {
                    TinkerVertex modelVertex = (TinkerVertex) v;
                    MetamodelVertex metamodelVertex = v.value(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX);
                    log.info("looking for edges for vertex of type {} with id {}", metamodelVertex.getTypeName(), v.value(MODEL_GRAPH_VERTEX_ENTITY_ID));
                    Collection<FieldEdge<MetamodelVertex>> edges = metamodelGraphRequester.getOutboundFieldEdges(metamodelVertex, entityMetamodelGraph);
                    return edges.stream().map(edge ->
                            new FieldEdgeTargetVertices(edge, relatedVerticesOfOutgoingEdgeFromModelElementRelationship(modelVertex, edge, modelGraph))
                    ).map(edgeAndTargetVertex ->
                            new SourceVertexFieldEdgeAndTargetVertices(modelVertex, edgeAndTargetVertex));
                })
                .flatMap(edgeAndTargetVertex -> edgeAndTargetVertex.targetVertices().map(targetVertex -> new SourceVertexFieldEdgeAndTargetVertex(edgeAndTargetVertex, targetVertex)));
    }


    private void createVertices(Set<MetamodelVertex> metamodelVertices, GraphTraversalSource modelGraph) {
        metamodelVertices.stream()
                .map(mv -> new MetamodelVertexAndModelElements(mv, entityImporter.importEntities(mv, new ArrayList<>())))
                .flatMap(mvaes -> mvaes.modelElements().map(modelElement -> new MetamodelVertexAndModelElement(mvaes.metamodelVertex(), modelElement)))
                .flatMap(mvae -> modelElementProcessor.getId(mvae.modelElement()).map(eid -> new MetamodelVertexAndModelElementAndModelElementId(mvae.metamodelVertex(), mvae.modelElement(), eid)).stream())
                .forEach(
                        mvaei -> {
                            GraphTraversal traversal = modelGraph
                                    .addV(mvaei.metamodelVertex().getTypeName())
                                    .property(MODEL_GRAPH_VERTEX_ENTITY_ID, mvaei.id())
                                    .property(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX, mvaei.metamodelVertex())
                                    .property(MODEL_GRAPH_VERTEX_ENTITY, mvaei.modelElement());
                            modelVertexCustomizer.getAdditionalModelVertexProperties(mvaei.metamodelVertex()).forEach(traversal::property);
                            traversal.next();
                        });
    }

    /**
     * get the target vertex or vertices corresponding to the entity referenced by the outboundEdge
     *
     * @param fieldEdge  the edge to get the target vertices from
     * @param modelGraph the model graph
     * @return the target Vertex or Vertices corresponding to the entities referenced by the outboundEdge
     */
    Optional<EdgeTargetVertexOrVertices> getEdgeValueFromVertexGraph(Vertex sourceVertex, FieldEdge<MetamodelVertex> fieldEdge, GraphTraversalSource modelGraph) {
        MetamodelVertex targetVertex = fieldEdge.getTarget();
        log.debug("Getting Edge value from model element relationship. Relationship getter: {}, target of the edge: {}",
                fieldEdge.getFieldName(),
                fieldEdge.getTarget().getTypeName());
        Object res = modelElementProcessor.getEdgeRawValue(fieldEdge, sourceVertex.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY));
        if (res instanceof Collection r) {
            log.trace("Target of the edge is a collection");
            Stream<Vertex> targets = r.stream()
                    .flatMap(ent -> elementHelper.getId(targetVertex, ent).stream())
                    .peek(id -> log.debug("Trying to seek for an existing vertex element with id: {} from the original collection", id))
                    .flatMap(id -> modelGraph.V().hasLabel(targetVertex.getTypeName())
                            .has(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID, id).toStream());
            return Optional.of(new ManyEdgeTarget(targets));
        } else if (res != null) {
            log.trace("Target of the edge is a single element");
            return elementHelper.getId(targetVertex, res)
                    .map(id -> {
                        log.debug("Trying to seek for an existing vertex element with id: {}", id);
                        return modelGraph.V().hasLabel(targetVertex.getTypeName())
                                .has(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID, id).next();
                    }).map(UnitaryEdgeTarget::new);
        }
        return Optional.empty();
    }
}
