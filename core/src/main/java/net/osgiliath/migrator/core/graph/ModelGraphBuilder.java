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
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.configuration.beans.GraphTraversalSourceProvider;
import net.osgiliath.migrator.core.graph.model.*;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
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

    public ModelGraphBuilder(RawElementProcessor elementHelper, EntityImporter entityImporter, GraphTraversalSourceProvider graphTraversalSource) {
        this.elementHelper = elementHelper;
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

    private void createVertices(Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph, GraphTraversalSource graphTraversalSource) {
        log.info("Creating model Vertex");
        createVertices(entityMetamodelGraph.vertexSet(), graphTraversalSource);
    }

    void createEdges(Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph, GraphTraversalSource graphTraversalSource) {
        log.info("Creating model edges");
        createEdges(graphTraversalSource, entityMetamodelGraph);
    }

    private Collection<Vertex> relatedVerticesOfOutgoingEdgeFromModelElementRelationship(Vertex modelVertex, FieldEdge edge, GraphTraversalSource modelGraph) {
        log.debug("looking for related vertices for edge {}", edge);
        Optional<EdgeTargetVertexOrVertices> targetModelElementsOpt = getEdgeValueFromVertexGraph(modelVertex, edge, modelGraph);
        return targetModelElementsOpt.map(targetModelElements -> switch (targetModelElements) {
                    case ManyEdgeTarget(Collection<Vertex> target) -> target;
                    case UnitaryEdgeTarget(Vertex target) -> {
                        Collection<Vertex> res = new ArrayList<>();
                        res.add(target);
                        yield res;
                    }
                }
        ).orElseGet(Collections::emptyList);
    }

    private List<Vertex> allVertices(GraphTraversalSource modelGraph) {
        GraphTraversal<Vertex, Vertex> entities = modelGraph.V();
        List<Vertex> list = entities.toList();
        return list;
    }

    private void createEdges(GraphTraversalSource modelGraph, org.jgrapht.Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph) {
        List<Vertex> list = allVertices(modelGraph);
        Stream<SourceVertexFieldEdgeAndTargetVertex> sourceVertexEdgeAndTargetVertexList = computeEdgesOfVertices(modelGraph, entityMetamodelGraph, list);
        sourceVertexEdgeAndTargetVertexList.forEach(sourceVertexEdgeAndTargetVertex ->
                sourceVertexEdgeAndTargetVertex
                        .sourceVertex()
                        .addEdge(sourceVertexEdgeAndTargetVertex.edge().getFieldName(), sourceVertexEdgeAndTargetVertex.targetVertex())
                        .property(MODEL_GRAPH_EDGE_METAMODEL_FIELD, sourceVertexEdgeAndTargetVertex.edge().getMetamodelField())
        );
    }

    private Stream<SourceVertexFieldEdgeAndTargetVertex> computeEdgesOfVertices(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph, List<Vertex> list) {
        Stream<SourceVertexFieldEdgeAndTargetVertex> sourceVertexEdgeAndTargetVertexList =
                list.stream().flatMap(v -> {
                            TinkerVertex modelVertex = (TinkerVertex) v;
                            MetamodelVertex metamodelVertex = v.value(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX);
                            log.info("looking for edges for vertex of type {} with id {}", metamodelVertex.getTypeName(), v.value(MODEL_GRAPH_VERTEX_ENTITY_ID));
                            Collection<FieldEdge> edges = metamodelVertex.getOutboundFieldEdges(entityMetamodelGraph);
                            return edges.stream().map(edge ->
                                    new FieldEdgeTargetVertices(edge, relatedVerticesOfOutgoingEdgeFromModelElementRelationship(modelVertex, edge, modelGraph))
                            ).map(edgeAndTargetVertex ->
                                    new SourceVertexFieldEdgeAndTargetVertices(modelVertex, edgeAndTargetVertex));
                        })
                        .flatMap(edgeAndTargetVertex -> edgeAndTargetVertex.targetVertices().stream().map(targetVertex -> new SourceVertexFieldEdgeAndTargetVertex(edgeAndTargetVertex, targetVertex)));
        return sourceVertexEdgeAndTargetVertexList;
    }


    private void createVertices(Set<MetamodelVertex> metamodelVertices, GraphTraversalSource modelGraph) {
        metamodelVertices.stream()
                .map(mv -> new MetamodelVertexAndModelElements(mv, entityImporter.importEntities(mv, new ArrayList<>())))
                .flatMap(mvaes -> mvaes.modelElements().stream().map(modelElement -> new MetamodelVertexAndModelElement(mvaes.metamodelVertex(), modelElement)))
                .flatMap(mvae -> mvae.modelElement().getId(mvae.metamodelVertex()).map(eid -> new MetamodelVertexAndModelElementAndModelElementId(mvae.metamodelVertex(), mvae.modelElement(), eid)).stream())
                .forEach(
                        mvaei -> {
                            GraphTraversal traversal = modelGraph
                                    .addV(mvaei.metamodelVertex().getTypeName())
                                    .property(MODEL_GRAPH_VERTEX_ENTITY_ID, mvaei.id())
                                    .property(MODEL_GRAPH_VERTEX_METAMODEL_VERTEX, mvaei.metamodelVertex())
                                    .property(MODEL_GRAPH_VERTEX_ENTITY, mvaei.modelElement());
                            mvaei.metamodelVertex().getAdditionalModelVertexProperties(mvaei.modelElement()).forEach((k, v) -> traversal.property(k, v));
                            traversal.next();
                        });
    }

    /**
     * get the target vertex or vertices corresponding to the entity referenced by the fieldEdge
     *
     * @param fieldEdge  the edge to get the target vertices from
     * @param modelGraph the model graph
     * @return the target Vertex or Vertices corresponding to the entities referenced by the fieldEdge
     */
    Optional<EdgeTargetVertexOrVertices> getEdgeValueFromVertexGraph(Vertex sourceVertex, FieldEdge fieldEdge, GraphTraversalSource modelGraph) {
        Method getterMethod = fieldEdge.relationshipGetter();
        MetamodelVertex targetVertex = fieldEdge.getTarget();
        log.debug("Getting Edge value from model element relationship. Relationship getter: {}, target of the edge: {}",
                fieldEdge.relationshipGetter().getName(),
                fieldEdge.getTarget().getTypeName());
        try {
            Object res = getterMethod.invoke(((ModelElement) sourceVertex.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY)).getRawElement());
            if (res instanceof Collection r) {
                log.trace("Target of the edge is a collection");
                Collection<Vertex> targets = (Collection<Vertex>) r.stream()
                        .flatMap(ent -> elementHelper.getId(((JpaMetamodelVertex) targetVertex).getEntityClass(), ent).stream())
                        .peek(id -> log.debug("Trying to seek for an existing vertex element with id: {} from the original collection", id))
                        .flatMap(id -> modelGraph.V().hasLabel(targetVertex.getTypeName())
                                .has(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID, id).toStream()).collect(Collectors.toSet());
                return Optional.of(new ManyEdgeTarget(targets));
            } else if (res != null) {
                log.trace("Target of the edge is a single element");
                return elementHelper.getId(((JpaMetamodelVertex) targetVertex).getEntityClass(), res)
                        .map(id -> {
                            log.debug("Trying to seek for an existing vertex element with id: {}", id);
                            return modelGraph.V().hasLabel(targetVertex.getTypeName())
                                    .has(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID, id).next();
                        }).map(UnitaryEdgeTarget::new);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }
}
