package net.osgiliath.migrator.core.graph;

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

import jakarta.transaction.Transactional;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.common.FakeEntity;
import net.osgiliath.migrator.core.common.MetamodelClass;
import net.osgiliath.migrator.core.common.MockTraversalVertex;
import net.osgiliath.migrator.core.common.MockVertex;
import net.osgiliath.migrator.core.graph.model.EdgeTargetVertexOrVertices;
import net.osgiliath.migrator.core.graph.model.ManyEdgeTarget;
import net.osgiliath.migrator.core.graph.model.UnitaryEdgeTarget;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.jpa.JpaEntityProcessor;
import net.osgiliath.migrator.core.rawelement.jpa.JpaRelationshipProcessor;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelElementTest {

    // @Mock
    private JpaEntityProcessor jpaEntityHelper;

    @Mock
    private FieldEdge fieldEdge;

    @Mock
    private GraphTraversalSource graphTraversalSource;

    private MockTraversalVertex traversal;

    @Mock
    private MetamodelRequester metamodelGraphRequester;

    private TinkerpopModelGraphEdgeBuilder modelGraphEdgeBuilder;
    @Mock
    private PlatformTransactionManager txMgr;

    @BeforeEach
    void setup() {
        jpaEntityHelper = new JpaEntityProcessor(txMgr);
        traversal = new MockTraversalVertex(jpaEntityHelper);
        JpaRelationshipProcessor jpaRelationshipProcessor = new JpaRelationshipProcessor(jpaEntityHelper);
        VertexResolver resolver = new InGraphVertexResolver();
        ModelElementProcessor processor = new ModelElementProcessor(jpaEntityHelper, metamodelGraphRequester, jpaRelationshipProcessor);
        modelGraphEdgeBuilder = new TinkerpopModelGraphEdgeBuilder(jpaEntityHelper, metamodelGraphRequester, resolver, processor, txMgr);
    }

    @Test
    void testGetEdgeValueFromVertexGraphToOne() throws NoSuchMethodException {
        // Arrange
        Vertex modelElement = traversal.getVertex(MockTraversalVertex.ENTITY_ID_1);
        FakeEntity fe = (FakeEntity) ((MockVertex) modelElement).getMe().rawElement();
        when(metamodelGraphRequester.relationshipGetter(any())).thenReturn(Optional.of(fe.getClass().getMethod("getOne")));
        when(fieldEdge.getTarget()).thenReturn(new JpaMetamodelVertex(MetamodelClass.class, FakeEntity.class));
        when(graphTraversalSource.V()).thenReturn(traversal);
        // Act
        Optional<EdgeTargetVertexOrVertices> result = modelGraphEdgeBuilder.getEdgeValueFromVertexGraph(modelElement, fieldEdge, graphTraversalSource);
        // Assert
        assertTrue(result.isPresent());
        Assertions.assertEquals(MockTraversalVertex.ENTITY_ID_1, ((UnitaryEdgeTarget) result.get()).target().id());
    }

    @Transactional
    @Test
    void testGetEdgeValueFromVertexGraphToMany() throws NoSuchMethodException {
        Vertex modelElement = traversal.getVertex(MockTraversalVertex.ENTITY_ID_1);
        FakeEntity fe = (FakeEntity) ((MockVertex) modelElement).getMe().rawElement();

        // Arrange
        when(metamodelGraphRequester.relationshipGetter(any())).thenReturn(Optional.of(fe.getClass().getMethod("getMany")));
        when(fieldEdge.getTarget()).thenReturn(new JpaMetamodelVertex(MetamodelClass.class, FakeEntity.class));
        when(graphTraversalSource.V()).thenReturn(traversal);
        // Act
        Optional<EdgeTargetVertexOrVertices> result = modelGraphEdgeBuilder.getEdgeValueFromVertexGraph(modelElement, fieldEdge, graphTraversalSource);
        // Assert
        assertTrue(result.isPresent());
        Collection<Vertex> target = ((ManyEdgeTarget) result.get()).target().collect(Collectors.toSet());
        Assertions.assertEquals(1, target.size());
        assertEquals(MockTraversalVertex.ENTITY_ID_1, ((MockVertex) target.iterator().next()).getFe().getId());

    }
}
