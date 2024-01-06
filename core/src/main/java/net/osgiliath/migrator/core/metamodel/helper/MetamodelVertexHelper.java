package net.osgiliath.migrator.core.metamodel.helper;

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import org.jgrapht.Graph;
import org.springframework.stereotype.Component;

@Component
public class MetamodelVertexHelper {
    public MetamodelVertex getTargetMetamodelVertex(Graph metamodelGraph, FieldEdge edge) {
        return (MetamodelVertex) metamodelGraph.getEdgeTarget(edge);
    }

}
