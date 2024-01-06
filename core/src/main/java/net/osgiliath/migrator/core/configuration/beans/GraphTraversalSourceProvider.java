package net.osgiliath.migrator.core.configuration.beans;

import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.configuration.model.GraphDatasourceType;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.springframework.stereotype.Component;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

@Component
public class GraphTraversalSourceProvider {

    private final DataMigratorConfiguration dataMigratorConfiguration;

    public GraphTraversalSourceProvider(DataMigratorConfiguration dataMigratorConfiguration) {

        this.dataMigratorConfiguration = dataMigratorConfiguration;
    }
    
    public GraphTraversalSource getGraph() {
       if (dataMigratorConfiguration.getGraphDatasource().getType() == GraphDatasourceType.EMBEDDED) {
            Graph graph = TinkerGraph.open();
            return traversal().withEmbedded(graph);
        }
        else {
           throw new UnsupportedOperationException("Only embedded graph is supported for now");
       }
    }
}
