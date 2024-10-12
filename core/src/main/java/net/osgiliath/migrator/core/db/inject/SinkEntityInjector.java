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
import net.osgiliath.migrator.core.graph.VertexResolver;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
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
    private final VertexResolver vertexResolver;

    public SinkEntityInjector(VertexPersister vertexPersister, MetamodelRequester metamodelGraphRequester, ModelElementProcessor modelElementProcessor, VertexResolver vertexResolver) {
        super();
        this.vertexPersister = vertexPersister;
        this.metamodelGraphRequester = metamodelGraphRequester;
        this.modelElementProcessor = modelElementProcessor;
        this.vertexResolver = vertexResolver;
    }

    public void persist(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        log.info("Least connected vertex are ordered, starting the import");
        removeCyclicElements(modelGraph);
        processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph, new HashSet<>());
    }

    private void processEntitiesWithoutCycles(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, Collection<Vertex> processedVertices) {
        GraphTraversal leafElements = modelGraph
                .V()
                .repeat(out())
                .until(
                        out().filter(not(is(P.within(processedVertices)))).count().is(0)
                ).filter(not(is(P.within(processedVertices))));// .or().loops().is(CYCLE_DETECTION_DEPTH)
        if (leafElements.hasNext()) {
            persistTraversal(entityMetamodelGraph, processedVertices, leafElements);
            processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph, processedVertices);
        } else {
            GraphTraversal orphansElements = modelGraph.V().filter(not(is(P.within(processedVertices)))).filter(out().count().is(0));
            if (orphansElements.hasNext()) {
                persistTraversal(entityMetamodelGraph, processedVertices, orphansElements);
                processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph, processedVertices);
            } else {
                persistTraversal(entityMetamodelGraph, processedVertices, modelGraph.V().filter(not(is(P.within(processedVertices)))));
            }
        }
    }

    private void persistTraversal(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, Collection<Vertex> processedVertices, GraphTraversal traversal) {
        Stream<ModelElement> res = traversal.toStream()
                .map(e -> {
                    Vertex modelVertex = (Vertex) e;
                    updateRawElementRelationshipsAccordingToGraphEdges(modelVertex, entityMetamodelGraph);
                    return modelVertex;
                })
                .peek(mv -> {
                    Vertex tv = (Vertex) mv;
                    log.info("Persisting vertex of type {} with id {}", tv.label(), vertexResolver.getVertexModelElementId(tv));
                })
                .map(m -> {
                    Vertex v = (Vertex) m;
                    processedVertices.add(v);
                    return v;
                })
                .map(me -> vertexResolver.getModelElement((Vertex) me));
        vertexPersister.persistVertices(res);
    }

    void updateRawElementRelationshipsAccordingToGraphEdges(Vertex sourceVertex, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        ModelElement sourceModelElement = vertexResolver.getModelElement(sourceVertex);
        MetamodelVertex sourceMetamodelVertex = vertexResolver.getMetamodelVertex(sourceVertex);
        metamodelGraphRequester.getOutboundFieldEdges(sourceMetamodelVertex, entityMetamodelGraph).stream()
                .map(metamodelEdge -> {
                    modelElementProcessor.resetModelElementEdge(metamodelEdge, sourceModelElement);
                    return metamodelEdge;
                })
                .flatMap(metamodelEdge ->
                        StreamSupport.stream(Spliterators.spliteratorUnknownSize(sourceVertex.edges(Direction.OUT, metamodelEdge.getFieldName()), 0), false)
                                .map(modelEdge -> new ModelAndMetamodelEdge(modelEdge, metamodelEdge))
                )
                .peek(modelAndMetamodelEdge -> log.info("Recomposing edge: {} between source vertex of type {} with id {} and target vertex of type {} and id {}", modelAndMetamodelEdge.modelEdge().label(), sourceVertex.label(), vertexResolver.getVertexModelElementId(sourceVertex), modelAndMetamodelEdge.modelEdge().inVertex().label(), vertexResolver.getVertexModelElementId(modelAndMetamodelEdge.modelEdge().inVertex())))
                .forEach(modelAndMetamodelEdge -> {
                    ModelElement targetModelElement = vertexResolver.getModelElement(modelAndMetamodelEdge.modelEdge().inVertex());
                    modelElementProcessor.addRawElementsRelationshipForEdge(modelAndMetamodelEdge.metamodelEdge(), sourceModelElement, targetModelElement, entityMetamodelGraph);
                });
    }


    private void removeCyclicElements(GraphTraversalSource modelGraph) {
        modelGraph.V().as("a")
                .repeat(out())
                .until(where(eq("a"))
                        .or().loops().is(CYCLE_DETECTION_DEPTH))
                .filter(where(eq("a")))
                .filter(t -> {
                    Vertex v = t.get();
                    log.warn("Cyclic element of type {} with id {} found in the graph", v.label(), vertexResolver.getVertexModelElementId(v));
                    return true;
                })
                .inE()
                .drop().iterate();
        modelGraph.V().as("a")
                .repeat(out())
                .until(where(eq("a"))
                        .or().loops().is(CYCLE_DETECTION_DEPTH))
                .filter(where(eq("a")))
                .filter(t -> {
                    Vertex v = t.get();
                    log.warn("Cyclic element of type {} with id {} found in the graph", v.label(), vertexResolver.getVertexModelElementId(v));
                    return true;
                })
                .drop().iterate();
/*        cyclicElements.toStream()
                .peek(v -> {
                    Vertex ve = (Vertex) v;
                    log.warn("Cyclic element of type {} with id {} found in the graph", ve.label(), vertexResolver.getId(ve));
                })
                .forEach(v -> {
                            Object id = switch (v) {
                                case DetachedVertex dv -> dv.id();
                                case TinkerVertex tv -> tv.id();
                                default -> throw new IllegalStateException("Unexpected value: " + v);
                            };
                            modelGraph.V(id).drop().iterate();
                        }
                );*/
    }
}
