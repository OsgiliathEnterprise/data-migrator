package net.osgiliath.migrator.modules.faker;

/*-
 * #%L
 * faker
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

import net.datafaker.Faker;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.configuration.ColumnTransformationDefinition;
import net.osgiliath.migrator.core.configuration.SequencerDefinition;
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import net.osgiliath.migrator.core.processing.FactorySequencer;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.jgrapht.Graph;
import org.springframework.stereotype.Component;

@Component
public class ColumnFakerFactorySequencer implements FactorySequencer {

    private final ModelElementProcessor modelElementProcessor;
    private final RawElementProcessor rawElementProcessor;
    private final Faker faker;

    public ColumnFakerFactorySequencer(ModelElementProcessor modelElementProcessor, RawElementProcessor rawElementProcessor, Faker faker) {

        this.modelElementProcessor = modelElementProcessor;
        this.rawElementProcessor = rawElementProcessor;
        this.faker = faker;
    }

    @Override
    public boolean canHandle(Class beanClass) {
        return ColumnFaker.class.isAssignableFrom(beanClass);
    }

    @Override
    public Object createSequencerBean(Class beanClass, SequencerDefinition definition, Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> graph, MetamodelVertex metamodelVertex, ModelElement entity, ColumnTransformationDefinition columnTransformationDefinition) {
        return new ColumnFaker(modelElementProcessor, metamodelVertex, columnTransformationDefinition, rawElementProcessor, faker);
    }
}
