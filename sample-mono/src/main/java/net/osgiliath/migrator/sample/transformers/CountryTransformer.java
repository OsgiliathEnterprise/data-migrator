package net.osgiliath.migrator.sample.transformers;

/*-
 * #%L
 * datamigrator-sample-mono
 * %%
 * Copyright (C) 2024 - 2025 Osgiliath Inc.
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
