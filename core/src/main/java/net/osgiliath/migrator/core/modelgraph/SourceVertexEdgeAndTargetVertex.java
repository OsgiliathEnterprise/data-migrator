package net.osgiliath.migrator.core.modelgraph;

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.modelgraph.model.SourceVertexEdgeAndTargetVertices;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class SourceVertexEdgeAndTargetVertex {
    private final SourceVertexEdgeAndTargetVertices edgeAndTargetVertex;
    private final Vertex targetVertex;

    public SourceVertexEdgeAndTargetVertex(SourceVertexEdgeAndTargetVertices edgeAndTargetVertex, Vertex targetVertex) {
        this.edgeAndTargetVertex = edgeAndTargetVertex;
        this.targetVertex = targetVertex;
    }

    public Vertex getSourceVertex() {
        return edgeAndTargetVertex.getSourceVertex();
    }

    public FieldEdge getEdge() {
        return edgeAndTargetVertex.getEdge();
    }

    public Vertex getTargetVertex() {
        return targetVertex;
    }
}
