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

import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.configuration.beans.GraphTraversalSourceProvider;
import net.osgiliath.migrator.core.metamodel.helper.MetamodelVertexHelper;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.modelgraph.model.*;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

@Component
public class ModelGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(ModelGraphBuilder.class);
    public static final String MODEL_GRAPH_VERTEX_ENTITY_ID = "id";
    public static final String MODEL_GRAPH_VERTEX_METAMODEL_VERTEX = "metamodelVertex";
    public static final String MODEL_GRAPH_VERTEX_ENTITY = "entity";
    public static final String MODEL_GRAPH_EDGE_METAMODEL_FIELD = "field";
    public static final String MODEL_GRAPH_EDGE_METAMODEL_FIELD_NAME = "field_name";
    private final EntityImporter entityImporter;
    private final MetamodelVertexHelper metamodelVertexHelper;
    private final GraphTraversalSourceProvider graphTraversalSource;

    public ModelGraphBuilder(EntityImporter entityImporter, MetamodelVertexHelper metamodelVertexHelper, GraphTraversalSourceProvider graphTraversalSource) {
        this.entityImporter = entityImporter;
        this.metamodelVertexHelper = metamodelVertexHelper;
        this.graphTraversalSource = graphTraversalSource;
    }

    @Transactional(transactionManager = "sourceTransactionManager", readOnly = true)
    public GraphTraversalSource modelGraphFromMetamodelGraph(org.jgrapht.Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        log.info("Creating model vertex");
        GraphTraversalSource graphTraversalSource = this.graphTraversalSource.getGraph();
        createVertices(entityMetamodelGraph.vertexSet(), graphTraversalSource);
        log.info("Creating model edges");
        createEdges(graphTraversalSource, entityMetamodelGraph);
        return graphTraversalSource;
    }

    private void createEdges(GraphTraversalSource modelGraph, org.jgrapht.Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        GraphTraversal<Vertex, Vertex> entities = modelGraph.V();
        entities.toList().stream().flatMap(v -> {
            log.info("looking for edges for vertex of type {} with id {}", v.values(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX).next(), v.values(MODEL_GRAPH_VERTEX_ENTITY_ID).next());
            TinkerVertex modelVertex = (TinkerVertex) v;
            MetamodelVertex metamodelVertex = (MetamodelVertex) modelVertex.values(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX).next();
            Collection<FieldEdge> edges = entityMetamodelGraph.outgoingEdgesOf(metamodelVertex);
            return edges.stream().map(edge ->
                new EdgeTargetVertices(edge, relatedVerticesOfOutgoingEdge(modelVertex, edge, metamodelVertex, modelGraph, entityMetamodelGraph))
            ).map(edgeAndTargetVertex ->
                new SourceVertexEdgeAndTargetVertices(modelVertex, edgeAndTargetVertex));
        }).flatMap(edgeAndTargetVertex -> edgeAndTargetVertex.getTargetVertices().stream().map(targetVertex -> new SourceVertexEdgeAndTargetVertex(edgeAndTargetVertex, targetVertex)))
        .forEach(sourceVertexEdgeAndTargetVertex ->
            sourceVertexEdgeAndTargetVertex.getSourceVertex().addEdge(sourceVertexEdgeAndTargetVertex.getEdge().getFieldName(), sourceVertexEdgeAndTargetVertex.getTargetVertex()).property(MODEL_GRAPH_EDGE_METAMODEL_FIELD, sourceVertexEdgeAndTargetVertex.getEdge().getMetamodelField())
        );
    }

    private Collection<Vertex> relatedVerticesOfOutgoingEdge(TinkerVertex modelVertex, FieldEdge edge, MetamodelVertex metamodelVertex, GraphTraversalSource modelGraph, org.jgrapht.Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        log.debug("looking for related vertices for edge {}", edge);
        Method relationshipGetterMethod = metamodelVertex.relationshipGetter(edge);
        try {
            Object targetEntities = relationshipGetterMethod.invoke(modelVertex.values(MODEL_GRAPH_VERTEX_ENTITY).next());
            if (targetEntities instanceof Collection) {
               return ((Collection<?>) targetEntities).stream().map(targetEntity ->
                    targetEdgeVertexOrEmpty(edge, getTargetEntityId(edge, targetEntity, entityMetamodelGraph), modelGraph, entityMetamodelGraph)
                ).filter(Optional::isPresent).map(Optional::get).toList();
            } else {
                if (null != targetEntities) {
                    return Stream.of(targetEdgeVertexOrEmpty(edge, getTargetEntityId(edge, targetEntities, entityMetamodelGraph), modelGraph, entityMetamodelGraph))
                            .filter(Optional::isPresent).map(Optional::get).toList();
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return Collections.EMPTY_LIST;
    }

    private Object getTargetEntityId(FieldEdge edge, Object targetEntity, org.jgrapht.Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        return metamodelVertexHelper.getTargetMetamodelVertex(entityMetamodelGraph, edge).getId(targetEntity);
    }

    private Optional<Vertex> targetEdgeVertexOrEmpty(FieldEdge edge, Object targetEntityId, GraphTraversalSource modelGraph, org.jgrapht.Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        if (null != targetEntityId) {
            return Optional.of(targetEdgeVertex(edge, targetEntityId, modelGraph, entityMetamodelGraph));
        }
        return Optional.empty();
    }

    private Vertex targetEdgeVertex(FieldEdge edge, Object relatedEntityId, GraphTraversalSource modelGraph, org.jgrapht.Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        String targetVertexType = entityMetamodelGraph.getEdgeTarget(edge).getTypeName();
        log.debug("looking for related target vertex type {} with id value {}", targetVertexType, relatedEntityId);
        return modelGraph.V()
            .has(targetVertexType,
                MODEL_GRAPH_VERTEX_ENTITY_ID,
                relatedEntityId)
            .property(MODEL_GRAPH_EDGE_METAMODEL_FIELD, edge.getMetamodelField())
                .next();
    }

    private void createVertices(Set<MetamodelVertex> metamodelVertices, GraphTraversalSource modelGraph) {
        metamodelVertices.stream()
            .map(mv -> new MetamodelVertexAndEntities(mv, entityImporter.importEntities(mv, new ArrayList<>())))
            .flatMap(mvae -> mvae.getEntities().stream().map(entity -> new ModelVertex(mvae.getMetamodelVertex(), entity)))
            .forEach(
                mvae -> {
                    Object entityId = mvae.getMetamodelVertex().getId(mvae.getEntity());
                    GraphTraversal traversal = modelGraph
                        .addV(mvae.getMetamodelVertex().getTypeName())
                        .property(MODEL_GRAPH_VERTEX_ENTITY_ID, entityId)
                        .property(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX, mvae.getMetamodelVertex())
                        .property(MODEL_GRAPH_VERTEX_ENTITY, mvae.getEntity());
                    mvae.getMetamodelVertex().getAdditionalModelVertexProperties(mvae.getEntity()).forEach((k, v) -> traversal.property(k, v));
                    traversal.next();
                });
    }
}
