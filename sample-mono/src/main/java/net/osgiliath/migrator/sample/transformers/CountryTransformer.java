package net.osgiliath.migrator.sample.transformers;

import jakarta.persistence.metamodel.SingularAttribute;
import net.osgiliath.datamigrator.sample.domain.Country;
import net.osgiliath.datamigrator.sample.domain.Country_;
import net.osgiliath.migrator.core.api.transformers.MetamodelColumnCellTransformer;
import org.springframework.stereotype.Component;

@Component
public class CountryTransformer extends MetamodelColumnCellTransformer<Country, String, SingularAttribute<Country, String>> {
    @Override
    public SingularAttribute<Country, String> column() {
        return Country_.countryName;
    }

    @Override
    public String evaluate(String toBeTransformed) {
        return "";
    }

}
