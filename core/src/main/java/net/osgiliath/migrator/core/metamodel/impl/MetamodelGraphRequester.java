package net.osgiliath.migrator.core.metamodel.impl;

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import org.jgrapht.Graph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

@Component
public class MetamodelGraphRequester {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetamodelGraphRequester.class);

    public List<MetamodelVertex> findLeastConnectedVertexIgnoringSelfReference(Graph<MetamodelVertex, FieldEdge> graph) {
        List<MetamodelVertex> res = new ArrayList<>();
        Set<MetamodelVertex> toProcess = graph.vertexSet();
        int currentMinEdgeCount = 0;
        while(res.size() != toProcess.size()) {
            for (MetamodelVertex v: toProcess) {
                Set<FieldEdge> outgoingEdges = graph.outgoingEdgesOf(v);
                if (outgoingEdges.size() == currentMinEdgeCount) {
                    res.add(v);
                }
                int currentEdgeCountWithoutSelfReference = 0;
                for (FieldEdge e: outgoingEdges) {
                    if (!graph.getEdgeTarget(e).equals(v)) {
                        currentEdgeCountWithoutSelfReference+=1;
                    }
                }
                if (currentEdgeCountWithoutSelfReference == currentMinEdgeCount && !res.contains(v)) {
                    res.add(v);
                }
            }
            currentMinEdgeCount+=1;
        }
        return res;
    }

    public void displayGraphWithGraphiz(Graph<MetamodelVertex, FieldEdge> graph) {
        DOTExporter<MetamodelVertex, FieldEdge> exporter =
            new DOTExporter<>(v -> v.getTypeName().toLowerCase());
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.getTypeName().toLowerCase()));
            return map;
        });
        Writer writer = new StringWriter();
        exporter.exportGraph(graph, writer);
        log.warn("Finished graph processing of the metamodel {}", writer);
    }

}
