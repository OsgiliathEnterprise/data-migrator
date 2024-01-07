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
import java.util.HashSet;

/**
 * Should be extended by all data transformer
 */
public class AbstractTransformationConfigurationDefinition {
    private String name;

    private TRANSFORMER_TYPE type = TRANSFORMER_TYPE.BEAN;

    private String transformerClass;

    private String entityClass = "*";

    private Collection<String> columns = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TRANSFORMER_TYPE getType() {
        return type;
    }

    public void setType(TRANSFORMER_TYPE type) {
        this.type = type;
    }

    public String getTransformerClass() {
        return transformerClass;
    }

    public void setTransformerClass(String transformerClass) {
        this.transformerClass = transformerClass;
    }

    public String getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    public Collection<String> getColumns() {
        return columns;
    }

    public void setColumns(Collection<String> columns) {
        this.columns = columns;
    }
}
