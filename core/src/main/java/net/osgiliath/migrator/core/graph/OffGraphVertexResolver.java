package net.osgiliath.migrator.core.graph;

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
import jakarta.persistence.criteria.Root;
import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertexFactory;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import net.osgiliath.migrator.core.rawelement.jpa.JpaEntityProcessor;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_PU;
import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;
import static net.osgiliath.migrator.core.graph.ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID;

public class OffGraphVertexResolver implements VertexResolver {

    private static final Logger log = LoggerFactory.getLogger(OffGraphVertexResolver.class);
    private final JpaMetamodelVertexFactory jpaMetamodelVertexFactory;
    private final MetamodelScanner metamodelScanner;
    private final ModelElementFactory modelElementFactory;
    private final RawElementProcessor rawElementProcessor;
    private final Map<String, Object> cachedIds;
    /**
     * Source Entity manager.
     */
    @PersistenceContext(unitName = SOURCE_PU)
    private EntityManager entityManager;

    public OffGraphVertexResolver(JpaMetamodelVertexFactory jpaMetamodelVertexFactory, MetamodelScanner metamodelScanner, ModelElementFactory modelElementFactory, RawElementProcessor rawElementProcessor) {
        this.jpaMetamodelVertexFactory = jpaMetamodelVertexFactory;
        this.metamodelScanner = metamodelScanner;
        this.modelElementFactory = modelElementFactory;
        this.rawElementProcessor = rawElementProcessor;
        this.cachedIds = new HashMap<>();
    }

    @Override
    public MetamodelVertex getMetamodelVertex(Vertex vertex) {
        String className = vertex.label() + "_";
        return jpaMetamodelVertexFactory.createMetamodelVertex(metamodelScanner.scanMetamodelClasses().parallelStream().filter(clazz -> clazz.getName().equals(className)).findAny().get());
    }

    @Override
    public GraphTraversal setMetamodelVertex(GraphTraversal traversal, MetamodelVertex metamodelVertex) {
        return traversal;
    }

    @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    @Override
    public ModelElement getModelElement(Vertex vertex) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> query = null;
        MetamodelVertex mvtx = getMetamodelVertex(vertex);
        Class entityClass = ((JpaMetamodelVertex) mvtx).entityClass();
        String idFieldName = JpaEntityProcessor.getterToFieldName(rawElementProcessor.getPrimaryKeyGetterMethod(entityClass).get().getName());
        query = builder.createQuery(entityClass);// Didn't find any better idea
        Root root = query.from(entityClass);
        CriteriaQuery<?> select = query.select(root);
        select.where(builder.equal(root.get(idFieldName), getId(vertex)));
        Object resultList;
        try {
            resultList = entityManager.createQuery(select).getSingleResult();
            return modelElementFactory.createModelElement(mvtx, resultList);
        } catch (Exception e) {
            log.error("Error when querying source datasource for entity {}", mvtx.getTypeName(), e);
        }
        return null;
    }

    @Override
    public GraphTraversal setModelElement(GraphTraversal traversal, ModelElement modelElement) {
        return traversal;
    }

    @Override
    public GraphTraversal setId(GraphTraversal traversal, Object id) {
        Object idToSet = id;
        if (!((idToSet instanceof Long) || (idToSet instanceof String) || (idToSet instanceof UUID))) {
            idToSet = "cachedId-" + UUID.randomUUID();
            cachedIds.put((String) idToSet, id);
        }
        return traversal.property(MODEL_GRAPH_VERTEX_ENTITY_ID, idToSet);
    }

    @Override
    public Object getId(Vertex vtx) {
        Object idToGet = vtx.value(MODEL_GRAPH_VERTEX_ENTITY_ID);
        if (idToGet instanceof String s && s.startsWith("cachedId-")) {
            return cachedIds.get(s);
        }
        return idToGet;
    }

    @Override
    public Object getWrappedRawId(Object originalId) {
        return cachedIds.entrySet().stream().filter(entry -> entry.getValue().equals(originalId)).map(Map.Entry::getValue).findAny().orElse(originalId);
    }

    @Override
    public void clear() {
        this.entityManager.clear();
    }

}
