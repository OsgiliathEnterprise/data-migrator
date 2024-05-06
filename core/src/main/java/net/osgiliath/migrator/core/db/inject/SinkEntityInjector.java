package net.osgiliath.migrator.core.db.inject;

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
import net.osgiliath.migrator.core.db.inject.model.ModelAndMetamodelEdge;
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import net.osgiliath.migrator.core.graph.ModelGraphBuilder;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerVertex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.apache.tinkerpop.gremlin.process.traversal.P.eq;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

@Component
public class SinkEntityInjector {
    private static final Logger log = LoggerFactory.getLogger(SinkEntityInjector.class);
    public static final Integer CYCLE_DETECTION_DEPTH = 30;
    private final VertexPersister vertexPersister;
    private final MetamodelRequester metamodelGraphRequester;
    private final ModelElementProcessor modelElementProcessor;

    public SinkEntityInjector(VertexPersister vertexPersister, MetamodelRequester metamodelGraphRequester, ModelElementProcessor modelElementProcessor) {
        super();
        this.vertexPersister = vertexPersister;
        this.metamodelGraphRequester = metamodelGraphRequester;
        this.modelElementProcessor = modelElementProcessor;
    }

    public void persist(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        log.info("Least connected vertex are ordered, starting the import");
        removeCyclicElements(modelGraph);
        processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph, new HashSet<>());
    }

    private void processEntitiesWithoutCycles(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, Collection<Vertex> processedVertices) {
        GraphTraversal leafElements = modelGraph.V()
                .repeat(out())
                .until(
                        out().filter(__.not(is(P.within(processedVertices)))).count().is(0)// .or().loops().is(CYCLE_DETECTION_DEPTH)
                )
                .filter(__.not(is(P.within(processedVertices))));
        if (!leafElements.hasNext()) {
            vertexPersister.persistVertices(modelGraph.V().filter(__.not(is(P.within(processedVertices)))).toStream()
                    .map(modelVertex -> modelVertex.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY)));
            return;
        }
        Stream<TinkerVertex> res = leafElements.toStream()
                .map(e -> {
                    TinkerVertex modelVertex = (TinkerVertex) e;
                    updateRawElementRelationshipsAccordingToGraphEdges(modelVertex, entityMetamodelGraph);
                    return modelVertex;
                })
                .peek(mv -> {
                    TinkerVertex tv = (TinkerVertex) mv;
                    log.info("Persisting vertex of type {} with id {}", tv.label(), tv.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID));
                });
        vertexPersister.persistVertices(res
                .map(modelVertex -> {
                    processedVertices.add(modelVertex);
                    return modelVertex;
                })
                .map(modelVertex -> modelVertex.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY)));
        processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph, processedVertices);
    }

    void updateRawElementRelationshipsAccordingToGraphEdges(TinkerVertex sourceVertex, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        ModelElement sourceModelElement = (ModelElement) sourceVertex.values(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY).next();
        MetamodelVertex sourceMetamodelVertex = (MetamodelVertex) sourceVertex.values(ModelGraphBuilder.MODEL_GRAPH_VERTEX_METAMODEL_VERTEX).next();
        metamodelGraphRequester.getOutboundFieldEdges(sourceMetamodelVertex, entityMetamodelGraph).stream()
                .map(metamodelEdge -> {
                    modelElementProcessor.resetModelElementEdge(metamodelEdge, sourceModelElement);
                    return metamodelEdge;
                })
                .flatMap(metamodelEdge ->
                        StreamSupport.stream(Spliterators.spliteratorUnknownSize(sourceVertex.edges(Direction.OUT, metamodelEdge.getFieldName()), 0), false)
                                .map(modelEdge -> new ModelAndMetamodelEdge(modelEdge, metamodelEdge))
                )
                .peek(modelAndMetamodelEdge -> log.info("Recomposing edge: {} between source vertex of type {} with id {} and target vertex of type {} and id {}", modelAndMetamodelEdge.modelEdge().label(), sourceVertex.label(), sourceVertex.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID), modelAndMetamodelEdge.modelEdge().inVertex().label(), modelAndMetamodelEdge.modelEdge().inVertex().value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID)))
                .forEach(modelAndMetamodelEdge -> {
                    ModelElement targetModelElement = (ModelElement) modelAndMetamodelEdge.modelEdge().inVertex().values(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY).next();
                    modelElementProcessor.addRawElementsRelationshipForEdge(modelAndMetamodelEdge.metamodelEdge(), sourceModelElement, targetModelElement, entityMetamodelGraph);
                });
    }


    private void removeCyclicElements(GraphTraversalSource modelGraph) {
        GraphTraversal cyclicElements = modelGraph.V().as("a")
                .repeat(out())
                .until(where(eq("a"))
                        .or().loops().is(CYCLE_DETECTION_DEPTH))
                .filter(__.where(eq("a")));
        cyclicElements.toStream()
                .peek(v -> {
                    TinkerVertex ve = (TinkerVertex) v;
                    log.warn("Cyclic element of type {} with id {} found in the graph", ve.label(), ve.values(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID).next());
                })
                .forEach(v ->
                        modelGraph.V(v).drop().iterate()
                );
    }
}
