package net.osgiliath.migrator.core.api.model;

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

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.common.FakeEntity;
import net.osgiliath.migrator.core.common.MetamodelClass;
import net.osgiliath.migrator.core.common.MockTraversalVertex;
import net.osgiliath.migrator.core.common.MockVertex;
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import net.osgiliath.migrator.core.modelgraph.GraphRequester;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ModelElementTest {

    // @Mock
    private JpaEntityHelper jpaEntityHelper;

    @Mock
    private FieldEdge fieldEdge;

    @Mock
    private GraphTraversalSource graphTraversalSource;

    private MockTraversalVertex traversal;

    private GraphRequester graphRequester;

    @BeforeEach
    public void setup() {
        jpaEntityHelper = new JpaEntityHelper();
        traversal = new MockTraversalVertex(jpaEntityHelper);
        graphRequester = new GraphRequester(jpaEntityHelper);

    }

    @Test
    public void testGetEdgeValueFromVertexGraphToOne() throws NoSuchMethodException {
        // Arrange
        Vertex modelElement = traversal.getVertex(MockTraversalVertex.ENTITY_ID_1);
        FakeEntity fe = (FakeEntity) ((MockVertex) modelElement).getMe().getRawElement();
        when(fieldEdge.relationshipGetter()).thenReturn(fe.getClass().getMethod("getOne"));
        when(fieldEdge.getTarget()).thenReturn(new JpaMetamodelVertex(MetamodelClass.class, FakeEntity.class, jpaEntityHelper, null));
        when(graphTraversalSource.V()).thenReturn(traversal);
        // Act
        Optional<EdgeTargetVertexOrVertices> result = graphRequester.getEdgeValueFromVertexGraph(modelElement, fieldEdge, graphTraversalSource);
        // Assert
        assertTrue(result.isPresent());
        assertEquals(((UnitaryEdgeTarget) result.get()).target().id(), MockTraversalVertex.ENTITY_ID_1);
    }

    @Test
    public void testGetEdgeValueFromVertexGraphToMany() throws NoSuchMethodException {
        Vertex modelElement = traversal.getVertex(MockTraversalVertex.ENTITY_ID_1);
        FakeEntity fe = (FakeEntity) ((MockVertex) modelElement).getMe().getRawElement();

        // Arrange
        when(fieldEdge.relationshipGetter()).thenReturn(fe.getClass().getMethod("getMany"));
        when(fieldEdge.getTarget()).thenReturn(new JpaMetamodelVertex(MetamodelClass.class, FakeEntity.class, jpaEntityHelper, null));
        when(graphTraversalSource.V()).thenReturn(traversal);
        // Act
        Optional<EdgeTargetVertexOrVertices> result = graphRequester.getEdgeValueFromVertexGraph(modelElement, fieldEdge, graphTraversalSource);
        // Assert
        assertTrue(result.isPresent());
        assertEquals(((ManyEdgeTarget) result.get()).target().size(), 1);
        assertEquals(((MockVertex) ((ManyEdgeTarget) result.get()).target().iterator().next()).getFe().getId(), MockTraversalVertex.ENTITY_ID_1);

    }
}
