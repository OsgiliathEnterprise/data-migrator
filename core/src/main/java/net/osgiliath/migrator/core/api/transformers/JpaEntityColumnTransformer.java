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
import net.osgiliath.migrator.core.api.model.ModelElement;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JpaEntityColumnTransformer<FIELD_TYPE> extends CamelExchangeWrapper<ModelElement> implements ColumnTransformer {
    private static final Logger log = LoggerFactory.getLogger(MetamodelColumnCellTransformer.class);
    private final MetamodelVertex metamodel;
    private final String columnName;
    private final Graph<MetamodelVertex, FieldEdge> metaModelGraph;

    public JpaEntityColumnTransformer(MetamodelVertex metamodel, String columnName, Graph<MetamodelVertex, FieldEdge> metaModelGraph) {
        this.metamodel = metamodel;
        this.columnName = columnName;
        this.metaModelGraph = metaModelGraph;
    }

    public String columnName() {
        return columnName;
    }

    @Override
    public ModelElement evaluate(ModelElement toBeTransformed) {
        Object rawValue = toBeTransformed.getFieldRawValue(metamodel, columnName);
        log.info("transforming Vertex of class {} with column {} and value {}", metamodel.getTypeName(), columnName, rawValue);
        Object transformedValue = evaluateField((FIELD_TYPE) rawValue);
        toBeTransformed.setFieldRawValue(metamodel, columnName, transformedValue);
        return toBeTransformed;
    }

    protected abstract FIELD_TYPE evaluateField(FIELD_TYPE fieldValue);
}
