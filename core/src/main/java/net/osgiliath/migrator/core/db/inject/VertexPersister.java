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
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import org.apache.tinkerpop.shaded.jackson.core.JsonProcessingException;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.stream.Stream;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SINK_TRANSACTION_MANAGER;

@Component
public class VertexPersister {

    private final PlatformTransactionManager sinkPlatformTxManager;
    private final ModelElementProcessor modelElementProcessor;
    private static Logger log = LoggerFactory.getLogger(VertexPersister.class);

    public VertexPersister(ModelElementProcessor modelElementProcessor, @Qualifier(SINK_TRANSACTION_MANAGER) PlatformTransactionManager sinkPlatformTxManager) {
        this.modelElementProcessor = modelElementProcessor;
        this.sinkPlatformTxManager = sinkPlatformTxManager;
    }

    @SuppressWarnings("java:S3864")
    public Stream<ModelElement> persistVertices(Stream<ModelElement> entities, PlatformTransactionManager sinkTxManager) {
        EntityManagerFactory emf = ((JpaTransactionManager) sinkTxManager).getEntityManagerFactory();
        log.info("******************** Persisting a batch of entities ****************");
        try {
            EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
            return entities
                    .peek((ModelElement me) -> {
                        log.debug("Persisting entity of type {}, with id {}", me.rawElement(), modelElementProcessor.getId(me).get());
                        if (log.isInfoEnabled()) {
                            ObjectMapper mapper = new ObjectMapper();
                            try {
                                log.info("Object passed to persist: {}", mapper.writer().writeValueAsString(me.rawElement()));
                            } catch (JsonProcessingException e) {
                                log.warn("Unable to serialize entity for logging", e);
                            }
                        }
                    })
                    .map((ModelElement me) -> {
                        //em.merge(me.rawElement());
                        me.setRawElement(em.merge(me.rawElement()));
                        return me;
                    });
        } catch (Exception e) {
            log.error("******************** ERROR Persisting last batch of entities ****************");
            log.warn("Unable to persist last batch of entities, you may retry once", e);
            log.error("******************** End error ****************");
        }
        return Stream.empty();
    }

    Optional<ModelElement> reattachEntityInSink(ModelElement entity) {
        JpaTransactionManager tm = (JpaTransactionManager) sinkPlatformTxManager;
        EntityManagerFactory emf = tm.getEntityManagerFactory();
        TransactionTemplate tpl = new TransactionTemplate(sinkPlatformTxManager);
        tpl.setReadOnly(true);
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
        return
                modelElementProcessor.getId(entity).map(
                        id -> new ModelElement(entity.vertex(), em.find(((JpaMetamodelVertex) entity.vertex()).entityClass(), id))
                );
    }
}
