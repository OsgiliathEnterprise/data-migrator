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

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;

public class MetamodelVertexAndEntityAndColumnNameAndFieldValue<FIELD_TYPE> {
    private final MetamodelVertex jpaMetamodelVertex;
    private final ModelElement entity;
    private final FieldEdge column;
    private final FIELD_TYPE fieldValue;

    public MetamodelVertexAndEntityAndColumnNameAndFieldValue(MetamodelVertex jpaMetamodelVertex, ModelElement entity, FieldEdge column, FIELD_TYPE fieldValue) {
        this.jpaMetamodelVertex = jpaMetamodelVertex;
        this.entity = entity;
        this.column = column;
        this.fieldValue = fieldValue;
    }

    public MetamodelVertex getJpaMetamodelVertex() {
        return jpaMetamodelVertex;
    }

    public ModelElement getEntity() {
        return entity;
    }

    public FieldEdge getColumn() {
        return column;
    }

    public FIELD_TYPE getFieldValue() {
        return fieldValue;
    }
}
