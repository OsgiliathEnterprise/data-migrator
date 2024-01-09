package net.osgiliath.migrator.core.processing;

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

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.configuration.AbstractTransformationConfigurationDefinition;
import net.osgiliath.migrator.core.api.model.ModelElement;
import org.jgrapht.Graph;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SequencerFactory {

    private final List<FactorySequencer> factorySequencers;

    public SequencerFactory(List<FactorySequencer> factorySequencers) {
        this.factorySequencers = factorySequencers;
    }


    public Object createSequencerBean(Class beanClass, AbstractTransformationConfigurationDefinition definition, Graph<MetamodelVertex, FieldEdge> graph, MetamodelVertex metamodelVertex, ModelElement entity, String columnName) {
        return factorySequencers.stream().filter(factorySequencer -> factorySequencer.canHandle(beanClass)).findFirst().orElseThrow(() -> new RuntimeException("No factory sequencer found for " + beanClass)).createSequencerBean(beanClass, definition, graph, metamodelVertex, entity, columnName);
    }
}
