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

import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.osgiliath.migrator.core.modelgraph.ModelElementFactory;
import net.osgiliath.migrator.core.modelgraph.model.ModelElement;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

@Component
public class JpaEntityImporter implements EntityImporter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JpaEntityImporter.class);
    private final JpaEntityHelper hibernateEntityHelper;
    private final ModelElementFactory modelElementFactory;

    @PersistenceContext(unitName = "source")
    private EntityManager entityManager;

    public JpaEntityImporter(JpaEntityHelper hibernateEntityHelper, ModelElementFactory modelElementFactory) {
        this.hibernateEntityHelper = hibernateEntityHelper;
        this.modelElementFactory = modelElementFactory;
    }

    public List<ModelElement> importEntities(MetamodelVertex entityVertex, List<ModelElement> objectToExclude) {
        log.info("Importing entity {}", entityVertex.getTypeName());
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> query = builder.createQuery(((JpaMetamodelVertex)entityVertex).getEntityClass());
        Root root = query.from(((JpaMetamodelVertex)entityVertex).getEntityClass());
        CriteriaQuery<?> select = query.select(root);
        if (!objectToExclude.isEmpty()) { // Not sure it really works
            List<Predicate> predicates = excludeAlreadyLoaded(entityVertex, objectToExclude, builder, root);
            predicates.add(builder.in(root).value(objectToExclude));
            select.where(predicates.toArray(new Predicate[predicates.size()]));
        }
        List<?> resultList = entityManager.createQuery(select).getResultList();
        log.info("Found {} results when querying source datasource for entity {}", resultList.size(), entityVertex.getTypeName());
        return resultList.stream().map(object -> modelElementFactory.createModelElement(object)).toList();
    }

    private List<Predicate> excludeAlreadyLoaded(MetamodelVertex entityVertex, List<ModelElement> objectToExclude, CriteriaBuilder builder, Root root) {
        String primaryKeyField = ((JpaMetamodelVertex)entityVertex).getPrimaryKeyField();
        return objectToExclude.stream()
            .map(object ->
                        builder.not(
                            builder.equal(
                                root.get(primaryKeyField), object.getId()))
            ).toList();

    }
}
