package net.osgiliath.migrator.core.metamodel.impl.internal.spring;

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

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Spring classpath scanner that also scans abstract classes.
 */
public class AbstractClassAwareClassPathScanningCandidateComponentProvider extends ClassPathScanningCandidateComponentProvider {

    /**
     * Constructor.
     * @param useDefaultFilters use default spring component filters
     */
    public AbstractClassAwareClassPathScanningCandidateComponentProvider(boolean useDefaultFilters) {
        super(useDefaultFilters);
    }

    /**
     * {@inheritDoc}
     * @param beanDefinition the bean definition to check
     * @return annotated bean definition and abstract components classes (useful for JPA metamodel)
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
		AnnotationMetadata metadata = beanDefinition.getMetadata();
        return super.isCandidateComponent(beanDefinition) || metadata.isAbstract();
	}
}
