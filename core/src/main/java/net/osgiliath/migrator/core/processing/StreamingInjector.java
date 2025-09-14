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

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.configuration.PerDSJpaProperties;
import net.osgiliath.migrator.core.db.inject.VertexPersister;
import net.osgiliath.migrator.core.processing.utils.BatchIterator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.*;

@Component
public class StreamingInjector {

    private final EntityImporter entityImporter;
    private final VertexPersister vertexPersister;
    private final PerDSJpaProperties jpaPropertiesWrapper;
    private final TransactionTemplate sourceTxTemplate;
    private final TransactionTemplate sinkTxTemplate;

    public StreamingInjector(@Qualifier(SOURCE_TRANSACTION_MANAGER) PlatformTransactionManager sourcePlatformTxManager, @Qualifier(SINK_TRANSACTION_MANAGER) PlatformTransactionManager sinkPlatformTxManager, EntityImporter entityImporter, VertexPersister vertexPersister, @Qualifier(SINK_JPA_PROPERTIES) PerDSJpaProperties jpaPropertiesWrapper) {
        this.entityImporter = entityImporter;
        this.vertexPersister = vertexPersister;
        this.jpaPropertiesWrapper = jpaPropertiesWrapper;
        this.sourceTxTemplate = new TransactionTemplate(sourcePlatformTxManager);
        sourceTxTemplate.setReadOnly(true);
        this.sinkTxTemplate = new TransactionTemplate(sinkPlatformTxManager);
    }

    public void injectVerticesInTargetDb(Set<MetamodelVertex> metamodelVertices) {
        metamodelVertices.forEach(
                this::injectVertexInTargetDb
        );
    }

    private void injectVertexInTargetDb(MetamodelVertex vertex) {
        Collection<List<ModelElement>> streamToInsert = sourceTxTemplate.execute(status -> {
            Stream<ModelElement> queried = entityImporter.importEntities(vertex, new HashSet<>());
            return BatchIterator.batchStreamOf(queried, Integer.parseInt(jpaPropertiesWrapper.getProperties().get("hibernate.jdbc.batch_size"))).collect(Collectors.toSet());
        });
        streamToInsert.forEach(batch -> {
            List<ModelElement> hydratedElements = entityImporter.hydrateElements(batch.stream());
            sinkTxTemplate.executeWithoutResult(status -> vertexPersister.persistVertices(hydratedElements.stream()));
        });
    }
}
