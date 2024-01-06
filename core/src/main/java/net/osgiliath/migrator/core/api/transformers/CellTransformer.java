package net.osgiliath.migrator.core.api.transformers;

import jakarta.persistence.metamodel.Attribute;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CellTransformer<TABLE, COLUMN_TYPE, COLUMN extends Attribute<TABLE, COLUMN_TYPE>> implements Expression {
    private static final Logger log = LoggerFactory.getLogger(CellTransformer.class);
    public abstract COLUMN column();

    public abstract COLUMN_TYPE evaluate(COLUMN_TYPE toBeTransformed);

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        log.warn("Transforming cell of type {} with transformer {}", column().getJavaType().getName(), this.getClass().getName());
        COLUMN_TYPE preValue = (COLUMN_TYPE) exchange.getIn().getBody(type);
        log.debug("Original cell value {}", preValue.toString());
        COLUMN_TYPE transformedValue = evaluate(preValue);
        log.debug("Post transformation cell value {}", transformedValue.toString());
        return (T) transformedValue;
    }
}
