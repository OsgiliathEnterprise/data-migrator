package net.osgiliath.migrator.core;


import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.db.inject.SinkEntityInjector;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphBuilder;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphRequester;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder;
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
public class TransformationSequencer implements CommandLineRunner  {

    private static final Logger log = LoggerFactory.getLogger(TransformationSequencer.class);
    private final MetamodelScanner metamodelScanner;
    private final MetamodelGraphBuilder graphBuilder;
    private final MetamodelGraphRequester graphRequester;
    private final ModelGraphBuilder modelGraphBuilder;
    private final SequenceProcessor sequenceProcessor;
    private final SinkEntityInjector sinkEntityInjector;

    public TransformationSequencer(MetamodelScanner metamodelScanner, MetamodelGraphBuilder graphProcessor, MetamodelGraphRequester graphRequester, ModelGraphBuilder modelGraphBuilder, SequenceProcessor sequenceProcessor, SinkEntityInjector sinkEntityInjector) {
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
        Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph = graphBuilder.metamodelGraphFromEntityMetamodel(metamodelClasses);
        graphRequester.displayGraphWithGraphiz(entityMetamodelGraph);
        try (GraphTraversalSource modelGraph = modelGraphBuilder.modelGraphFromMetamodelGraph(entityMetamodelGraph)) {
            sequenceProcessor.process(modelGraph);
            sinkEntityInjector.persist(modelGraph, entityMetamodelGraph);
        }
    }
}
