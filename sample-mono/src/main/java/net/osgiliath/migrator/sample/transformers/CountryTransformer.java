package net.osgiliath.migrator.sample.transformers;

import jakarta.persistence.metamodel.SingularAttribute;
import net.osgiliath.domain.Country;
import net.osgiliath.domain.Country_;
import net.osgiliath.migrator.core.api.transformers.CellTransformer;
import org.springframework.stereotype.Component;

@Component
public class CountryTransformer extends CellTransformer<Country, String, SingularAttribute<Country, String>> {
    @Override
    public SingularAttribute<Country, String> column() {
        return Country_.countryName;
    }

    @Override
    public String evaluate(String toBeTransformed) {
        return "";
    }

}
