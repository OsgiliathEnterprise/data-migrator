package net.osgiliath.migrator.core.metamodel.impl.internal.spring;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

public class AbstractClassAwareClassPathScanningCandidateComponentProvider extends ClassPathScanningCandidateComponentProvider {
    public AbstractClassAwareClassPathScanningCandidateComponentProvider(boolean useDefaultFilters) {
        super(useDefaultFilters);
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
		AnnotationMetadata metadata = beanDefinition.getMetadata();
        return super.isCandidateComponent(beanDefinition) || metadata.isAbstract();
	}
}
