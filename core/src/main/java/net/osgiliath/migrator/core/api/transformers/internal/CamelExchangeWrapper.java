package net.osgiliath.migrator.core.api.transformers.internal;

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

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the processor into .
 *
 * @param <COLUMN_TYPE>
 */
public abstract class CamelExchangeWrapper<COLUMN_TYPE> implements Expression {

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(CamelExchangeWrapper.class);

    /**
     * Evaluate the data transformation on the given column.
     *
     * @param toBeTransformed the column to be transformed.
     * @return the transformed column.
     */
    public abstract COLUMN_TYPE evaluate(COLUMN_TYPE toBeTransformed);

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        log.debug("Transforming cell with transformer {}", this.getClass().getName());
        COLUMN_TYPE preValue = (COLUMN_TYPE) exchange.getIn().getBody(type);
        log.debug("Original cell value {}", preValue);
        COLUMN_TYPE transformedValue = evaluate(preValue);
        log.debug("Post transformation value {}", transformedValue);
        return (T) transformedValue;
    }

}
