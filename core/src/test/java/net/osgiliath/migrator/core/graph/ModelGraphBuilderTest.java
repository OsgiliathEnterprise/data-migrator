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
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.common.FakeEntity;
import net.osgiliath.migrator.core.common.MetamodelClass;
import net.osgiliath.migrator.core.configuration.ModelVertexCustomizer;
import net.osgiliath.migrator.core.configuration.beans.GraphTraversalSourceProvider;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RelationshipProcessor;
import net.osgiliath.migrator.core.rawelement.jpa.JpaEntityProcessor;
import net.osgiliath.migrator.core.rawelement.jpa.JpaRelationshipProcessor;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jgrapht.Graph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;


class ModelGraphBuilderTest {

    @Mock
    private EntityImporter entityImporter;

    @Mock
    private GraphTraversalSourceProvider graphTraversalSourceProvider;

    @Mock
    private Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph;

    @Mock
    private GraphTraversalSource graphTraversalSource;

    private TinkerpopModelGraphBuilder modelGraphBuilder;

    @Mock
    private PlatformTransactionManager txMgr;
    private JpaEntityProcessor jpaEntityHelper;
    private MetamodelRequester metamodelGraphRequester;
    private TinkerpopModelGraphEdgeBuilder modelGraphEdgeBuilder;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        jpaEntityHelper = new JpaEntityProcessor(txMgr);
        RelationshipProcessor relationshipProcessor = new JpaRelationshipProcessor(jpaEntityHelper);
        metamodelGraphRequester = new MetamodelRequester(jpaEntityHelper, relationshipProcessor);
        ModelElementProcessor modelElementProcessor = new ModelElementProcessor(jpaEntityHelper, metamodelGraphRequester, relationshipProcessor);
        VertexResolver resolver = new InGraphVertexResolver();
        ModelVertexInformationRetriever modelVertexInformationRetriever = new ModelVertexInformationRetriever(entityImporter, modelElementProcessor, txMgr);
        modelGraphEdgeBuilder = new TinkerpopModelGraphEdgeBuilder(relationshipProcessor, jpaEntityHelper, metamodelGraphRequester, resolver, modelElementProcessor, txMgr);
        modelGraphBuilder = new TinkerpopModelGraphBuilder(graphTraversalSourceProvider, new ModelVertexCustomizer(), resolver, modelVertexInformationRetriever, modelGraphEdgeBuilder);
    }

    @Test
    void testModelGraphFromMetamodelGraph() {
        GraphTraversal<Vertex, Vertex> traversal = mock(GraphTraversal.class);
        when(graphTraversalSource.inject(0)).thenReturn((GraphTraversal) traversal);
        when(graphTraversalSource.V()).thenReturn(traversal);
        when(traversal.count()).thenReturn((GraphTraversal) traversal);
        when(graphTraversalSourceProvider.getGraph()).thenReturn(graphTraversalSource);
        modelGraphBuilder.modelGraphFromMetamodelGraph(entityMetamodelGraph);
        verify(graphTraversalSourceProvider).getGraph();
        verifyNoMoreInteractions(graphTraversalSourceProvider);
    }


    @Test
    void testCreateEdges() {
        // Arrange
        MetamodelVertex metamodelVertex = new JpaMetamodelVertex(MetamodelClass.class, FakeEntity.class);
        FieldEdge<MetamodelVertex> fieldEdge = new FieldEdge(null);
        Collection<FieldEdge<MetamodelVertex>> edges = Arrays.asList(fieldEdge);
        Set<MetamodelVertex> metaVertex = new HashSet<>();
        metaVertex.add(metamodelVertex);
        GraphTraversal g = new DefaultGraphTraversal();
        when(graphTraversalSource.inject(0)).thenReturn(g);
        when(graphTraversalSource.V()).thenReturn(g);
        when(entityMetamodelGraph.vertexSet()).thenReturn(metaVertex);
        // Act
        modelGraphBuilder.createEdges(entityMetamodelGraph, graphTraversalSource);
        // Assert
        verify(graphTraversalSource, times(1)).V();
    }

}
