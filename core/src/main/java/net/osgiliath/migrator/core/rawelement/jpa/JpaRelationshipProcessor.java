package net.osgiliath.migrator.core.rawelement.jpa;

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
import net.osgiliath.migrator.core.exception.ErrorCallingRawElementMethodException;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

@Component
public class JpaRelationshipProcessor {

    private final MetamodelRequester metamodelRequester;

    public JpaRelationshipProcessor(MetamodelRequester metamodelRequester) {
        this.metamodelRequester = metamodelRequester;
    }

    /**
     * Returns the Raw value(s) corresponding to the entity referenced by the outboundEdge
     *
     * @param fieldEdge the edge to get the target vertices from
     * @return Returns the ModelElement(s) corresponding to the entity referenced by the outboundEdge
     */
    // @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    public Object getEdgeRawValue(FieldEdge<MetamodelVertex> fieldEdge, ModelElement modelElement) {
        Optional<Method> getterMethodOpt = metamodelRequester.relationshipGetter(fieldEdge);
        return getterMethodOpt.map(getterMethod -> {
            try {
                return getterMethod.invoke(modelElement.rawElement());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ErrorCallingRawElementMethodException(e);
            }
        }).orElse(null);
    }
}
