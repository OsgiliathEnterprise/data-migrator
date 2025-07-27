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
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.shaded.jackson.core.JsonProcessingException;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SINK_TRANSACTION_MANAGER;
import static org.apache.tinkerpop.gremlin.process.traversal.Order.asc;
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


    public SinkEntityInjector(VertexPersister vertexPersister, MetamodelRequester metamodelGraphRequester, ModelElementProcessor modelElementProcessor, VertexResolver vertexResolver, @Qualifier(SINK_TRANSACTION_MANAGER) PlatformTransactionManager sinkPlatformTxManager) {
        super();
        this.sinkPlatformTxManager = sinkPlatformTxManager;
        this.vertexPersister = vertexPersister;
        this.metamodelGraphRequester = metamodelGraphRequester;
        this.modelElementProcessor = modelElementProcessor;
        this.vertexResolver = vertexResolver;
    }

    public void persist(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        log.info("Least connected vertex are ordered, starting the import");
        removeCyclicElements(modelGraph); // TODO: remove this line: theorically it should not be needed as the graph because cyclis elements are processed together with the others
        processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph);
    }

    private void processEntitiesWithoutCycles(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {

        GraphTraversal<Vertex, Vertex> orphansElements = modelGraph.V().hasNot(PROCESSED).
                not(outE()).property(PROCESSED, true);
        if (orphansElements.hasNext()) {
            log.info("Persisting leaf elements");
            persistTraversal(entityMetamodelGraph, orphansElements);
            processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph);
        } else {
            GraphTraversal<Vertex, Vertex> allOutProcessed = modelGraph.V().hasNot(PROCESSED).
                    where(not(out().hasNot(PROCESSED))).property(PROCESSED, true);
            if (allOutProcessed.hasNext()) {
                log.info("Persisting processed outbound elements");
                persistTraversal(entityMetamodelGraph, allOutProcessed);
                processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph);
            } else {
                GraphTraversal<Vertex, Vertex> cyclicPath = modelGraph.V()
                        .hasNot(PROCESSED)
                        .repeat(out().filter(hasNot(PROCESSED)))
                        .until(or(loops().is(CYCLE_DETECTION_DEPTH), cyclicPath())).path().order().by(count(Scope.local), asc).limit(1).reverse().unfold();
                GraphTraversal<Vertex, Vertex> markAndSweepCyclic = cyclicPath.asAdmin().clone();
                if (cyclicPath.hasNext()) {
                    log.warn("Persisting cyclic paths, element are supposed to be removed");
                    persistTraversal(entityMetamodelGraph, cyclicPath);
                    markAndSweepCyclic.property(PROCESSED, true).iterate();
                    processEntitiesWithoutCycles(modelGraph, entityMetamodelGraph);
                }
            }
        }
    }

    private void persistTraversal(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, GraphTraversal<Vertex, Vertex> traversal) {
        Stream<ModelElement> res =
                traversal
                        .toStream()
                        .map(modelVertex -> updateRawElementRelationshipsAccordingToGraphEdges(modelVertex, entityMetamodelGraph)
                        ).filter(
                                me -> {
                                    Optional<Object> id = modelElementProcessor.getId(me);
                                    return id.isPresent() && id.get() != null &&
                                            ((id.get() instanceof Long && 0L != (Long) id.get()) ||
                                                    (id.get() instanceof String && !((String) id.get()).isEmpty()) ||
                                                    id.get().getClass().getAnnotation(jakarta.persistence.Embeddable.class) != null);
                                }
                        ).peek(me -> {
                            log.info("Persisting vertex of type {} with id {}", me.vertex().getTypeName(), modelElementProcessor.getId(me).get());
                        });
        TransactionTemplate tpl = new TransactionTemplate(sinkPlatformTxManager);
        try {
            tpl.executeWithoutResult(
                    act ->
                            vertexPersister.persistVertices(res, sinkPlatformTxManager).collect(Collectors.toList())
            );
        } catch (Exception e) {
            log.error("Unable to save one element", e);
        }
    }

    record FieldEdgesAndSourceVertex(FieldEdge<MetamodelVertex> fieldEdge, Vertex sourceVertex) {
    }

    record SourceVertexAndModelEdgeAndMetamodelEdge(Vertex sourceVertex,
                                                    ModelEdgeAndMetamodelEdge modelEdgeAndMetamodelEdge) {

        public Edge modelEdge() {
            return modelEdgeAndMetamodelEdge.modelEdge();
        }
    }

    record SourceVertexModelEdgeMetamodelEdgeAndTargetModelElement(ModelElement modelElement,
                                                                   ModelEdgeMetamodelEdgeAndTargetModelElement modelEdgeMetamodelEdgeAndTargetModelElement) {
        public FieldEdge<MetamodelVertex> metamodelEdge() {
            return modelEdgeMetamodelEdgeAndTargetModelElement.metamodelEdge();
        }

        public ModelElement target() {
            return modelEdgeMetamodelEdgeAndTargetModelElement.target();
        }
    }

    @java.lang.SuppressWarnings("java:S3864")
    ModelElement updateRawElementRelationshipsAccordingToGraphEdges(Vertex sourceVertex, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        ModelElement sourceModelElement = modelElementProcessor.unproxy(vertexResolver.getModelElement(sourceVertex));
        ModelElement sourceModelElementWithRelationshipsReset = modelElementProcessor.resetModelElementEdge(sourceModelElement);
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
                    return new SourceVertexModelEdgeMetamodelEdgeAndTargetModelElement(sourceModelElementWithRelationshipsReset, new ModelEdgeMetamodelEdgeAndTargetModelElement(sourceVertexAndMetamodelEdge.modelEdgeAndMetamodelEdge(), targetModelElement));
                })
                .forEach(modelAndMetamodelEdge ->
                        modelElementProcessor.addRawElementsRelationshipForEdge(modelAndMetamodelEdge.metamodelEdge(), modelAndMetamodelEdge.modelElement(), modelAndMetamodelEdge.target())
                );
        return sourceModelElement;
    }


    private void removeCyclicElements(GraphTraversalSource modelGraph) {
        GraphTraversal<Vertex, Vertex> cycle = modelGraph.V()
                .hasNot(PROCESSED).as("a")
                .repeat(out().filter(hasNot(PROCESSED)))
                .until(or(loops().is(CYCLE_DETECTION_DEPTH), cyclicPath())).path().order().by(count(Scope.local), asc).unfold();
        if (cycle.hasNext()) {
            Vertex v = cycle.next();
            ObjectMapper mapper = new ObjectMapper();
            try {
                log.warn("Cyclic element of type {} : {}", v.label(), mapper.writer().writeValueAsString(modelElementProcessor.unproxy(vertexResolver.getModelElement(v)).rawElement()));
            } catch (JsonProcessingException e) {
            }
            modelGraph.V(v).drop().iterate();
            removeCyclicElements(modelGraph);
        }
    }
}
