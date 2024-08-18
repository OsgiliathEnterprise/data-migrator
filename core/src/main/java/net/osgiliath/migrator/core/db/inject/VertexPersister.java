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
import jakarta.persistence.PersistenceContext;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.configuration.model.GraphDatasourceType;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Stream;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SINK_PU;
import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SINK_TRANSACTION_MANAGER;

@Component
public class VertexPersister {

    private final boolean reconcile;
    private final RawElementProcessor rawElementProcessor;
    @PersistenceContext(unitName = SINK_PU)
    private EntityManager entityManager;

    public VertexPersister(DataMigratorConfiguration dmc, RawElementProcessor rawElementProcessor) {
        this.rawElementProcessor = rawElementProcessor;
        this.reconcile = dmc.getGraphDatasource().getType().equals(GraphDatasourceType.REMOTE);
    }

    @Transactional(transactionManager = SINK_TRANSACTION_MANAGER)
    public void persistVertices(Stream<ModelElement> entities) {
        entities.flatMap(me -> {
                    if (reconcile) {
                        return rawElementProcessor.getId(me).map(
                                id -> entityManager.find(((JpaMetamodelVertex) me.vertex()).entityClass(), id)
                        ).stream();
                    }
                    return Optional.of(me.rawElement()).stream();
                })
                .forEach(entityManager::persist);
    }
}
