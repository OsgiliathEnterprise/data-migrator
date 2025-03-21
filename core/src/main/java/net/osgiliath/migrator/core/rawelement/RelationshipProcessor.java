package net.osgiliath.migrator.core.rawelement;

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
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public interface RelationshipProcessor {
    Object getEdgeRawValue(ModelElement modelElement, Method getterMethod);

    ModelElement resetElementRelationships(ModelElement o);

    void resetElementRelationships(ModelElement o, Field f);

    RelationshipType relationshipType(Method getterMethod);

    Optional<? extends Field> inverseRelationshipField(Method getterMethod, MetamodelVertex target);

    void setEdgeRawValue(String fieldName, Field metamodelField, MetamodelVertex target, ModelElement modelElement, Object value);
}
