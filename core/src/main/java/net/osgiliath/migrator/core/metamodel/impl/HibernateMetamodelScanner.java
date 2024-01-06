package net.osgiliath.migrator.core.metamodel.impl;

import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.metamodel.impl.internal.spring.AbstractClassAwareClassPathScanningCandidateComponentProvider;
import jakarta.persistence.metamodel.StaticMetamodel;
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
                .resolveBeanClass(this.getClass().getClassLoader());
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
