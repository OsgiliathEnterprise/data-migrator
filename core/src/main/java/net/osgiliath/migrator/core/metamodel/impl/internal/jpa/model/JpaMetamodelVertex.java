package net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model;

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
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of a metamodel vertex.
 */
public class JpaMetamodelVertex implements MetamodelVertex {

    /**
     * The JPA metamodel class.
     */
    private final Class<?> metamodelClass;

    /**
     * The entity class.
     */
    private final Class<?> entityClass;
    /**
     * JPA entity helper.
     */
    private final RawElementProcessor rawElementProcessor;

    /**
     * Constructor.
     *
     * @param metamodelClass      The JPA metamodel class.
     * @param entityClass         The entity class.
     * @param rawElementProcessor JPA entity helper.
     */
    public JpaMetamodelVertex(Class<?> metamodelClass, Class<?> entityClass, RawElementProcessor rawElementProcessor) {
        this.metamodelClass = metamodelClass;
        this.entityClass = entityClass;
        this.rawElementProcessor = rawElementProcessor;
    }

    /**
     * Get the entity class.
     *
     * @return The entity class.
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * Get the metamodel class.
     *
     * @return The metamodel class.
     */
    public Class<?> getMetamodelClass() {
        return metamodelClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getAdditionalModelVertexProperties(Object entity) {
        return new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RelationshipType relationshipType(Method getterMethod) {
        return rawElementProcessor.relationshipType(getterMethod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeName() {
        return getEntityClass().getSimpleName();
    }

    /**
     * Get the Java Type of the target of a field.
     */
    public Optional<Type> targetTypeOfMetamodelField(Field f) {
        Type t = f.getGenericType();
        if (t instanceof ParameterizedType pt) {
            Type[] types = pt.getActualTypeArguments();
            if (types.length == 2) {
                return Optional.of(types[1]);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ClassVertex{" +
                "metamodelClass=" + metamodelClass.getName() +
                ", entityClass=" + entityClass.getName() +
                '}';
    }

}
