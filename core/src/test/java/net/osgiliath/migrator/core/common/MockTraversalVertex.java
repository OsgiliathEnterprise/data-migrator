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
import net.osgiliath.migrator.core.graph.ModelGraphBuilder;
import net.osgiliath.migrator.core.rawelement.jpa.JpaEntityProcessor;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MockTraversalVertex extends DefaultGraphTraversal<Vertex, Vertex> implements GraphTraversal<Vertex, Vertex> {
    private final JpaEntityProcessor jpaEntityHelper;
    private List<Vertex> vertices = new ArrayList<>();

    private Iterator<Vertex> vertexIterator;

    public static int ENTITY_ID_1 = 0;
    public static int ENTITY_ID_2 = 1;

    public MockTraversalVertex(JpaEntityProcessor jpaEntityHelper) {
        FakeEntity fe1 = new FakeEntity(ENTITY_ID_1, this);
        ModelElement me1 = new ModelElement(fe1, jpaEntityHelper);
        MockVertex mv1 = new MockVertex(me1);
        vertices.add(mv1);
        FakeEntity fe2 = new FakeEntity(ENTITY_ID_2, this);
        ModelElement me2 = new ModelElement(fe2, jpaEntityHelper);
        MockVertex mv2 = new MockVertex(me2);
        vertices.add(mv2);
        vertexIterator = vertices.iterator();
        this.jpaEntityHelper = jpaEntityHelper;
    }

    public MockTraversalVertex(int id, JpaEntityProcessor jpaEntityHelper) {
        if (id == ENTITY_ID_1) {
            FakeEntity fe1 = new FakeEntity(ENTITY_ID_1, this);
            ModelElement me1 = new ModelElement(fe1, jpaEntityHelper);
            MockVertex mv1 = new MockVertex(me1);
            vertices.add(mv1);
            vertexIterator = vertices.iterator();
        }
        this.jpaEntityHelper = jpaEntityHelper;
    }

    public Vertex getVertex(int vertexId) {
        return vertices.get(vertexId);
    }

    public ModelElement getModelElement(int vertexId) {
        return ((MockVertex) vertices.get(vertexId)).getMe();
    }

    @Override
    public boolean hasNext() {
        return vertexIterator.hasNext();
    }

    @Override
    public Vertex next() {
        return vertexIterator.next();
    }

    @Override
    public GraphTraversal<Vertex, Vertex> hasLabel(String label, String... otherLabels) {
        return this;
    }

    @Override
    public GraphTraversal<Vertex, Vertex> has(String propertyKey, Object value) {
        if (propertyKey.equals(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID)) {
            return new MockTraversalVertex((Integer) value, jpaEntityHelper);
        }
        return super.has(propertyKey, value);
    }

}
