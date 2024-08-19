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
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.jgrapht.Graph;

public interface ModelGraphBuilder {
    String MODEL_GRAPH_VERTEX_ENTITY_ID = "rawid";
    String MODEL_GRAPH_VERTEX_METAMODEL_VERTEX = "metamodelVertex";
    String MODEL_GRAPH_VERTEX_ENTITY = "entity";

    GraphTraversalSource modelGraphFromMetamodelGraph(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> metamodelGraph);
}
