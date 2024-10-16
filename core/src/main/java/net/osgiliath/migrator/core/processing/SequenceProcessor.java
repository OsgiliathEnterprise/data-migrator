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
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.api.transformers.GraphTransformer;
import net.osgiliath.migrator.core.api.transformers.MetamodelColumnCellTransformer;
import net.osgiliath.migrator.core.api.transformers.ModelElementColumnTransformer;
import net.osgiliath.migrator.core.configuration.ColumnTransformationDefinition;
import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.configuration.TRANSFORMER_TYPE;
import net.osgiliath.migrator.core.exception.RawElementFieldOrMethodNotFoundException;
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import net.osgiliath.migrator.core.graph.VertexResolver;
import net.osgiliath.migrator.core.processing.model.SequencerBeanMetamodelVertexAndEntity;
import net.osgiliath.migrator.core.processing.model.SequencerDefinitionAndBean;
import net.osgiliath.migrator.core.processing.model.SequencersBeansMetamodelVertexAndEntity;
import net.osgiliath.migrator.core.processing.model.VertexAndSequencerBeanClass;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.jgrapht.Graph;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.HashSet;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;
import static net.osgiliath.migrator.core.configuration.SequencerDefinition.WILDCARD;

@Component
public class SequenceProcessor {
    private final DataMigratorConfiguration dataMigratorConfiguration;
    private final ApplicationContext context;
    private final SequencerFactory sequencerFactory;
    private final ModelElementProcessor modelElementProcessor;
    private final VertexResolver vertexResolver;
    private final PlatformTransactionManager sourcePlatformTxManager;

    public SequenceProcessor(DataMigratorConfiguration dataMigratorConfiguration, ApplicationContext context, SequencerFactory sequencerFactory, ModelElementProcessor modelElementProcessor, VertexResolver vertexResolver, @Qualifier(SOURCE_TRANSACTION_MANAGER) PlatformTransactionManager sourcePlatformTxManager) {
        this.dataMigratorConfiguration = dataMigratorConfiguration;
        this.context = context;
        this.sequencerFactory = sequencerFactory;
        this.modelElementProcessor = modelElementProcessor;
        this.vertexResolver = vertexResolver;
        this.sourcePlatformTxManager = sourcePlatformTxManager;
    }

    public void process(GraphTraversalSource modelGraph, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> metamodelGraph) {
        TransactionTemplate tpl = new TransactionTemplate(sourcePlatformTxManager);
        tpl.setReadOnly(true);
        tpl.executeWithoutResult(status -> { // TODO refine
            dataMigratorConfiguration.getSequence().stream()
                    .flatMap(sequenceName -> dataMigratorConfiguration.getSequencers().stream().filter(seq -> seq.getName().equals(sequenceName)))
                    .map(seq -> {
                                try {
                                    return new SequencerDefinitionAndBean(seq, Class.forName(seq.getTransformerClass()));
                                } catch (ClassNotFoundException e) {
                                    throw new RawElementFieldOrMethodNotFoundException(e);
                                }
                            }
                    )
                    .map(seqAndBeanClass -> {
                        Class transformerClass = seqAndBeanClass.beanClass();
                        if (WILDCARD.equals(seqAndBeanClass.sequencerConfiguration().getEntityClass()) && GraphTransformer.class.isAssignableFrom(transformerClass)) {
                            context.getBeansOfType(transformerClass).values().stream().forEach(
                                    globalSequencerBean -> ((GraphTransformer) globalSequencerBean).evaluate(modelGraph, metamodelGraph, seqAndBeanClass.sequencerConfiguration().getSequencerOptions())
                            );
                        }
                        return seqAndBeanClass;
                    })
                    .flatMap(sequencerAndBeanClass -> modelGraph.V().hasLabel(sequencerAndBeanClass.sequencerConfiguration().getEntityClass()).toStream()
                            .parallel().map(vertex -> new VertexAndSequencerBeanClass(vertex, sequencerAndBeanClass)))
                    .map(vertexAndSequencerBean -> {
                        MetamodelVertex metamodelVertex = vertexResolver.getMetamodelVertex(vertexAndSequencerBean.vertex());
                        ModelElement entity = vertexResolver.getModelElement(vertexAndSequencerBean.vertex());
                        Collection beans = findSequencerBeans(metamodelGraph, vertexAndSequencerBean, metamodelVertex, entity);
                        return new SequencersBeansMetamodelVertexAndEntity(beans, metamodelVertex, entity);
                    }).flatMap(sequencersBeansMetamodelVertexAndEntity ->
                            sequencersBeansMetamodelVertexAndEntity.beans().stream()
                                    .map(bean -> new SequencerBeanMetamodelVertexAndEntity(bean, sequencersBeansMetamodelVertexAndEntity.metamodelVertex(), sequencersBeansMetamodelVertexAndEntity.entity())))
                    .forEach(sbmvae -> {
                        SequencerBeanMetamodelVertexAndEntity sequencerBeanMetamodelVertexAndEntity = (SequencerBeanMetamodelVertexAndEntity) sbmvae;
                        if (sequencerBeanMetamodelVertexAndEntity.bean() instanceof MetamodelColumnCellTransformer) {
                            processMetamodelCellTransformer((MetamodelColumnCellTransformer<?, ?, ?>) sequencerBeanMetamodelVertexAndEntity.bean(), sequencerBeanMetamodelVertexAndEntity.entity());
                        } else if (sequencerBeanMetamodelVertexAndEntity.bean() instanceof ModelElementColumnTransformer ject) {
                            processJpaEntityColumnTransformer(ject, sequencerBeanMetamodelVertexAndEntity.entity());
                        }
                    });
        });
    }

    private Collection findSequencerBeans(Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> metamodelGraph, VertexAndSequencerBeanClass vertexAndSequencerBean, MetamodelVertex metamodelVertex, ModelElement entity) {
        Collection beans = new HashSet<>();
        if (vertexAndSequencerBean.definition().getType().equals(TRANSFORMER_TYPE.BEAN)) {
            beans.addAll(context.getBeansOfType(vertexAndSequencerBean.beanClass()).values());
        } else if (vertexAndSequencerBean.definition().getType().equals(TRANSFORMER_TYPE.FACTORY)) {
            if (vertexAndSequencerBean.definition().getColumnTransformationDefinitions().isEmpty()) {
                beans.add(sequencerFactory.createSequencerBean(vertexAndSequencerBean.beanClass(), vertexAndSequencerBean.definition(), metamodelGraph, metamodelVertex, entity, null));
            } else {
                for (ColumnTransformationDefinition columnName : vertexAndSequencerBean.definition().getColumnTransformationDefinitions()) {
                    beans.add(sequencerFactory.createSequencerBean(vertexAndSequencerBean.beanClass(), vertexAndSequencerBean.definition(), metamodelGraph, metamodelVertex, entity, columnName));
                }
            }
        }
        return beans;
    }

    private void processJpaEntityColumnTransformer(ModelElementColumnTransformer<Object> bean, ModelElement entity) {
        bean.evaluate(entity);
    }

    private void processMetamodelCellTransformer(MetamodelColumnCellTransformer transformerBean, ModelElement entity) {
        Object value = modelElementProcessor.getFieldRawValue(entity, transformerBean.columnName());
        Object transformedValue = transformerBean.evaluate(value);
        modelElementProcessor.setFieldRawValue(entity, transformerBean.columnName(), transformedValue);
    }
}
