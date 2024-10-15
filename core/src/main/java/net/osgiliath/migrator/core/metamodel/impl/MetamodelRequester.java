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

import net.osgiliath.migrator.core.api.metamodel.RelationshipType;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import net.osgiliath.migrator.core.rawelement.RelationshipProcessor;
import org.jgrapht.Graph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class MetamodelRequester {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetamodelRequester.class);
    public static final String LABEL = "label";
    private final RawElementProcessor rawElementProcessor;
    private final RelationshipProcessor relationshipProcessor;

    public MetamodelRequester(RawElementProcessor rawElementProcessor, RelationshipProcessor relationshipProcessor) {
        this.rawElementProcessor = rawElementProcessor;
        this.relationshipProcessor = relationshipProcessor;
    }

    public void displayGraphWithGraphiz(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> graph) {
        DOTExporter<MetamodelVertex, FieldEdge<MetamodelVertex>> exporter =
                new DOTExporter<>(v -> v.getTypeName().substring(v.getTypeName().lastIndexOf('.') + 1).toLowerCase());
        exporter.setVertexAttributeProvider(v -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put(LABEL, DefaultAttribute.createAttribute(v.getTypeName().substring(v.getTypeName().lastIndexOf('.') + 1).toLowerCase()));
            return map;
        });
        exporter.setEdgeAttributeProvider(e -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put(LABEL, DefaultAttribute.createAttribute(e.getFieldName().toLowerCase()));
            return map;
        });
        try {
            Writer writer = new StringWriter();
            exporter.exportGraph(graph, writer);
            log.warn("*************** Metamodel graph ***************");
            log.warn("{}", writer);
            log.warn("*************** End Metamodel graph ***************");
        } catch (Exception e) {
            log.warn("Metamodel graph can't be generated");
        }
    }

    /**
     * Get the type of the relationship (one to one, one to many, many to one, many to many).
     *
     * @param fieldEdge the field edge to get the type of relationship for
     * @return the type of the relationship.
     */
    public Optional<RelationshipType> getRelationshipType(FieldEdge<MetamodelVertex> fieldEdge) {
        Optional<Method> getterMethodOpt = relationshipGetter(fieldEdge);
        return getterMethodOpt.map(relationshipProcessor::relationshipType);
    }

    /**
     * {@inheritDoc}
     */
    public Collection<FieldEdge<MetamodelVertex>> getOutboundFieldEdges(MetamodelVertex sourceVertex, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> graph) {
        return graph.outgoingEdgesOf(sourceVertex);
    }

    /**
     * Get the inverse relationship of an edge.
     *
     * @param fieldEdge    Edge to get the inverse of.
     *                     The edge must be an outbound edge of this vertex.
     * @param targetVertex Target vertex of the edge.
     * @param graph        The metamodel graph.
     * @return The inverse edge.
     */
    /*public Optional<FieldEdge<MetamodelVertex>> getInverseFieldEdge(FieldEdge<MetamodelVertex> fieldEdge, MetamodelVertex targetVertex, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> graph) {
        Optional<Method> getterMethodOpt = relationshipGetter(fieldEdge);
        return getterMethodOpt.flatMap(getterMethod -> relationshipProcessor.inverseRelationshipField(getterMethod, targetVertex).flatMap(
                f -> getOutboundFieldEdges(targetVertex, graph).stream().filter(e -> e.getFieldName().equals(f.getName())).findAny()
        ));
    }*/

    /**
     * returns the getter method of the entity's relationship.
     *
     * @return the getter method of the entity's relationship.
     */
    public Optional<Method> relationshipGetter(FieldEdge<MetamodelVertex> fieldEdge) {
        return rawElementProcessor.getterMethod(fieldEdge.getSource(), fieldEdge.getMetamodelField());
    }

}
