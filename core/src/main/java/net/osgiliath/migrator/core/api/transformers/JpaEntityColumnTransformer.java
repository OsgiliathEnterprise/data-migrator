package net.osgiliath.migrator.core.api.transformers;

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
import net.osgiliath.migrator.core.api.transformers.internal.CamelExchangeWrapper;
import net.osgiliath.migrator.core.api.transformers.internal.model.MetamodelVertexAndEntityAndColumnNameAndFieldValue;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public abstract class JpaEntityColumnTransformer<ENTITY_TYPE, FIELD_TYPE> extends CamelExchangeWrapper<ENTITY_TYPE> implements ColumnTransformer {
    private static final Logger log = LoggerFactory.getLogger(MetamodelColumnCellTransformer.class);
    private final String entityName;
    private final String columnName;

    private final Graph<MetamodelVertex, FieldEdge> metaModelGraph;

    public JpaEntityColumnTransformer(String entityName, String columnName, Graph<MetamodelVertex, FieldEdge> metaModelGraph) {
        this.entityName = entityName;
        this.columnName = columnName;
        this.metaModelGraph = metaModelGraph;
    }

    private Optional<MetamodelVertex> getCorrespondingMetamodelClass() {
        return metaModelGraph.vertexSet().stream().filter(v -> v.getTypeName().equals(entityName)).findAny();
    }

    public String columnName() {
        return columnName;
    }

    @Override
    public ENTITY_TYPE evaluate(ENTITY_TYPE toBeTransformed) {
        return getCorrespondingMetamodelClass().map(
            jpaMetamodelVertex -> {
                log.debug("Transforming entity with type {} with id {} column of name {} of with transformer {}", jpaMetamodelVertex.getTypeName(), jpaMetamodelVertex.getId(toBeTransformed), columnName(), this.getClass().getName());
                return new MetamodelVertexAndEntityAndColumnNameAndFieldValue<ENTITY_TYPE, FIELD_TYPE>(jpaMetamodelVertex, toBeTransformed, columnName, (FIELD_TYPE) ((JpaMetamodelVertex)jpaMetamodelVertex).getFieldValue(toBeTransformed, columnName()));
            }
        ).map(metamodelVertexAndEntityAndColumnNameAndFieldValue -> {
            ((JpaMetamodelVertex) metamodelVertexAndEntityAndColumnNameAndFieldValue.getJpaMetamodelVertex()).setFieldValue(metamodelVertexAndEntityAndColumnNameAndFieldValue.getEntity(), columnName, evaluateField(metamodelVertexAndEntityAndColumnNameAndFieldValue.getFieldValue()));
            return metamodelVertexAndEntityAndColumnNameAndFieldValue.getEntity();
        }).orElseThrow(() -> new RuntimeException("No metamodel class found for entity name " + entityName));
    }

    protected abstract FIELD_TYPE evaluateField(FIELD_TYPE fieldValue);
}
