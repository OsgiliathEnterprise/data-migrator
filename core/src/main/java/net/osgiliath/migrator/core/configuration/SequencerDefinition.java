package net.osgiliath.migrator.core.configuration;

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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Configuration of the transformation sequences.
 */
public class SequencerDefinition {
    public static final String WILDCARD = "*";
    /**
     * Name of the transformation sequencer.
     */
    private String name;

    /**
     * Type of the transformation sequencer. Beans being singleton, Factory meaning bean instantiation at each call.
     */
    private TRANSFORMER_TYPE type = TRANSFORMER_TYPE.BEAN;

    /**
     *
     */
    private String transformerClass;

    /**
     * Entity class to ben handled by the transformer.
     */
    private String entityClass = WILDCARD;

    private Map<String, String> sequencerOptions = HashMap.newHashMap(0);

    /**
     * Columns to be handled by the transformer.
     */
    private Collection<ColumnTransformationDefinition> columnTransformationDefinitions = HashSet.newHashSet(0);

    /**
     * Get name of the sequencer to be referenced by the sequence.
     */
    public String getName() {
        return name;
    }

    /**
     * Set name of the sequencer to be referenced by the sequence.
     *
     * @param name name of the sequencer to be referenced by the sequence.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get type of the transformation sequencer. Beans being singleton, Factory meaning bean instantiation at each call.
     */
    public TRANSFORMER_TYPE getType() {
        return type;
    }

    /**
     * Set type of the transformation sequencer. Beans being singleton, Factory meaning bean instantiation at each call.
     *
     * @param type the type of the transformation sequencer. Beans being singleton, Factory meaning bean instantiation at each call.
     */
    public void setType(TRANSFORMER_TYPE type) {
        this.type = type;
    }

    /**
     * Get the transformer class.
     *
     * @return the transformer class.
     */
    public String getTransformerClass() {
        return transformerClass;
    }

    /**
     * Set the transformer class.
     *
     * @param transformerClass the transformer class.
     */
    public void setTransformerClass(String transformerClass) {
        this.transformerClass = transformerClass;
    }

    /**
     * Get the entity class simple name that will be processed by the sequencer.
     *
     * @return the entity class.
     */
    public String getEntityClass() {
        return entityClass;
    }

    /**
     * Set the entity class simple name that will be processed by the sequencer.
     *
     * @param entityClass the entity class.
     */
    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Get the columns to be handled by the transformer.
     *
     * @return the column transformation definition
     */
    public Collection<ColumnTransformationDefinition> getColumnTransformationDefinitions() {
        return columnTransformationDefinitions;
    }

    /**
     * Set the columns to be handled by the transformer.
     *
     * @param columnTransformationDefinitions the columns to be handled by the transformer.
     */
    public void setColumnTransformationDefinitions(Collection<ColumnTransformationDefinition> columnTransformationDefinitions) {
        this.columnTransformationDefinitions = columnTransformationDefinitions;
    }

    public Map<String, String> getSequencerOptions() {
        Map<String, String> ret = HashMap.newHashMap(0);
        for (Map.Entry<String, String> entry : this.sequencerOptions.entrySet()) {
            String key = entry.getKey();
            ret.put(key.replaceFirst("\\d+\\.", ""), entry.getValue());
        }
        return ret;
    }

    public void setSequencerOptions(Map<String, String> sequencerOptions) {
        this.sequencerOptions = sequencerOptions;
    }
}
