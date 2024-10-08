package net.osgiliath.migrator.core.common;

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

import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.graph.TinkerpopModelGraphBuilder;
import org.apache.tinkerpop.gremlin.structure.*;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class MockVertex implements Vertex {

    private final ModelElement me;

    public MockVertex(ModelElement me) {
        this.me = me;
    }


    @Override
    public Edge addEdge(String label, Vertex inVertex, Object... keyValues) {
        return null;
    }

    @Override
    public <V> VertexProperty<V> property(VertexProperty.Cardinality cardinality, String key, V value, Object... keyValues) {
        return null;
    }

    @Override
    public Iterator<Edge> edges(Direction direction, String... edgeLabels) {
        return null;
    }

    @Override
    public Iterator<Vertex> vertices(Direction direction, String... edgeLabels) {
        return null;
    }

    @Override
    public Object id() {
        return ((FakeEntity) me.rawElement()).getId();
    }

    @Override
    public String label() {
        return ((FakeEntity) me.rawElement()).getClass().getSimpleName();
    }

    @Override
    public Graph graph() {
        return null;
    }

    @Override
    public void remove() {
    }

    @Override
    public <V> Iterator<VertexProperty<V>> properties(String... propertyKeys) {
        return null;
    }

    public FakeEntity getFe() {
        return ((FakeEntity) me.rawElement());
    }

    public ModelElement getMe() {
        return me;
    }

    @Override
    public <V> V value(final String key) throws NoSuchElementException {
        if (key.equals(TinkerpopModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY))
            return (V) me;
        return null;
    }
}
