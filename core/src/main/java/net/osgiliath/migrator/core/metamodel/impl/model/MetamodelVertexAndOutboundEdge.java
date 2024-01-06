package net.osgiliath.migrator.core.metamodel.impl.model;

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;

public class MetamodelVertexAndOutboundEdge {
    private final MetamodelVertex sourceVertex;
    private final OutboundEdge fieldEdge;

    public MetamodelVertexAndOutboundEdge(MetamodelVertex v, OutboundEdge e) {
        this.sourceVertex = v;
        this.fieldEdge = e;
    }

    public MetamodelVertex getSourceVertex() {
        return sourceVertex;
    }

    public MetamodelVertex getTargetVertex() {
        return fieldEdge.getTargetVertex();
    }

    public FieldEdge getFieldEdge() {
        return fieldEdge.getFieldEdge();
    }
}
