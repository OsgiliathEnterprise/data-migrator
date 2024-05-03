package net.osgiliath.migrator.core.metamodel.impl;

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
import org.jgrapht.Graph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


public abstract class MetamodelGraphRequester<M extends MetamodelVertex> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetamodelGraphRequester.class);
    public static final String LABEL = "label";

    public void displayGraphWithGraphiz(Graph<M, FieldEdge<M>> graph) {
        DOTExporter<M, FieldEdge<M>> exporter =
                new DOTExporter<>(v -> v.getTypeName().toLowerCase());
        exporter.setVertexAttributeProvider(v -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put(LABEL, DefaultAttribute.createAttribute(v.getTypeName().toLowerCase()));
            return map;
        });
        exporter.setEdgeAttributeProvider(e -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put(LABEL, DefaultAttribute.createAttribute(e.getFieldName().toLowerCase()));
            return map;
        });
        Writer writer = new StringWriter();
        exporter.exportGraph(graph, writer);
        log.warn("*************** Metamodel graph ***************");
        log.warn("{}", writer);
        log.warn("*************** End Metamodel graph ***************");
    }

    public abstract Collection<FieldEdge<M>> getOutboundFieldEdges(M sourceVertex, Graph<M, FieldEdge<M>> graph);

    /**
     * Get the inverse relationship of an edge.
     *
     * @param fieldEdge    Edge to get the inverse of.
     *                     The edge must be an outbound edge of this vertex.
     * @param targetVertex Target vertex of the edge.
     * @param graph        The metamodel graph.
     * @return The inverse edge.
     */
    public abstract Optional<FieldEdge<M>> getInverseFieldEdge(FieldEdge<M> fieldEdge, M targetVertex, Graph<M, FieldEdge<M>> graph);


}
