package net.osgiliath.migrator.core;

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


import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.db.inject.SinkEntityInjector;
import net.osgiliath.migrator.core.graph.ModelGraphBuilder;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphBuilder;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import net.osgiliath.migrator.core.processing.SequenceProcessor;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Order(1)
@Profile("!test")
public class TransformationSequencer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TransformationSequencer.class);
    private final MetamodelScanner metamodelScanner;
    private final MetamodelGraphBuilder graphBuilder;
    private final MetamodelRequester graphRequester;
    private final ModelGraphBuilder modelGraphBuilder;
    private final SequenceProcessor sequenceProcessor;
    private final SinkEntityInjector sinkEntityInjector;

    public TransformationSequencer(MetamodelScanner metamodelScanner, MetamodelGraphBuilder graphProcessor, MetamodelRequester graphRequester, ModelGraphBuilder modelGraphBuilder, SequenceProcessor sequenceProcessor, SinkEntityInjector sinkEntityInjector) {
        this.metamodelScanner = metamodelScanner;
        this.graphBuilder = graphProcessor;
        this.graphRequester = graphRequester;
        this.modelGraphBuilder = modelGraphBuilder;
        this.sequenceProcessor = sequenceProcessor;
        this.sinkEntityInjector = sinkEntityInjector;
    }

    @Override
    public void run(String... args) throws Exception {
        log.warn("Starting the anonymization sequence");
        Collection<Class<?>> metamodelClasses = metamodelScanner.scanMetamodelClasses();
        Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph = graphBuilder.metamodelGraphFromRawElementClasses(metamodelClasses);
        graphRequester.displayGraphWithGraphiz(entityMetamodelGraph);
        try (GraphTraversalSource modelGraph = modelGraphBuilder.modelGraphFromMetamodelGraph(entityMetamodelGraph)) {
            sequenceProcessor.process(modelGraph, entityMetamodelGraph);
            sinkEntityInjector.persist(modelGraph, entityMetamodelGraph);
        }
    }
}
