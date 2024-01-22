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
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MetamodelGraphRequester {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetamodelGraphRequester.class);
    public static final String LABEL = "label";

    public void displayGraphWithGraphiz(Graph<MetamodelVertex, FieldEdge> graph) {
        DOTExporter<MetamodelVertex, FieldEdge> exporter =
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
}
