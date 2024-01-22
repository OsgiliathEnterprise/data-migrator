package net.osgiliath.migrator.core.modelgraph;

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
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.configuration.beans.GraphTraversalSourceProvider;
import net.osgiliath.migrator.core.modelgraph.model.*;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerVertex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;

@Component
public class ModelGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(ModelGraphBuilder.class);
    public static final String MODEL_GRAPH_VERTEX_ENTITY_ID = "id";
    public static final String MODEL_GRAPH_VERTEX_METAMODEL_VERTEX = "metamodelVertex";
    public static final String MODEL_GRAPH_VERTEX_ENTITY = "entity";
    public static final String MODEL_GRAPH_EDGE_METAMODEL_FIELD = "field";
    public static final String MODEL_GRAPH_EDGE_METAMODEL_FIELD_NAME = "field_name";
    private final EntityImporter entityImporter;
    private final GraphTraversalSourceProvider graphTraversalSource;

    public ModelGraphBuilder(EntityImporter entityImporter, GraphTraversalSourceProvider graphTraversalSource) {
        this.entityImporter = entityImporter;
        this.graphTraversalSource = graphTraversalSource;
    }

    @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    public GraphTraversalSource modelGraphFromMetamodelGraph(org.jgrapht.Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        log.info("Creating model vertex");
        GraphTraversalSource gTS = this.graphTraversalSource.getGraph();
        createVertices(entityMetamodelGraph, gTS);
        createEdges(entityMetamodelGraph, gTS);
        return gTS;
    }

    private void createEdges(Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph, GraphTraversalSource graphTraversalSource) {
        log.info("Creating model edges");
        createEdges(graphTraversalSource, entityMetamodelGraph);
    }

    public void createVertices(Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph, GraphTraversalSource graphTraversalSource) {
        log.info("Creating model Vertex");
        createVertices(entityMetamodelGraph.vertexSet(), graphTraversalSource);
    }

    private void createEdges(GraphTraversalSource modelGraph, org.jgrapht.Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        GraphTraversal<Vertex, Vertex> entities = modelGraph.V();
        List<Vertex> list = entities.toList();
        list.stream().flatMap(v -> {
                    TinkerVertex modelVertex = (TinkerVertex) v;
                    MetamodelVertex metamodelVertex = v.value(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX);
                    log.info("looking for edges for vertex of type {} with id {}", metamodelVertex.getTypeName(), v.value(MODEL_GRAPH_VERTEX_ENTITY_ID));
                    Collection<FieldEdge> edges = metamodelVertex.getOutboundFieldEdges(entityMetamodelGraph).stream().toList();
                    return edges.stream().map(edge ->
                            new FieldEdgeTargetVertices(edge, relatedVerticesOfOutgoingEdgeFromModelElementRelationship(modelVertex, edge, modelGraph))
                    ).map(edgeAndTargetVertex ->
                            new SourceVertexFieldEdgeAndTargetVertices(modelVertex, edgeAndTargetVertex));
                })
                .flatMap(edgeAndTargetVertex -> edgeAndTargetVertex.getTargetVertices().stream().map(targetVertex -> new SourceVertexEdgeAndTargetVertex(edgeAndTargetVertex, targetVertex)))
                .forEach(sourceVertexEdgeAndTargetVertex ->
                        sourceVertexEdgeAndTargetVertex.getSourceVertex().addEdge(sourceVertexEdgeAndTargetVertex.getEdge().getFieldName(), sourceVertexEdgeAndTargetVertex.getTargetVertex()).property(MODEL_GRAPH_EDGE_METAMODEL_FIELD, sourceVertexEdgeAndTargetVertex.getEdge().getMetamodelField())
                );
    }

    private Collection<Vertex> relatedVerticesOfOutgoingEdgeFromModelElementRelationship(Vertex modelVertex, FieldEdge edge, GraphTraversalSource modelGraph) {
        log.debug("looking for related vertices for edge {}", edge);
        ModelElement modelElement = modelVertex.value(MODEL_GRAPH_VERTEX_ENTITY);
        Optional<Object> targetModelElementsOpt = modelElement.getEdgeValueFromModelElementRelationShip(edge, modelGraph);
        return targetModelElementsOpt.map(targetModelElements -> {
            if (targetModelElements instanceof Collection) {
                return ((Collection<Vertex>) targetModelElements);
            } else {
                Collection<Vertex> res = new ArrayList<>();
                res.add((Vertex) targetModelElements);
                return res;
            }
        }).orElse(Collections.emptyList());
    }

    private void createVertices(Set<MetamodelVertex> metamodelVertices, GraphTraversalSource modelGraph) {
        metamodelVertices.stream()
                .map(mv -> new MetamodelVertexAndModelElements(mv, entityImporter.importEntities(mv, new ArrayList<>())))
                .flatMap(mvaes -> mvaes.getEntities().stream().map(entity -> new MetamodelVertexAndModelElement(mvaes.getMetamodelVertex(), entity)))
                .flatMap(mvae -> mvae.getModelElement().getId(mvae.getMetamodelVertex()).map(eid -> new MetamodelVertexAndModelElementAndModelElementId(mvae.getMetamodelVertex(), mvae.getModelElement(), eid)).stream())
                .forEach(
                        mvaei -> {
                            GraphTraversal traversal = modelGraph
                                    .addV(mvaei.getMetamodelVertex().getTypeName())
                                    .property(MODEL_GRAPH_VERTEX_ENTITY_ID, mvaei.getId())
                                    .property(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX, mvaei.getMetamodelVertex())
                                    .property(MODEL_GRAPH_VERTEX_ENTITY, mvaei.getModelElement());
                            mvaei.getMetamodelVertex().getAdditionalModelVertexProperties(mvaei.getModelElement()).forEach((k, v) -> traversal.property(k, v));
                            traversal.next();
                        });
    }
}
