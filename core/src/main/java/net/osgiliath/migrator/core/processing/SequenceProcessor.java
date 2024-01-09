package net.osgiliath.migrator.core.processing;

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
import net.osgiliath.migrator.core.api.transformers.JpaEntityColumnTransformer;
import net.osgiliath.migrator.core.api.transformers.MetamodelColumnCellTransformer;
import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.configuration.TRANSFORMER_TYPE;
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder;
import net.osgiliath.migrator.core.modelgraph.model.ModelElement;
import net.osgiliath.migrator.core.processing.model.SequencerDefinitionAndBean;
import net.osgiliath.migrator.core.processing.model.VertexAndSequencerBeanClass;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.jgrapht.Graph;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;

@Component
public class SequenceProcessor {
    private final DataMigratorConfiguration dataMigratorConfiguration;
    private final ApplicationContext context;
    private final JpaEntityHelper jpaEntityHelper;
    private final SequencerFactory sequencerFactory;

    public SequenceProcessor(DataMigratorConfiguration dataMigratorConfiguration, ApplicationContext context, JpaEntityHelper jpaEntityHelper, SequencerFactory sequencerFactory) {
        this.dataMigratorConfiguration = dataMigratorConfiguration;
        this.context = context;
        this.jpaEntityHelper = jpaEntityHelper;
        this.sequencerFactory = sequencerFactory;
    }

    @Transactional(transactionManager = "sourceTransactionManager")
    public void process(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge> metamodelGraph) {
        dataMigratorConfiguration.getSequence().stream()
            .flatMap(sequenceName -> dataMigratorConfiguration.getSequencers().stream().filter(seq -> seq.getName().equals(sequenceName)))
                .map(seq -> {
                            try {
                                return new SequencerDefinitionAndBean(seq, Class.forName(seq.getTransformerClass()));
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ).flatMap(sequencerAndBeanClass -> modelGraph.V().hasLabel(sequencerAndBeanClass.getSequencerConfiguration().getEntityClass()).toStream()
                        .parallel().map(vertex -> new VertexAndSequencerBeanClass(vertex, sequencerAndBeanClass)))
                .forEach(vertexAndSequencerBean -> {
                    MetamodelVertex metamodelVertex = vertexAndSequencerBean.getVertex().value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_METAMODEL_VERTEX);
                    Collection beans = new HashSet<>();
                    ModelElement entity = vertexAndSequencerBean.getVertex().value(ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY);
                    if (vertexAndSequencerBean.getDefinition().getType().equals(TRANSFORMER_TYPE.BEAN)) {
                        beans.addAll(context.getBeansOfType(vertexAndSequencerBean.getBeanClass()).values());
                    } else if (vertexAndSequencerBean.getDefinition().getType().equals(TRANSFORMER_TYPE.FACTORY)) {
                        if (vertexAndSequencerBean.getDefinition().getColumns().isEmpty()) {
                            beans.add(sequencerFactory.createSequencerBean(vertexAndSequencerBean.getBeanClass(), vertexAndSequencerBean.getDefinition(), metamodelGraph, metamodelVertex, entity, null));
                        } else {
                            for (String columnName : vertexAndSequencerBean.getDefinition().getColumns()) {
                                beans.add(sequencerFactory.createSequencerBean(vertexAndSequencerBean.getBeanClass(), vertexAndSequencerBean.getDefinition(), metamodelGraph, metamodelVertex, entity, columnName));
                            }
                        }
                    }
                    for (Object bean : beans) {
                        if (bean instanceof MetamodelColumnCellTransformer) {
                            processMetamodelCellTransformer((MetamodelColumnCellTransformer<?, ?, ?>) bean, entity, metamodelVertex);
                        } else if (bean instanceof JpaEntityColumnTransformer) {
                            processJpaEntityColumnTransformer((JpaEntityColumnTransformer) bean, entity);
                        }
                    }
                });
    }

    private void processJpaEntityColumnTransformer(JpaEntityColumnTransformer<Object> bean, ModelElement entity) {
        bean.evaluate(entity);
    }

    private void processMetamodelCellTransformer(MetamodelColumnCellTransformer transformerBean, ModelElement entity, MetamodelVertex metamodelVertex) {
        Object value = entity.getFieldRawValue(metamodelVertex, transformerBean.columnName());
        Object transformedValue = transformerBean.evaluate(value);
        entity.setFieldRawValue(metamodelVertex, transformerBean.columnName(), transformedValue);
    }
}
