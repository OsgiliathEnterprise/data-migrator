package net.osgiliath.migrator.core.graph;

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
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

@Service
public class ModelElementProcessor {

    private final RawElementProcessor rawElementHelper;

    public ModelElementProcessor(RawElementProcessor rawElementHelper) {

        this.rawElementHelper = rawElementHelper;
    }

    public void removeEdgeValueFromModelElementRelationShip(ModelElement sourceModelElement, FieldEdge fieldEdge, ModelElement targetModelElement) {
        Object targetValue = sourceModelElement.getEdgeRawValue(fieldEdge);
        if (targetValue instanceof Collection) {
            Collection targetValues = (Collection) targetValue;
            targetValues.remove(targetModelElement.getRawElement());
            sourceModelElement.setEdgeRawValue(fieldEdge.getSource(), fieldEdge, targetValue);
        } else {
            sourceModelElement.setEdgeRawValue(fieldEdge.getSource(), fieldEdge, null);
        }
        Method getterMethod = fieldEdge.relationshipGetter();
        Optional<Field> inverseFieldOpt = rawElementHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex) fieldEdge.getTarget()).getEntityClass());
        inverseFieldOpt.ifPresent(
                inverseField -> {
                    Object inverseValue = rawElementHelper.getFieldValue(((JpaMetamodelVertex) fieldEdge.getTarget()).getEntityClass(), targetModelElement.getRawElement(), inverseField.getName());
                    if (inverseValue instanceof Collection) {
                        Collection inverseValues = (Collection) inverseValue;
                        inverseValues.remove(sourceModelElement.getRawElement());
                        rawElementHelper.setFieldValue(((JpaMetamodelVertex) fieldEdge.getTarget()).getEntityClass(), targetModelElement.getRawElement(), inverseField.getName(), inverseValues);
                    } else {
                        rawElementHelper.setFieldValue(((JpaMetamodelVertex) fieldEdge.getTarget()).getEntityClass(), targetModelElement.getRawElement(), inverseField.getName(), null);
                    }
                }
        );

    }
}
