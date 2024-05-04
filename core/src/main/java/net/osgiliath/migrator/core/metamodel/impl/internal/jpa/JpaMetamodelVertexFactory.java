package net.osgiliath.migrator.core.metamodel.impl.internal.jpa;

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.osgiliath.migrator.core.api.metamodel.MetamodelVertexFactory;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.jpa.JpaEntityProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class JpaMetamodelVertexFactory implements MetamodelVertexFactory<JpaMetamodelVertex> {
    private static final Logger log = LoggerFactory.getLogger(JpaMetamodelVertexFactory.class);
    private final JpaEntityProcessor hibernateEntityHelper;

    @PersistenceContext(unitName = "source")
    private EntityManager entityManager;

    public JpaMetamodelVertexFactory(JpaEntityProcessor hibernateEntityHelper) {
        this.hibernateEntityHelper = hibernateEntityHelper;
    }

    public JpaMetamodelVertex createMetamodelVertex(Class<?> metamodelClass) {
        log.info("Creating a new metamodel vertex for metamodel class {}", metamodelClass.getName());
        return metamodelClassToEntityVertexAdapter(metamodelClass).orElseThrow(() -> new IllegalArgumentException("Cannot create a metamodel vertex for metamodel class " + metamodelClass.getName()));
    }

    public OutboundEdge<JpaMetamodelVertex> createOutboundEdge(FieldEdge<JpaMetamodelVertex> fieldEdge, JpaMetamodelVertex targetMetamodelVertex) {
        log.info("Creating a new field edge {} with target metamodel class {}", fieldEdge.getFieldName(), targetMetamodelVertex.getTypeName());
        return new OutboundEdge(fieldEdge, targetMetamodelVertex);
    }

    public FieldEdge<JpaMetamodelVertex> createFieldEdge(Field field) {
        log.info("Creating a new field edge {}", field.getName());
        return new FieldEdge(field);
    }

    private Optional<JpaMetamodelVertex> metamodelClassToEntityVertexAdapter(final Class<?> metamodelClass) {
        return Stream.of(metamodelClass.getDeclaredFields())
                .filter((Field f) -> "class_".equals(f.getName()))
                .map(Field::getGenericType)
                .filter(ParameterizedType.class::isInstance)
                .map(t3 -> ((ParameterizedType) t3).getActualTypeArguments()[0])
                .filter(Class.class::isInstance)
                .map(t5 -> (Class<?>) t5)
                .map(c -> {
                    try {
                        return internalCreateMetamodelVertex(metamodelClass, entityManager.getClass().getClassLoader().loadClass(c.getName()));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .findAny();
    }

    private JpaMetamodelVertex internalCreateMetamodelVertex(Class<?> metamodelClass, Class<?> entityClass) {
        return new JpaMetamodelVertex(metamodelClass, entityClass);
    }
}
