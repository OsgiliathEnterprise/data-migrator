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
import net.osgiliath.migrator.core.configuration.ColumnTransformationDefinition;
import net.osgiliath.migrator.core.configuration.SequencerDefinition;
import net.osgiliath.migrator.core.api.model.ModelElement;
import org.jgrapht.Graph;

/**
 * Factory for sequencers: will create a dedicated object to handle the model element anonymization.
 */
public interface FactorySequencer {
    /**
     * Checks if the factory can handle the bean class.
     * @param beanClass the bean class.
     * @return true if the factory can handle the bean class.
     */
    boolean canHandle(Class beanClass);

    /**
     * Creates the sequencer bean.
     * @param beanClass class of the sequencer.
     * @param definition the Configuration of the sequencer.
     * @param graph the metamodel graph.
     * @param metamodelVertex the metamodel vertex representing the entity definition   .
     * @param entity the entity to be handled by the sequencer.
     * @param columnTransformationDefinition the column name and options to be handled by the sequencer.
     * @return the resulting configured sequencer bean.
     */
    Object createSequencerBean(Class beanClass, SequencerDefinition definition, Graph<MetamodelVertex, FieldEdge> graph, MetamodelVertex metamodelVertex, ModelElement entity, ColumnTransformationDefinition columnTransformationDefinition);
}
