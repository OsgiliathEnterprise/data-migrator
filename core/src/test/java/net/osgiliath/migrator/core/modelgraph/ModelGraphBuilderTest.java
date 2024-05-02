package net.osgiliath.migrator.core.modelgraph;

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
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.common.FakeEntity;
import net.osgiliath.migrator.core.common.MetamodelClass;
import net.osgiliath.migrator.core.configuration.beans.GraphTraversalSourceProvider;
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jgrapht.Graph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;


public class ModelGraphBuilderTest {

    @Mock
    private EntityImporter entityImporter;

    @Mock
    private GraphTraversalSourceProvider graphTraversalSourceProvider;

    @Mock
    private Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph;

    @Mock
    private GraphTraversalSource graphTraversalSource;

    private ModelGraphBuilder modelGraphBuilder;


    @Mock
    private Vertex vertex;
    private JpaEntityHelper jpaEntityHelper;
    private GraphRequester graphRequester;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        jpaEntityHelper = new JpaEntityHelper();

        graphRequester = new GraphRequester(jpaEntityHelper);
        modelGraphBuilder = new ModelGraphBuilder(entityImporter, graphTraversalSourceProvider, graphRequester);
    }

    @Test
    public void testModelGraphFromMetamodelGraph() {
        GraphTraversal<Vertex, Vertex> traversal = mock(GraphTraversal.class);
        when(graphTraversalSource.V()).thenReturn(traversal);
        when(graphTraversalSourceProvider.getGraph()).thenReturn(graphTraversalSource);
        modelGraphBuilder.modelGraphFromMetamodelGraph(entityMetamodelGraph);
        verify(graphTraversalSourceProvider).getGraph();
        verifyNoMoreInteractions(graphTraversalSourceProvider);
    }


    @Test
    public void testCreateEdges() {
        // Arrange
        MetamodelVertex metamodelVertex = new JpaMetamodelVertex(MetamodelClass.class, FakeEntity.class, null, null);
        FieldEdge fieldEdge = new FieldEdge(null, null);
        Collection<FieldEdge> edges = Arrays.asList(fieldEdge);
        metamodelVertex.computeOutboundEdges(entityMetamodelGraph);
        Set<MetamodelVertex> metaVertex = new HashSet<>();
        metaVertex.add(metamodelVertex);
        when(graphTraversalSource.V()).thenReturn(new DefaultGraphTraversal());
        when(entityMetamodelGraph.vertexSet()).thenReturn(metaVertex);
        // Act
        modelGraphBuilder.createEdges(entityMetamodelGraph, graphTraversalSource);
        // Assert
        verify(graphTraversalSource, times(1)).V();
    }

}
