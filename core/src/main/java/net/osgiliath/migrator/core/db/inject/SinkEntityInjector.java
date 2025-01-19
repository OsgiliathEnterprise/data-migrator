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
import net.osgiliath.migrator.core.db.inject.model.ModelEdgeAndMetamodelEdge;
import net.osgiliath.migrator.core.db.inject.model.ModelEdgeMetamodelEdgeAndTargetModelElement;
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import net.osgiliath.migrator.core.graph.VertexResolver;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
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
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SINK_TRANSACTION_MANAGER;
import static org.apache.tinkerpop.gremlin.process.traversal.P.eq;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

@Component
public class SinkEntityInjector {
    private static final Logger log = LoggerFactory.getLogger(SinkEntityInjector.class);
    public static final Integer CYCLE_DETECTION_DEPTH = 30;
    public static final String PROCESSED = "processed";
    private final VertexPersister vertexPersister;
    private final MetamodelRequester metamodelGraphRequester;
    private final ModelElementProcessor modelElementProcessor;
    private final VertexResolver vertexResolver;
    private final PlatformTransactionManager sinkPlatformTxManager;
    private final VertexResolver vertexResolverInGraph;


    public SinkEntityInjector(VertexPersister vertexPersister, MetamodelRequester metamodelGraphRequester, ModelElementProcessor modelElementProcessor, VertexResolver vertexResolver, @Qualifier(SINK_TRANSACTION_MANAGER) PlatformTransactionManager sinkPlatformTxManager, VertexResolver vertexResolverInGraph) {
        super();
        this.sinkPlatformTxManager = sinkPlatformTxManager;
        this.vertexPersister = vertexPersister;
        this.metamodelGraphRequester = metamodelGraphRequester;
        this.modelElementProcessor = modelElementProcessor;
        this.vertexResolver = vertexResolver;
        this.vertexResolverInGraph = vertexResolverInGraph;
    }

