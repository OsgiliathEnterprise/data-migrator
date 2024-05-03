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

import net.osgiliath.migrator.core.api.metamodel.RelationshipType;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * A vertex in the metamodel graph.
 */
public interface MetamodelVertex {


    /**
     * Type of the vertex
     */
    String getTypeName();


    /**
     * @return Additional properties to display in the model graph (Key: property name, Value: property value).
     */
    Map<String, Object> getAdditionalModelVertexProperties(Object entity);

    /**
     * Type of the relationship between the two vertices.
     *
     * @param getterMethod Method of the relaitionship to get the type from.
     * @return Type of the relationship between the two vertices.
     */
    RelationshipType relationshipType(Method getterMethod);


}
