package net.osgiliath.migrator.core.processing.model;

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

import net.osgiliath.migrator.core.configuration.SequencerDefinition;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class VertexAndSequencerBeanClass {
    private final Vertex vertex;
    private final SequencerDefinitionAndBean definitionAndBeanClass;

    public VertexAndSequencerBeanClass(Vertex vertex, SequencerDefinitionAndBean definitionAndBeanClass) {
        this.vertex = vertex;
        this.definitionAndBeanClass = definitionAndBeanClass;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public Class getBeanClass() {
        return definitionAndBeanClass.beanClass();
    }

    public SequencerDefinition getDefinition() {
        return definitionAndBeanClass.sequencerConfiguration();
    }
}
