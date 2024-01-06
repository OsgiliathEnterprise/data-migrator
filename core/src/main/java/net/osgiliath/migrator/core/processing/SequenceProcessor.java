package net.osgiliath.migrator.core.processing;

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.transformers.CellTransformer;
import net.osgiliath.migrator.core.configuration.AbstractTransformationConfigurationDefinition;
import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@Component
public class SequenceProcessor {
    private final DataMigratorConfiguration dataMigratorConfiguration;
    private final ApplicationContext context;
    private final JpaEntityHelper jpaEntityHelper;

    public SequenceProcessor(DataMigratorConfiguration dataMigratorConfiguration, ApplicationContext context, JpaEntityHelper jpaEntityHelper) {
        this.dataMigratorConfiguration = dataMigratorConfiguration;
        this.context = context;
        this.jpaEntityHelper = jpaEntityHelper;
    }

    @Transactional(transactionManager = "sourceTransactionManager")
    public void process(GraphTraversalSource modelGraph) {
        dataMigratorConfiguration.getSequence().stream()
            .flatMap(sequenceName -> dataMigratorConfiguration.getSequencers().stream().filter(seq -> seq.getName().equals(sequenceName)))
                .map(seq -> {
                    try {
                        return new SequencerAndBean(seq, context.getBeansOfType(Class.forName(seq.getTransformerClass())).values().iterator().next());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).flatMap(sequencerAndBean -> modelGraph.V().hasLabel(sequencerAndBean.getSequencerConfiguration().getEntityClass()).toStream()
                        .parallel().map(vertex -> new VertexAndSequencerBean(vertex, sequencerAndBean.getBean())))
                .forEach(vertexAndSequencerBean -> {
                    Class entityClass = ((JpaMetamodelVertex) vertexAndSequencerBean.getVertex().value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_METAMODEL_VERTEX)).getEntityClass();
                    try {
                        Field attributeField = entityClass.getDeclaredField(String.valueOf(((CellTransformer<?, ?, ?>) vertexAndSequencerBean.getBean()).column().getName()));
                        Method getter = jpaEntityHelper
                                .getterMethod(entityClass, attributeField);
                        Object value = getter.invoke(vertexAndSequencerBean.getVertex().value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY));
                        Object transformedValue = ((CellTransformer)vertexAndSequencerBean.getBean()).evaluate(value);
                        Method setter = jpaEntityHelper
                            .setterMethod(entityClass, attributeField);
                        setter.invoke(vertexAndSequencerBean.getVertex().value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY), transformedValue);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
