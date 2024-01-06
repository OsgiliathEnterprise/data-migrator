package net.osgiliath.migrator.core.api.transformers;

import jakarta.persistence.metamodel.Attribute;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class ColumnTransformer <TABLE, COLUMN_TYPE, COLUMN extends Attribute<TABLE, COLUMN_TYPE>> implements Expression {
    private static final Logger log = LoggerFactory.getLogger(CellTransformer.class);
    private final CellTransformer cellTransformer;

    protected abstract COLUMN column();

    public ColumnTransformer(CellTransformer cellTransformer) {
        this.cellTransformer = cellTransformer;
    }
    public abstract List<COLUMN_TYPE> evaluate(List<COLUMN_TYPE> toBeTransformed);

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        log.warn("Transforming column of type {} with transformer {}", column().getJavaType().getName(), this.getClass().getName());
        List<COLUMN_TYPE> preValue = (List<COLUMN_TYPE>) exchange.getIn().getBody(type);
        if (log.isDebugEnabled()) {
            for (COLUMN_TYPE val : preValue) {
                log.debug("Original cell value {}", val.toString());
            }
        }
        List<COLUMN_TYPE> transformedValue = evaluate(preValue);
        if (log.isDebugEnabled()) {
            for (COLUMN_TYPE val : transformedValue) {
                log.debug("Post transformation value {}", val.toString());
            }
        }
        return (T) transformedValue;
    }
}
