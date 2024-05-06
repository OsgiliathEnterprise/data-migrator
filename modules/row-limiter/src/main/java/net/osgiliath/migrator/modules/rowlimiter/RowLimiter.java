package net.osgiliath.migrator.modules.rowlimiter;

/*-
 * #%L
 * row-limiter
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
import net.osgiliath.migrator.core.api.transformers.GraphTransformer;
import net.osgiliath.migrator.core.configuration.DataSourceConfiguration;
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import net.osgiliath.migrator.core.graph.ModelGraphBuilder;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Map;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;

@Component
public class RowLimiter implements GraphTransformer {

    public static final String LIMIT = "rows-to-keep";
    private static final Logger log = LoggerFactory.getLogger(RowLimiter.class);
    private final ModelElementProcessor modelElementProcessor;
    private final MetamodelRequester metamodelGraphRequester;

    public RowLimiter(ModelElementProcessor modelElementProcessor, MetamodelRequester metamodelGraphRequester) {

        this.modelElementProcessor = modelElementProcessor;
        this.metamodelGraphRequester = metamodelGraphRequester;
    }

    @Override
    @Transactional(transactionManager = DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER, readOnly = true)
    public void evaluate(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, Map<String, String> sequencerOptions) {
        Integer limit = Integer.valueOf(sequencerOptions.get(LIMIT));
        Comparator<MetamodelVertex> asc = Comparator.comparingInt(entityMetamodelGraph::inDegreeOf);
        entityMetamodelGraph.vertexSet().stream().sorted(asc)
                .forEach(metamodelVertex -> processEntities(metamodelVertex, modelGraph, limit, entityMetamodelGraph));
    }

    private void processEntities(MetamodelVertex studiedVertex, GraphTraversalSource modelGraph, Integer limit, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {

        GraphTraversal elementsOfStudiedVertex = modelGraph.V()
                .hasLabel(studiedVertex.getTypeName());
        if (limit >= (Long) elementsOfStudiedVertex.asAdmin().clone().count().next()) {
            return;
        }
        GraphTraversal noOut = elementsOfStudiedVertex.asAdmin().clone()
                .filter(
                        __.not(
                                __.out())).order().by(__.in().count()).by(Order.desc).limit(((Long) elementsOfStudiedVertex.asAdmin().clone().count().next()) - limit);
        removeRelationshipFromInboundModelElements(modelGraph, entityMetamodelGraph, noOut.asAdmin().clone());
        noOut.drop().iterate();

        if (limit >= (Long) elementsOfStudiedVertex.asAdmin().clone().count().next()) {
            return;
        }
        GraphTraversal leafElements = elementsOfStudiedVertex.asAdmin().clone()
                .repeat(out())
                .until(out()
                        .filter(
                                __.count().is(0).or().loops().is(10)))
                .order().by(__.in().count()).by(Order.desc).limit(((Long) elementsOfStudiedVertex.asAdmin().clone().count().next()) - limit);
        removeRelationshipFromInboundModelElements(modelGraph, entityMetamodelGraph, leafElements.asAdmin().clone());
        leafElements.drop().iterate();
        // remove off-limit elements
    }

    private void removeRelationshipFromInboundModelElements(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph, GraphTraversal leafElements) {
        leafElements.toStream().map(leafElement -> new InEdgesfVertex((Vertex) leafElement, modelGraph.V(((Vertex) leafElement).id()).inE().toList()))
                .flatMap(inEdgesAndTargetVertex -> ((InEdgesfVertex) inEdgesAndTargetVertex).getEdges().stream().map(edge -> new InEdgeAndTargetVertex(edge, ((InEdgesfVertex) inEdgesAndTargetVertex).getLeafElement())))
                .forEach(inEdgeAndTargetVertex -> {
                    Edge edge = ((InEdgeAndTargetVertex) inEdgeAndTargetVertex).getEdge();
                    Vertex leafElement = ((InEdgeAndTargetVertex) inEdgeAndTargetVertex).getLeafElement();
                    removeRelationshipsFromSourceToTarget(edge.outVertex(), leafElement, entityMetamodelGraph);
                });
    }

    private void removeRelationshipsFromSourceToTarget(Vertex sourceVertex, Vertex targetVertex, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph) {
        MetamodelVertex sourceMetamodelVertex = sourceVertex.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_METAMODEL_VERTEX);
        ModelElement sourceModelElement = sourceVertex.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY);
        MetamodelVertex targetMetamodelVertex = targetVertex.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_METAMODEL_VERTEX);
        ModelElement targetModelElement = targetVertex.value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY);
        metamodelGraphRequester.getOutboundFieldEdges(sourceMetamodelVertex, entityMetamodelGraph).stream()
                .filter(fieldEdge -> fieldEdge.getTarget().equals(targetMetamodelVertex))
                .forEach(fieldEdge -> {
                    log.info("Removing edge {} from vertex {} to vertex {}", fieldEdge.getFieldName(), sourceVertex.id(), targetVertex.id());
                    modelElementProcessor.removeEdgeValueFromModelElementRelationShip(sourceModelElement, fieldEdge, targetModelElement);
                });
    }

}
