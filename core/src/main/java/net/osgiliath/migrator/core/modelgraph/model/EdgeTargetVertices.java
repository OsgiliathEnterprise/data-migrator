package net.osgiliath.migrator.core.modelgraph.model;

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Collection;

public class EdgeTargetVertices {
    private FieldEdge edge;
    private Collection<Vertex> targetVertices;

    public EdgeTargetVertices(FieldEdge edge, Collection<Vertex> targetVertices) {
        this.edge = edge;
        this.targetVertices = targetVertices;
    }

    public FieldEdge getEdge() {
        return edge;
    }

    public void setEdge(FieldEdge edge) {
        this.edge = edge;
    }

    public Collection<Vertex> getTargetVertices() {
        return targetVertices;
    }

    public void setTargetVertices(Collection<Vertex> targetVertices) {
        this.targetVertices = targetVertices;
    }
}
