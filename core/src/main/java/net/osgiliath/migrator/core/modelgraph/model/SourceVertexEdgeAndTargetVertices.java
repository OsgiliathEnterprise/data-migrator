package net.osgiliath.migrator.core.modelgraph.model;

import org.apache.tinkerpop.gremlin.structure.Vertex;

public class SourceVertexEdgeAndTargetVertices extends EdgeTargetVertices {
    private final Vertex sourceVertex;

    public SourceVertexEdgeAndTargetVertices(Vertex sourceVertex, EdgeTargetVertices edgeAndTargetVertices) {
        super(edgeAndTargetVertices.getEdge(), edgeAndTargetVertices.getTargetVertices());
        this.sourceVertex = sourceVertex;
    }

    public Vertex getSourceVertex() {
        return sourceVertex;
    }

}
