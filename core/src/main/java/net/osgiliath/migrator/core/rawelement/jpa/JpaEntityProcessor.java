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

import jakarta.persistence.*;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.exception.ErrorCallingRawElementMethodException;
import net.osgiliath.migrator.core.exception.RawElementFieldOrMethodNotFoundException;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;

/**
 * JPA entity helper containing JPA reflection queries.
 */
@Component
public class JpaEntityProcessor implements RawElementProcessor {

    private static final Logger log = LoggerFactory.getLogger(JpaEntityProcessor.class);
    private final PlatformTransactionManager sourcePlatformTxManager;

    public JpaEntityProcessor(@Qualifier(SOURCE_TRANSACTION_MANAGER) PlatformTransactionManager sourcePlatformTxManager) {
        this.sourcePlatformTxManager = sourcePlatformTxManager;
    }


    /**
     * Gets the primary key getter entity method.
     *
     * @param entityClass the entity class.
     * @return the primary key getter method.
     */
    public Optional<Method> getPrimaryKeyGetterMethod(Class<?> entityClass) {
        log.debug("Finding getId method for class: {}", entityClass.getSimpleName());
        try {
            return Arrays.stream(Introspector.getBeanInfo(entityClass).getPropertyDescriptors()).map(
                    PropertyDescriptor::getReadMethod
            ).filter(
                    m -> m.getAnnotation(Id.class) != null || m.getAnnotation(EmbeddedId.class) != null
            ).findAny();
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object unproxy(Object o) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(sourcePlatformTxManager);
        transactionTemplate.setReadOnly(true);
        return transactionTemplate.execute(status -> {
            Object ret = Hibernate.unproxy(o);
            EntityManagerFactory emf = ((JpaTransactionManager) sourcePlatformTxManager).getEntityManagerFactory();
            EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
            em.detach(ret);
            return ret;
        });
    }

    @Override
    public String fieldNameOfGetter(Method getterMethod) {
        return Character.toUpperCase(getterMethod.getName().charAt(3)) + getterMethod.getName().substring(3);
    }

    @Override
    // @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    public Optional<Object> getId(ModelElement element) {
        // return getRawId(((JpaMetamodelVertex) element.vertex()).entityClass(), element.rawElement()); // Not calling getId(MV, E) to avoid nested transaction proxy
        return getId(element.vertex(), element.rawElement());
    }

