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

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;

/**
 * JPA implementation of a metamodel vertex.
 *
 * @param metamodelClass The JPA metamodel class.
 * @param entityClass    The entity class.
 */

public record JpaMetamodelVertex(Class<?> metamodelClass, Class<?> entityClass) implements MetamodelVertex {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeName() {
        return entityClass().getSimpleName();
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
