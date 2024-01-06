package net.osgiliath.migrator.core.db.inject;

import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;
import org.apache.tinkerpop.gremlin.structure.Edge;

public class ModelAndMetamodelEdge {
    private final Edge edge;
    private final OutboundEdge metamodelEdge;

    public ModelAndMetamodelEdge(Edge modelEdge, OutboundEdge metamodelEdge) {
        this.edge = modelEdge;
        this.metamodelEdge = metamodelEdge;
    }

    public Edge getModelEdge() {
        return edge;
    }

    public OutboundEdge getMetamodelEdge() {
        return metamodelEdge;
    }
}