    /**
     * Gets the primary key value.
     *
     * @param metamodelVertex the metamodel vertex.
     * @param entity          the entity.
     * @return the primary key value.
     */
    @Override
    // @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    public Optional<Object> getId(MetamodelVertex metamodelVertex, Object entity) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(sourcePlatformTxManager);
        transactionTemplate.setReadOnly(true);
        return transactionTemplate.execute(status -> internalGetRawId(null != metamodelVertex ? ((JpaMetamodelVertex) metamodelVertex).entityClass() : entity.getClass(), entity));
    }

    private Optional<Object> internalGetRawId(Class entityClass, Object entity) {
        // Cannot use getRawFieldValue due to cycle and the @Transactional aspect
        log.debug("getting Id of entity with class {}, entity {}", entityClass, entity);
        return getPrimaryKeyGetterMethod(entityClass).map(
                primaryKeyGetterMethod -> {
                    try {
                        if (null != entity) {
                            return primaryKeyGetterMethod.invoke(entity);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new ErrorCallingRawElementMethodException(e);
                    }
                    return Optional.empty();
                }
        );
    }

    @Override
    public Optional<Method> getterMethod(MetamodelVertex entityClass, Field attribute) {
        return getterMethod(((JpaMetamodelVertex) entityClass).entityClass(), attribute);
    }

    /**
     * Gets the getter method for a field.
     *
     * @param entityClass the entity class.
     * @param attribute   the attribute.
     * @return the getter method.
     */
    @Override
    public Optional<Method> getterMethod(Class<?> entityClass, Field attribute) {
        try {
            return getPropertyDescriptor(entityClass, attribute).map(PropertyDescriptor::getReadMethod);
        } catch (Exception e) {
            throw new RuntimeException("No getter for field " + attribute.getName() + " in class " + entityClass.getName());
        }
    }

    private static Optional<PropertyDescriptor> getPropertyDescriptor(Class<?> entityClass, Field attribute) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(entityClass).getPropertyDescriptors()).filter(pd -> attribute.getName().equals(pd.getName())).findAny();
    }

    /**
     * Gets the primary key field name.
     *
     * @param entityClass the entity class.
     * @return the primary key field name.
     */
    public Optional<String> getPrimaryKeyFieldName(Class<?> entityClass) {
        return getPrimaryKeyGetterMethod(entityClass).map(primaryKeyGetter -> getterToFieldName(primaryKeyGetter.getName()));
    }

    /**
     * Gets the field name from a getter method name.
     *
     * @param getterName the getter name.
     * @return the field name.
     */
    public static String getterToFieldName(String getterName) {
        return Character.toLowerCase(getterName.charAt(3)) + getterName.substring(4);
    }

    // @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    @Override
    public Object getFieldValue(ModelElement modelElement, String attributeName) {
        Object entity = modelElement.rawElement();
        return getRawElementFieldValue(entity, attributeName);
    }

    private Object getRawElementFieldValue(Object entity, String attributeName) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(sourcePlatformTxManager);
        transactionTemplate.setReadOnly(true);
        return transactionTemplate.execute(status -> internalGetRawElementValue(entity, attributeName));
    }

    private Object internalGetRawElementValue(Object entity, String attributeName) {
        JpaTransactionManager tm = (JpaTransactionManager) sourcePlatformTxManager;
        EntityManagerFactory emf = tm.getEntityManagerFactory();
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
        try {
            if (null != em && isDetached(entity.getClass(), entity)) {
                entity = em.merge(entity);
                em.refresh(entity);
            }
            Object entityToUse = entity;
            return Arrays.stream(Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors()).filter(
                            pd -> pd.getName().equals(attributeName)
                    ).map(PropertyDescriptor::getReadMethod).map(getterMethod -> {
                        try {
                            Object result = getterMethod.invoke(entityToUse);
                            if (null != em && null != result && result instanceof Collection results) {
                                PersistenceUnitUtil unitUtil =
                                        emf.getPersistenceUnitUtil();
                                if (!unitUtil.isLoaded(entityToUse, attributeName)) { // TODO performance issue here, hack due to nofk
                                    TransactionTemplate transactionTemplate = new TransactionTemplate(sourcePlatformTxManager);
                                    transactionTemplate.setReadOnly(true);
                                    transactionTemplate.execute(status -> results.iterator().hasNext());
                                }
                            }
                            return result;
                        } catch (Exception e) {
                            log.error("error while getting value for entity");
                            throw new ErrorCallingRawElementMethodException(e);
                        }
                    }).findAny()
                    .orElseThrow(() -> new RuntimeException("No field named " + attributeName + " in " + entityToUse.getClass().getName()));
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the field value.
     *
     * @param entityClass the Class of the entity
     * @param entity      the entity.
     * @return the field value.
     */
    private boolean isDetached(Class entityClass, Object entity) {
        JpaTransactionManager tm = (JpaTransactionManager) sourcePlatformTxManager;
        EntityManagerFactory emf = tm.getEntityManagerFactory();
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);

        Optional<Object> idValue = internalGetRawId(entityClass, entity);
        return idValue.map(id -> {
            return !em.contains(entity)  // must not be managed now
                    && em.find(entityClass, id) != null; // must not have been removed
        }).orElse(false);
    }

    @Override
    public void setFieldValue(ModelElement modelElement, String attributeName, Object value) {
        setFieldValue(modelElement.rawElement(), attributeName, value);
    }

    private void setFieldValue(Object entity, String attributeName, Object value) {
        try {
            Arrays.stream(Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors())
                    .filter(pd -> pd.getName().equals(attributeName))
                    .map(PropertyDescriptor::getWriteMethod)
                    .findAny().ifPresentOrElse(
                            m -> {
                                try {
                                    m.invoke(entity, value);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                } catch (InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                            }, () -> {
                                throw new RawElementFieldOrMethodNotFoundException("No setter for field name " + attributeName + " in class " + entity.getClass().getSimpleName());
                            }
                    );
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
}
