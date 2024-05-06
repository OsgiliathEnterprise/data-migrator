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

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.api.transformers.internal.CamelExchangeWrapper;
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Column transformer
 *
 * @param <F> the field type
 */
public abstract class ModelElementColumnTransformer<F> extends CamelExchangeWrapper<ModelElement> implements ColumnTransformer {
    private static final Logger log = LoggerFactory.getLogger(ModelElementColumnTransformer.class);
    private final ModelElementProcessor modelElementProcessor;
    private final MetamodelVertex metamodel;
    private final String columnName;

    protected ModelElementColumnTransformer(ModelElementProcessor modelElementProcessor, MetamodelVertex metamodel, String columnName) {
        this.modelElementProcessor = modelElementProcessor;
        this.metamodel = metamodel;
        this.columnName = columnName;
    }

    public String columnName() {
        return columnName;
    }

    @Override
    public ModelElement evaluate(ModelElement toBeTransformed) {
        Object rawValue = modelElementProcessor.getFieldRawValue(metamodel, columnName, toBeTransformed);
        log.info("transforming Vertex of class {} with column {} and value {}", metamodel.getTypeName(), columnName, rawValue);
        Object transformedValue = evaluateField((F) rawValue);
        modelElementProcessor.setFieldRawValue(metamodel, columnName, toBeTransformed, transformedValue);
        return toBeTransformed;
    }

    protected abstract F evaluateField(F fieldValue);
}
