package net.osgiliath.migrator.core.api.metamodel.model;

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

public class OutboundEdge {

    private final FieldEdge fieldEdge;
    private final MetamodelVertex targetVertex;
    public OutboundEdge(FieldEdge fieldEdge, MetamodelVertex targetVertex) {
        this.fieldEdge = fieldEdge;
        this.targetVertex = targetVertex;
    }

    public MetamodelVertex getTargetVertex() {
        return targetVertex;
    }

    public FieldEdge getFieldEdge() {
        return fieldEdge;
    }

}
