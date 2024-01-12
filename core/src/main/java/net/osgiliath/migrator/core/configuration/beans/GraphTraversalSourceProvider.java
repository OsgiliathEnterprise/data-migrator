package net.osgiliath.migrator.core.configuration.beans;

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

import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.configuration.model.GraphDatasourceType;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.springframework.stereotype.Component;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;
/**
 * Tinkerpop Graph traversal source provider.
 */
@Component
public class GraphTraversalSourceProvider {

    /**
     * The data migrator configuration.
     */
    private final DataMigratorConfiguration dataMigratorConfiguration;

    /**
     * Constructor.
     * @param dataMigratorConfiguration the data migrator configuration.
     */
    public GraphTraversalSourceProvider(DataMigratorConfiguration dataMigratorConfiguration) {
        this.dataMigratorConfiguration = dataMigratorConfiguration;
    }
    /**
     * Gets the graph traversal source (graph instance) in regards to configuration.
     * @return the graph traversal source.
     */
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
