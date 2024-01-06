package net.osgiliath.migrator.core.processing;

import org.apache.tinkerpop.gremlin.structure.Vertex;

public class VertexAndSequencerBean {
    private final Vertex vertex;
    private final Object bean;

    public VertexAndSequencerBean(Vertex vertex, Object bean) {
        this.vertex = vertex;
        this.bean = bean;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public Object getBean() {
        return bean;
    }
}