    public void persist(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        log.info("Least connected vertex are ordered, starting the import");
        removeCyclicElements(modelGraph);
        processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph);
    }

    private void processEntitiesWithoutCycles(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        GraphTraversal<Vertex, Vertex> orphansElements = modelGraph.V()
                .hasNot(PROCESSED)
                .as("s")
                .or(
                        not(outE()),
                        out().filter(not(where(eq("s"))))
                                .hasNot(PROCESSED)
                                .count().is(0)
                );
        if (orphansElements.hasNext()) {
            log.debug("persisting orphans elements");
            persistTraversal(entityMetamodelGraph, orphansElements);
            modelGraph.V()
                    .hasNot(PROCESSED)
                    .as("s")
                    .or(
                            not(outE()),
                            out()
                                    .filter(not(where(eq("s"))))
                                    .hasNot(PROCESSED)
                                    .count().is(0)
                    ).property(PROCESSED, true).iterate();
            processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph);
        } else {
            log.debug("persisting related elements in the same transaction");
            GraphTraversal<Vertex, Vertex> bulk = modelGraph.V().hasNot(PROCESSED);
            persistTraversal(entityMetamodelGraph, bulk);
            modelGraph.V().hasNot(PROCESSED).property(PROCESSED, true).iterate();
        }
    }

    private void persistTraversal(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, GraphTraversal<Vertex, Vertex> traversal) {
        Stream<ModelElement> res =
                traversal
                        .toStream()
                        .map(modelVertex ->
                                updateRawElementRelationshipsAccordingToGraphEdges(modelVertex, entityMetamodelGraph)
                        )
                        .peek(me -> {
                            log.info("Persisting vertex of type {} with id {}", me.vertex().getTypeName(), modelElementProcessor.getId(me).get());
                        }).filter(
                                me -> {
                                    Optional<Object> id = modelElementProcessor.getId(me);
                                    return id.isPresent() && id.get() != null &&
                                            ((id.get() instanceof Long && 0L != (Long) id.get()) ||
                                                    (id.get() instanceof String && !((String) id.get()).isEmpty()) ||
                                                    id.get().getClass().getAnnotation(jakarta.persistence.Embeddable.class) != null);
                                }
                        );
        TransactionTemplate tpl = new TransactionTemplate(sinkPlatformTxManager);
        try {
            tpl.executeWithoutResult(
                    act -> {
                        vertexPersister.persistVertices(res);
                    }
            );
        } catch (Exception e) {
            log.error("Unable to save one element", e);
        }
    }

    record FieldEdgesAndSourceVertex(FieldEdge<MetamodelVertex> fieldEdge, Vertex sourceVertex) {
    }

    ;

    record SourceVertexAndModelEdgeAndMetamodelEdge(Vertex sourceVertex,
                                                    ModelEdgeAndMetamodelEdge modelEdgeAndMetamodelEdge) {

        public Vertex sourceVertex() {
            return sourceVertex;
        }

        public Edge modelEdge() {
            return modelEdgeAndMetamodelEdge.modelEdge();
        }
    }

    ;

    record SourceVertexModelEdgeMetamodelEdgeAndTargetModelElement(ModelElement modelElement,
                                                                   ModelEdgeMetamodelEdgeAndTargetModelElement modelEdgeMetamodelEdgeAndTargetModelElement) {
        public ModelElement modelElement() {
            return modelElement;
        }

        public FieldEdge<MetamodelVertex> metamodelEdge() {
            return modelEdgeMetamodelEdgeAndTargetModelElement.metamodelEdge();
        }

        public ModelElement target() {
            return modelEdgeMetamodelEdgeAndTargetModelElement.target();
        }
    }

    ;

    ModelElement updateRawElementRelationshipsAccordingToGraphEdges(Vertex sourceVertex, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        ModelElement sourceModelElement = modelElementProcessor.unproxy(vertexResolver.getModelElement(sourceVertex));
        modelElementProcessor.resetModelElementEdge(sourceModelElement);
        MetamodelVertex sourceMetamodelVertex = vertexResolver.getMetamodelVertex(sourceVertex);
        Collection<FieldEdge<MetamodelVertex>> fieldEdges = metamodelGraphRequester.getOutboundFieldEdges(sourceMetamodelVertex, entityMetamodelGraph);
        Collection<FieldEdgesAndSourceVertex> toProcess = fieldEdges.stream().map(fE -> new FieldEdgesAndSourceVertex(fE, sourceVertex)).toList();

        toProcess.stream()
                .flatMap(fieldEdgeAndSourceVertex ->
                        StreamSupport.stream(Spliterators.spliteratorUnknownSize(fieldEdgeAndSourceVertex.sourceVertex().edges(Direction.OUT, fieldEdgeAndSourceVertex.fieldEdge().getFieldName()), 0), false)
                                .map(modelEdge -> new SourceVertexAndModelEdgeAndMetamodelEdge(fieldEdgeAndSourceVertex.sourceVertex(), new ModelEdgeAndMetamodelEdge(modelEdge, fieldEdgeAndSourceVertex.fieldEdge())))
                )
                .peek(sourceVertexAndMetamodelEdge -> log.info("Recomposing edge: {} between source vertex of type {} with id {} and target vertex of type {} and id {}", sourceVertexAndMetamodelEdge.modelEdge().label(), sourceVertexAndMetamodelEdge.sourceVertex().label(), vertexResolver.getVertexModelElementId(sourceVertexAndMetamodelEdge.sourceVertex()), sourceVertexAndMetamodelEdge.modelEdge().inVertex().label(), vertexResolver.getVertexModelElementId(sourceVertexAndMetamodelEdge.modelEdge().inVertex())))
                .map(sourceVertexAndMetamodelEdge -> {
                    ModelElement targetModelElement = vertexResolver.getModelElement(sourceVertexAndMetamodelEdge.modelEdge().inVertex());
                    return new SourceVertexModelEdgeMetamodelEdgeAndTargetModelElement(sourceModelElement, new ModelEdgeMetamodelEdgeAndTargetModelElement(sourceVertexAndMetamodelEdge.modelEdgeAndMetamodelEdge(), targetModelElement));
                })
                .forEach(modelAndMetamodelEdge ->
                        modelElementProcessor.addRawElementsRelationshipForEdge(modelAndMetamodelEdge.metamodelEdge(), modelAndMetamodelEdge.modelElement(), modelAndMetamodelEdge.target())
                );
        return sourceModelElement;
    }


    private void removeCyclicElements(GraphTraversalSource modelGraph) {
        modelGraph.V().as("a")
                .repeat(out())
                .until(where(eq("a"))
                        .or().loops().is(CYCLE_DETECTION_DEPTH))
                .filter(where(eq("a")))
                .filter(not(in().where(eq("a"))))
                .filter(t -> {
                    Vertex v = t.get();
                    log.warn("Cyclic element of type {} with id {} found in the graph", v.label(), vertexResolver.getVertexModelElementId(v));
                    return true;
                })
/*                .inE()
                .filter(e -> {
                    Edge v = e.get();
                    log.warn("Will remove the problematic incoming edge {} which creates cycle between {} with id {} and {} with id {}", v.label(), v.inVertex().label(), vertexResolver.getVertexModelElementId(v.inVertex()), v.outVertex().label(), vertexResolver.getVertexModelElementId(v.outVertex()));
                    return true;
                })*/
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
