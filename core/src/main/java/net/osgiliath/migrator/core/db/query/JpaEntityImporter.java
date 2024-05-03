package net.osgiliath.migrator.core.db.query;

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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.graph.ModelElementFactory;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_PU;

/**
 * Imports entities from the database.
 */
@Component
public class JpaEntityImporter implements EntityImporter {

    /**
     * Logger.
     */
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JpaEntityImporter.class);
    /**
     * Model element factory (to create model elements from the entries.
     */
    private final ModelElementFactory modelElementFactory;
    private final RawElementProcessor elementProcessor;

    /**
     * Source Entity manager.
     */
    @PersistenceContext(unitName = SOURCE_PU)
    private EntityManager entityManager;

    /**
     * Constructor.
     *
     * @param modelElementFactory model element factory
     */
    public JpaEntityImporter(ModelElementFactory modelElementFactory, RawElementProcessor elementProcessor) {
        this.modelElementFactory = modelElementFactory;
        this.elementProcessor = elementProcessor;
    }

    /**
     * Imports entities from the database.
     *
     * @param entityVertex    entity vertex (definition of the entity to import)
     * @param objectToExclude objects to exclude
     * @return list of model elements
     */
    // @Transactional(readOnly = true, transactionManager = SOURCE_TRANSACTION_MANAGER, propagation = Propagation.REQUIRES_NEW)
    public List<ModelElement> importEntities(MetamodelVertex entityVertex, List<ModelElement> objectToExclude) {
        log.info("Importing entity {}", entityVertex.getTypeName());
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> query = null;
        Class entityClass = ((JpaMetamodelVertex) entityVertex).getEntityClass();
        query = builder.createQuery(entityClass);// Didn't find any better idea
        Root root = query.from(entityClass);
        CriteriaQuery<?> select = query.select(root);
        if (!objectToExclude.isEmpty()) { // Not sure it really works
            List<Predicate> predicates = excludeAlreadyLoaded(entityVertex, objectToExclude, builder, root);
            predicates.add(builder.in(root).value(objectToExclude));
            select.where(predicates.toArray(new Predicate[predicates.size()]));
        }
        List<?> resultList = new ArrayList<>();
        try {
            resultList = entityManager.createQuery(select).getResultList();
        } catch (Exception e) {
            log.error("Error when querying source datasource for entity {}", entityVertex.getTypeName(), e);
        }
        log.info("Found {} results when querying source datasource for entity {}", resultList.size(), entityVertex.getTypeName());
        return resultList.stream().map(modelElementFactory::createModelElement).toList();
    }

    /**
     * Excludes already loaded objects from the database query.
     *
     * @param entityVertex    entity vertex (definition of the entity to import)
     * @param objectToExclude objects to exclude
     * @param builder         criteria builder
     * @param root            root
     * @return list of predicates
     */
    private List<Predicate> excludeAlreadyLoaded(MetamodelVertex entityVertex, List<ModelElement> objectToExclude, CriteriaBuilder builder, Root root) {
        Optional<String> primaryKeyField = getPrimaryKeyField(((JpaMetamodelVertex) entityVertex));
        return primaryKeyField.map(pk -> objectToExclude.stream()
                .map(object ->
                        builder.not(
                                builder.equal(
                                        root.get(pk), object.getId(entityVertex)))
                ).toList()).orElseGet(ArrayList::new);
    }

    /**
     * {@inheritDoc}
     */
    private Optional<String> getPrimaryKeyField(JpaMetamodelVertex jpaMetamodelVertex) {
        return elementProcessor.getPrimaryKeyFieldName(jpaMetamodelVertex.getEntityClass());
    }

}
