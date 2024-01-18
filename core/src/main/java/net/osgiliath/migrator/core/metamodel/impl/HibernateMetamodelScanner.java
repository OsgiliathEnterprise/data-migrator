package net.osgiliath.migrator.core.metamodel.impl;

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
import jakarta.persistence.metamodel.StaticMetamodel;
import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.metamodel.impl.internal.spring.AbstractClassAwareClassPathScanningCandidateComponentProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;


@Component
public class HibernateMetamodelScanner implements MetamodelScanner {
    private final DataMigratorConfiguration dataAnonymizationConfiguration;

    @PersistenceContext(unitName = "source")
    private EntityManager entityManager;

    public HibernateMetamodelScanner(DataMigratorConfiguration dataAnonymizationConfiguration) {
        this.dataAnonymizationConfiguration = dataAnonymizationConfiguration;
    }

    public Collection<BeanDefinition> scanBeandefinition() {
        ClassPathScanningCandidateComponentProvider scanner = new AbstractClassAwareClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(StaticMetamodel.class));
        return scanner.findCandidateComponents(dataAnonymizationConfiguration.getModelBasePackage());
    }

    private Class<?> extractBeanClass(BeanDefinition beanDefinition) {
        try {
            return ((ScannedGenericBeanDefinition) beanDefinition)
                    .resolveBeanClass(entityManager.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public Collection<Class<?>> scanMetamodelClasses() {
        return this.scanBeandefinition()
                .stream()
                .map((def) -> extractBeanClass(def))
                .filter((clazz) -> !isLiquibaseTable(clazz.getSimpleName()))
                .collect(Collectors.toSet());
    }

    private boolean isLiquibaseTable(String simpleName) {
        return Arrays.asList("Databasechangelog_", "DatabasechangelogId_", "Databasechangeloglock_").contains(simpleName);
    }
}
