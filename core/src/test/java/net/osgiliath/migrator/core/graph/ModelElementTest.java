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

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.common.FakeEntity;
import net.osgiliath.migrator.core.common.MetamodelClass;
import net.osgiliath.migrator.core.common.MockTraversalVertex;
import net.osgiliath.migrator.core.common.MockVertex;
import net.osgiliath.migrator.core.configuration.ModelVertexCustomizer;
import net.osgiliath.migrator.core.configuration.beans.GraphTraversalSourceProvider;
import net.osgiliath.migrator.core.graph.model.EdgeTargetVertexOrVertices;
import net.osgiliath.migrator.core.graph.model.ManyEdgeTarget;
import net.osgiliath.migrator.core.graph.model.UnitaryEdgeTarget;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.jpa.JpaEntityProcessor;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ModelElementTest {

    // @Mock
    private JpaEntityProcessor jpaEntityHelper;

    @Mock
    private FieldEdge fieldEdge;

    @Mock
    private GraphTraversalSource graphTraversalSource;

    private MockTraversalVertex traversal;

    private ModelGraphBuilder modelGraphBuilder;

    @Mock
    private MetamodelRequester metamodelGraphRequester;

    @BeforeEach
    public void setup() {
        jpaEntityHelper = new JpaEntityProcessor();
        traversal = new MockTraversalVertex(jpaEntityHelper);
        GraphTraversalSourceProvider provider = new GraphTraversalSourceProvider(null);
        EntityImporter entityImporter = (entityVertex, objectToExclude) -> List.of();
        ModelElementProcessor modelElementProcessor = new ModelElementProcessor(jpaEntityHelper, metamodelGraphRequester);
        modelGraphBuilder = new ModelGraphBuilder(jpaEntityHelper, entityImporter, provider, metamodelGraphRequester, modelElementProcessor, new ModelVertexCustomizer());

    }

    @Test
    public void testGetEdgeValueFromVertexGraphToOne() throws NoSuchMethodException {
        // Arrange
        Vertex modelElement = traversal.getVertex(MockTraversalVertex.ENTITY_ID_1);
        FakeEntity fe = (FakeEntity) ((MockVertex) modelElement).getMe().rawElement();
        when(metamodelGraphRequester.relationshipGetter(any())).thenReturn(fe.getClass().getMethod("getOne"));
        when(fieldEdge.getTarget()).thenReturn(new JpaMetamodelVertex(MetamodelClass.class, FakeEntity.class));
        when(graphTraversalSource.V()).thenReturn(traversal);
        // Act
        Optional<EdgeTargetVertexOrVertices> result = modelGraphBuilder.getEdgeValueFromVertexGraph(modelElement, fieldEdge, graphTraversalSource);
        // Assert
        assertTrue(result.isPresent());
        Assertions.assertEquals(((UnitaryEdgeTarget) result.get()).target().id(), MockTraversalVertex.ENTITY_ID_1);
    }

    @Test
    public void testGetEdgeValueFromVertexGraphToMany() throws NoSuchMethodException {
        Vertex modelElement = traversal.getVertex(MockTraversalVertex.ENTITY_ID_1);
        FakeEntity fe = (FakeEntity) ((MockVertex) modelElement).getMe().rawElement();

        // Arrange
        when(metamodelGraphRequester.relationshipGetter(any())).thenReturn(fe.getClass().getMethod("getMany"));
        when(fieldEdge.getTarget()).thenReturn(new JpaMetamodelVertex(MetamodelClass.class, FakeEntity.class));
        when(graphTraversalSource.V()).thenReturn(traversal);
        // Act
        Optional<EdgeTargetVertexOrVertices> result = modelGraphBuilder.getEdgeValueFromVertexGraph(modelElement, fieldEdge, graphTraversalSource);
        // Assert
        assertTrue(result.isPresent());
        Assertions.assertEquals(((ManyEdgeTarget) result.get()).target().size(), 1);
        assertEquals(((MockVertex) ((ManyEdgeTarget) result.get()).target().iterator().next()).getFe().getId(), MockTraversalVertex.ENTITY_ID_1);

    }
}
