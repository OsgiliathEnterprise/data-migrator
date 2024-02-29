package net.osgiliath.migrator.core.modelgraph;

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.configuration.beans.GraphTraversalSourceProvider;
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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        modelGraphBuilder = new ModelGraphBuilder(entityImporter, graphTraversalSourceProvider);
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

    class MetamodelClass {
    }

    class EntityClass {
    }

    @Test
    public void testCreateEdges() {
        // Arrange
        MetamodelVertex metamodelVertex = new JpaMetamodelVertex(MetamodelClass.class, EntityClass.class, null, null);
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
