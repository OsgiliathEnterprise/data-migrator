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

import jakarta.persistence.metamodel.StaticMetamodel;
import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.metamodel.impl.internal.spring.AbstractClassAwareClassPathScanningCandidateComponentProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;


@Component
public class JpaMetamodelScanner implements MetamodelScanner {
    private final DataMigratorConfiguration dataAnonymizationConfiguration;

    public JpaMetamodelScanner(DataMigratorConfiguration dataAnonymizationConfiguration) {
        this.dataAnonymizationConfiguration = dataAnonymizationConfiguration;
    }

    private Collection<BeanDefinition> scanBeandefinition() {
        ClassPathScanningCandidateComponentProvider scanner = new AbstractClassAwareClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(StaticMetamodel.class));
        return scanner.findCandidateComponents(dataAnonymizationConfiguration.getModelBasePackage());
    }

    private Class<?> extractBeanClass(BeanDefinition beanDefinition) {
        try {
            return ((ScannedGenericBeanDefinition) beanDefinition)
                    .resolveBeanClass(dataAnonymizationConfiguration.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Cacheable("metamodelEntitiesClass")
    @Override
    public Collection<Class<?>> scanMetamodelClasses() {
        return this.scanBeandefinition()
                .stream()
                .map(this::extractBeanClass)
                .filter(clazz -> !isTechnicalTable(clazz.getSimpleName()))
                .collect(Collectors.toSet());
    }

    protected boolean isTechnicalTable(String simpleName) {
        return Arrays.asList("Databasechangelog_", "DatabasechangelogId_", "Databasechangeloglock_").contains(simpleName);
    }
}
