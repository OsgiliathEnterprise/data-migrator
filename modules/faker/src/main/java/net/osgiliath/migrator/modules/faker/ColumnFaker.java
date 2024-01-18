package net.osgiliath.migrator.modules.faker;

/*-
 * #%L
 * faker
 * %%
 * Copyright (C) 2021 Osgiliath
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
import net.osgiliath.migrator.core.configuration.ColumnTransformationDefinition;

/**
 * Faker for columns.
 *
 * @param <T> the type of the column
 */
public class ColumnFaker extends AbstractFaker<Object> {


    /**
     * Constructor.
     *
     * @param metamodel                      the Vertex metamodel (class)
     * @param columnTransformationDefinition the column transformation definition
     */
    public ColumnFaker(MetamodelVertex metamodel, ColumnTransformationDefinition columnTransformationDefinition) {
        super(metamodel, columnTransformationDefinition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object evaluateField(Object fieldValue) {
        return super.fake(fieldValue.toString());
    }
}
