package net.osgiliath.migrator.core.api.transformers.internal.model;

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

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;

public class MetamodelVertexAndEntityAndColumnNameAndFieldValue<ENTITY_TYPE, FIELD_TYPE> {
    private final MetamodelVertex jpaMetamodelVertex;
    private final ENTITY_TYPE entity;
    private final String columnName;
    private final FIELD_TYPE fieldValue;

    public MetamodelVertexAndEntityAndColumnNameAndFieldValue(MetamodelVertex jpaMetamodelVertex, ENTITY_TYPE entity, String columnName, FIELD_TYPE fieldValue) {
        this.jpaMetamodelVertex = jpaMetamodelVertex;
        this.entity = entity;
        this.columnName = columnName;
        this.fieldValue = fieldValue;
    }

    public MetamodelVertex getJpaMetamodelVertex() {
        return jpaMetamodelVertex;
    }

    public ENTITY_TYPE getEntity() {
        return entity;
    }

    public String getColumnName() {
        return columnName;
    }

    public FIELD_TYPE getFieldValue() {
        return fieldValue;
    }
}
