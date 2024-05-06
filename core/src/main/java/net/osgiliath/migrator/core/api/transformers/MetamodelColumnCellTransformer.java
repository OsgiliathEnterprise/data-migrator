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

import jakarta.persistence.metamodel.Attribute;
import net.osgiliath.migrator.core.api.transformers.internal.CamelExchangeWrapper;

/**
 * Modify a cell.
 *
 * @param <T> the table
 * @param <F> the column element type
 * @param <C> the column
 */
public abstract class MetamodelColumnCellTransformer<T, F, C extends Attribute<T, F>> extends CamelExchangeWrapper<F> implements ColumnTransformer {
    public abstract C column();

    @Override
    public String columnName() {
        return column().getName();
    }

    public abstract F evaluate(F toBeTransformed);


}
