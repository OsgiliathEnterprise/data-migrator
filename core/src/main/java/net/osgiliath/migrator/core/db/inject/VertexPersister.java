package net.osgiliath.migrator.core.db.inject;

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.configuration.model.GraphDatasourceType;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.stream.Stream;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SINK_TRANSACTION_MANAGER;
import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;

@Component
public class VertexPersister {

    private final boolean reconcile;
    private final PlatformTransactionManager sinkPlatformTxManager;
    private final PlatformTransactionManager sourcePlatformTxManager;
    private final RawElementProcessor rawElementProcessor;
    private static Logger log = LoggerFactory.getLogger(VertexPersister.class);

    public VertexPersister(DataMigratorConfiguration dmc, RawElementProcessor rawElementProcessor, @Qualifier(SINK_TRANSACTION_MANAGER) PlatformTransactionManager sinkPlatformTxManager, @Qualifier(SOURCE_TRANSACTION_MANAGER) PlatformTransactionManager sourcePlatformTxManager) {
        this.rawElementProcessor = rawElementProcessor;
        this.reconcile = dmc.getGraphDatasource().getType().equals(GraphDatasourceType.REMOTE);
        this.sinkPlatformTxManager = sinkPlatformTxManager;
        this.sourcePlatformTxManager = sourcePlatformTxManager;
    }

    public void persistVertices(Stream<ModelElement> entities) {
        Stream<?> reattachedEntities = reattachEntities(entities);
        TransactionTemplate tpl = new TransactionTemplate(sinkPlatformTxManager);
        JpaTransactionManager tm = (JpaTransactionManager) sinkPlatformTxManager;
        EntityManagerFactory emf = tm.getEntityManagerFactory();
        log.info("******************** Persisting a batch of entities ****************");
        try {
            tpl.executeWithoutResult(res -> {
                EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
                reattachedEntities
                        .forEach((ent) -> {
                                    log.debug("persisting entity of type {}, with id {}", ent.getClass(), rawElementProcessor.getId(null, ent).get());
                                    em.merge(ent);
                                }
                        );

            });
        } catch (Exception e) {
            log.error("******************** ERROR Persisting last batch of entities ****************");
            log.warn("Unable to persist last batch of entities, you may retry once", e);
            log.error("******************** End error ****************");
        }
    }

    private Stream<?> reattachEntities(Stream<ModelElement> entities) {
        if (reconcile) {
            JpaTransactionManager tm = (JpaTransactionManager) sourcePlatformTxManager;
            EntityManagerFactory emf = tm.getEntityManagerFactory();
            TransactionTemplate tpl = new TransactionTemplate(sourcePlatformTxManager);
            tpl.setReadOnly(true);
            return tpl.execute(status -> {
                EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
                return entities.flatMap(me ->
                        rawElementProcessor.getId(me).map(
                                id -> em.find(((JpaMetamodelVertex) me.vertex()).entityClass(), id)
                        ).stream()
                );
            });
        } else {
            return entities.map(ModelElement::rawElement);
        }
    }
}
