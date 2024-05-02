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
import net.osgiliath.migrator.core.api.model.EdgeTargetVertexOrVertices;
import net.osgiliath.migrator.core.api.model.ManyEdgeTarget;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.api.model.UnitaryEdgeTarget;
import net.osgiliath.migrator.core.metamodel.helper.RawElementHelper;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GraphRequester {

    private final RawElementHelper elementHelper;
    private static final Logger log = LoggerFactory.getLogger(GraphRequester.class);

    public GraphRequester(RawElementHelper elementHelper) {
        this.elementHelper = elementHelper;
    }

    /**
     * get the target vertex or vertices corresponding to the entity referenced by the fieldEdge
     *
     * @param fieldEdge  the edge to get the target vertices from
     * @param modelGraph the model graph
     * @return the target Vertex or Vertices corresponding to the entities referenced by the fieldEdge
     */
    public Optional<EdgeTargetVertexOrVertices> getEdgeValueFromVertexGraph(Vertex sourceVertex, FieldEdge fieldEdge, GraphTraversalSource modelGraph) {
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
