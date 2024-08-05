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
import net.osgiliath.migrator.core.api.metamodel.RelationshipType;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.exception.ErrorCallingRawElementMethodException;
import net.osgiliath.migrator.core.exception.RawElementFieldOrMethodNotFoundException;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_PU;
import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;

/**
 * JPA entity helper containing JPA reflection queries.
 */
@Component
public class JpaEntityProcessor implements RawElementProcessor {

    /**
     * List of many to many owning side chosen randomly (when no mappedBy instruction is set on any of both sides).
     */
    private static final Collection<Class<?>> randomManyToManyOwningSide = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(JpaEntityProcessor.class);

    @PersistenceContext(unitName = SOURCE_PU)
    private EntityManager entityManager;

    @Autowired
    @Qualifier(SOURCE_TRANSACTION_MANAGER)
    private PlatformTransactionManager sourcePlatformTransactionManager;

    /**
     * selects the owning side of a many to many relationship.
     *
     * @param entityClass      the entity class.
     * @param manyToManyMethod the many to many method.
     */
    private void addEntityClassAsOwningSideIfMappedByIsNotDefinedOnBothSides(Class<?> entityClass, Method manyToManyMethod) {
        if (randomManyToManyOwningSide.contains(entityClass)) {
            return;
        }
        boolean isMappedBy = Arrays.stream(manyToManyMethod.getDeclaredAnnotations())
                .filter(ManyToMany.class::isInstance)
                .anyMatch(a -> !((ManyToMany) a).mappedBy().isEmpty());
        if (isMappedBy) {
            return;
        }
        Class<?> targetEntityClass = (Class<?>) ((ParameterizedType) manyToManyMethod.getGenericReturnType()).getActualTypeArguments()[0];
        try {
            log.debug("finding inverse many to many method: {} for class {}", manyToManyMethod.getName(), targetEntityClass.getSimpleName());
            Arrays.stream(Introspector.getBeanInfo(targetEntityClass)
                            .getPropertyDescriptors()).map(PropertyDescriptor::getReadMethod)
                    .forEach(targetEntityClassMethod -> {
                        for (Annotation a : targetEntityClassMethod.getDeclaredAnnotations()) {
                            if (a instanceof ManyToMany mtm && mtm.mappedBy().isEmpty()) {
                                Class<?> targetEntityClassManyToManyTargetEntity = (Class<?>) ((ParameterizedType) targetEntityClassMethod.getGenericReturnType()).getActualTypeArguments()[0];
                                if (targetEntityClassManyToManyTargetEntity.equals(entityClass) && !randomManyToManyOwningSide.contains(targetEntityClass)) {
                                    randomManyToManyOwningSide.add(entityClass);
                                    break;
                                }
                            }
                        }
                    });
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
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
    public Optional<Object> getId(ModelElement element) {
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
    public Optional<Object> getId(MetamodelVertex metamodelVertex, Object entity) {
        return getRawId(((JpaMetamodelVertex) metamodelVertex).entityClass(), entity);
    }


    private Optional<Object> getRawId(Class entityClass, Object entity) {
        // Cannot use getRawFieldValue due to cycle and the @Transactional aspect
        return getPrimaryKeyGetterMethod(entityClass).map(
                primaryKeyGetterMethod -> {
                    try {
                        return primaryKeyGetterMethod.invoke(entity);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new ErrorCallingRawElementMethodException(e);
                    }
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
    private Optional<Method> getterMethod(Class<?> entityClass, Field attribute) {
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

    /**
     * Gets Relationship type of a relationship between two entities.
     *
     * @param getterMethod the getter method of the relationship.
     * @return the type of the relationship (one to one, one to many, many to many).
     */
    @Override
    public RelationshipType relationshipType(Method getterMethod) {
        if (getterMethod.isAnnotationPresent(OneToMany.class)) {
            return RelationshipType.ONE_TO_MANY;
        } else if (getterMethod.isAnnotationPresent(ManyToMany.class)) {
            return RelationshipType.MANY_TO_MANY;
        } else if (getterMethod.isAnnotationPresent(OneToOne.class)) {
            return RelationshipType.ONE_TO_ONE;
        } else if (getterMethod.isAnnotationPresent(ManyToOne.class)) {
            return RelationshipType.MANY_TO_ONE;
        } else {
            throw new RawElementFieldOrMethodNotFoundException("The getter method " + getterMethod.getName() + " is not a relationship");
        }
    }

    /**
     * Gets the inverse relationship field.
     *
     * @param getterMethod      the getter method that targets an entity (relationship).
     * @param targetEntityClass the target entity class.
     * @return the inverse relationship field.
     */
    private Optional<Field> inverseRelationshipField(Method getterMethod, Class<?> targetEntityClass) {
        RelationshipType relationshipType = relationshipType(getterMethod);
        Optional<String> mappedBy = getMappedByValue(getterMethod);
        Optional<Field> mappedByField = mappedBy.flatMap(mappedByValue -> Arrays.stream(targetEntityClass.getDeclaredFields()).filter(f -> f.getName().equals(mappedByValue)).findAny());
        if (mappedByField.isPresent()) {
            return mappedByField;
        } else {
            return findInverseRelationshipFieldWithoutMappedByInformation(targetEntityClass, getterMethod, relationshipType);
        }
    }

    @Override
    public Optional<Field> inverseRelationshipField(Method getterMethod, MetamodelVertex targetEntityClass) {
        return inverseRelationshipField(getterMethod, ((JpaMetamodelVertex) targetEntityClass).entityClass());
    }

    @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    @Override
    public Object getFieldValue(ModelElement modelElement, String attributeName) {
        Object entity = modelElement.rawElement();
        return getRawElementFieldValue(entity, attributeName);
    }

    private Object getRawElementFieldValue(Object entity, String attributeName) {
        try {
            if (null != entityManager && isDetached(entity.getClass(), entity)) {
                Session session = entityManager.unwrap(Session.class);
                entity = session.merge(entity); // reattach entity to session (otherwise lazy loading won't work)
                entityManager.refresh(entity);
            }
            Object entityToUse = entity;
            return Arrays.stream(Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors()).filter(
                            pd -> pd.getName().equals(attributeName)
                    ).map(PropertyDescriptor::getReadMethod).map(getterMethod -> {
                        try {
                            Object result = getterMethod.invoke(entityToUse);
                            if (null != entityManager && null != result && result instanceof Collection results) {
                                PersistenceUnitUtil unitUtil =
                                        entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
                                if (!unitUtil.isLoaded(entityToUse, attributeName)) {
                                    TransactionTemplate transactionTemplate = new TransactionTemplate(sourcePlatformTransactionManager);
                                    transactionTemplate.setReadOnly(true);
                                    transactionTemplate.execute(status -> results.iterator().hasNext());
                                }
                            }
                            return result;
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new ErrorCallingRawElementMethodException(e);
                        }
                    }).findAny()
                    .orElseThrow(() -> new RuntimeException("No field named " + attributeName + " in " + entityToUse.getClass().getName()));
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds the inverse relationship field without mappedBy information.
     *
     * @param targetEntityClass the target entity class.
     * @param getterMethod      the getter method that targets the target entity.
     * @param relationshipType  the relationship type.
     * @return the inverse relationship field.
     */
    private Optional<Field> findInverseRelationshipFieldWithoutMappedByInformation(Class<?> targetEntityClass, Method getterMethod, RelationshipType relationshipType) {
        Class<?> sourceClass = getterMethod.getDeclaringClass();
        return Arrays.stream(targetEntityClass.getDeclaredFields())
                .filter((Field field) -> {
                    if (field.getGenericType().equals(sourceClass)) {
                        return true;
                    } else if (Collection.class.isAssignableFrom(field.getType())) {
                        Class<?> typeOfCollection = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                        return typeOfCollection.equals(sourceClass);
                    }
                    return false;
                }).map((Field field) -> {
                    Optional<Method> getterMethodOfFieldOpt = getterMethod(targetEntityClass, field);
                    Optional<RelationshipType> inverseRelationshipTypeOpt = getterMethodOfFieldOpt.map(getterMethodOfField -> relationshipType(getterMethodOfField));
                    return inverseRelationshipTypeOpt.map(inverseRelationshipType -> new AbstractMap.SimpleEntry<>(field, inverseRelationshipType)).orElseThrow();
                }).filter(entry -> isInverseRelationshipType(relationshipType, entry.getValue()))
                .map(Map.Entry::getKey).findAny();
    }

    /**
     * Gets the mappedBy value.
     *
     * @param getterMethod the getter method to get the information from (the annotation value).
     * @return the mappedBy value.
     */
    private static Optional<String> getMappedByValue(Method getterMethod) {
        return Arrays.stream(getterMethod.getDeclaredAnnotations()).map(a ->
                switch (a) {
                    case ManyToMany mtm -> mtm.mappedBy();
                    case OneToMany otm -> otm.mappedBy();
                    case OneToOne oto -> oto.mappedBy();
                    default -> null;
                }
        ).filter(mappedBy -> null != mappedBy && !mappedBy.isEmpty()).findAny();
    }

    /**
     * Checks if the inverse relationship type is the inverse type of a the relationship type.
     *
     * @param relationshipType        the relationship type.
     *                                (one to one, one to many, many to many, many to one).
     * @param inverseRelationshipType the inverse relationship type.
     * @return the inverse relationship field.
     */
    private boolean isInverseRelationshipType(RelationshipType relationshipType, RelationshipType inverseRelationshipType) {
        if (relationshipType.equals(RelationshipType.MANY_TO_MANY)) {
            return inverseRelationshipType.equals(RelationshipType.MANY_TO_MANY);
        } else if (relationshipType.equals(RelationshipType.ONE_TO_MANY)) {
            return inverseRelationshipType.equals(RelationshipType.MANY_TO_ONE);
        } else if (relationshipType.equals(RelationshipType.MANY_TO_ONE)) {
            return inverseRelationshipType.equals(RelationshipType.ONE_TO_MANY);
        } else if (relationshipType.equals(RelationshipType.ONE_TO_ONE)) {
            return inverseRelationshipType.equals(RelationshipType.ONE_TO_ONE);
        } else {
            throw new RawElementFieldOrMethodNotFoundException("The relationship type " + relationshipType + " is not supported");
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
        Optional<Object> idValue = getRawId(entityClass, entity);
        return idValue.isPresent()  // must not be transient
                && !entityManager.contains(entity)  // must not be managed now
                && entityManager.find(entityClass, idValue.orElseThrow()) != null;  // must not have been removed
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

    /**
     * Assess if the class relationship is derived (not the owner side).
     *
     * @param entityClass   the entity class.
     * @param attributeName the attribute name.
     * @return the entity class name.
     */
    public boolean isDerived(Class<?> entityClass, String attributeName) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(entityClass).getPropertyDescriptors())
                    .filter(pd -> attributeName.equals(pd.getName()))
                    .map(PropertyDescriptor::getReadMethod)
                    .anyMatch(
                            m -> {
                                for (Annotation a : m.getDeclaredAnnotations()) {
                                    if (a instanceof OneToMany otm) {
                                        return !otm.mappedBy().isEmpty();
                                    } else if (a instanceof ManyToMany mtm) {
                                        addEntityClassAsOwningSideIfMappedByIsNotDefinedOnBothSides(entityClass, m);
                                        return !mtm.mappedBy().isEmpty() || randomManyToManyOwningSide.contains(((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0]);
                                    } else if (a instanceof OneToOne oto) {
                                        return !oto.mappedBy().isEmpty();
                                    }
                                }
                                return false;
                            }
                    );
        } catch (IntrospectionException e) {
            throw new RawElementFieldOrMethodNotFoundException("The relationship scan didn't succeed to find the getter method for the relation attribute", e);
        }
    }

    /**
     * Assess if the class relationship should ignore foreign key.
     *
     * @param entityClass   the entity class.
     * @param attributeName the attribute name.
     * @return the entity class name.
     */

    public boolean isFkIgnored(Class<?> entityClass, String attributeName) {
        try {

            return Arrays.stream(Introspector.getBeanInfo(entityClass).getPropertyDescriptors())
                    .filter(pd -> attributeName.equals(pd.getName()))
                    .map(PropertyDescriptor::getReadMethod)
                    .anyMatch(
                            m -> {
                                for (Annotation a : m.getDeclaredAnnotations()) {
                                    if (a instanceof JoinTable otm) {
                                        return joinTableIgnoresFk(otm);
                                    } else if (a instanceof JoinColumns mtm) {
                                        return joinColumnsIgnoresFk(mtm);
                                    } else if (a instanceof JoinColumn oto) {
                                        return joinColumnIgnoresFk(oto);
                                    } else if (a instanceof PrimaryKeyJoinColumn oto) {
                                        return joinColumnIgnoresFk(oto);
                                    }

                                }
                                return false;
                            }
                    );
        } catch (IntrospectionException e) {
            throw new RawElementFieldOrMethodNotFoundException("The relationship scan didn't succeed to find the getter method for the relation attribute", e);
        }
    }

    public static boolean joinTableIgnoresFk(JoinTable a1) { // TODO refine joincol, is a pretty naive impl
        boolean ignores = a1.foreignKey().value().equals(ConstraintMode.NO_CONSTRAINT);
        ignores = ignores || a1.inverseForeignKey().value().equals(ConstraintMode.NO_CONSTRAINT);
        for (var joinCol : a1.joinColumns()) {
            ignores = ignores || joinColumnIgnoresFk(joinCol);
        }
        return ignores;
    }

    public static boolean joinColumnsIgnoresFk(JoinColumns a1) { // TODO refine joincol, is a pretty naive impl
        boolean ignores = a1.foreignKey().value().equals(ConstraintMode.NO_CONSTRAINT);
        for (var joinCol : a1.value()) {
            ignores = ignores || joinColumnIgnoresFk(joinCol);
        }
        return a1.foreignKey().value().equals(ConstraintMode.NO_CONSTRAINT);
    }

    public static boolean joinColumnIgnoresFk(JoinColumn a1) {
        return a1.foreignKey().value().equals(ConstraintMode.NO_CONSTRAINT);
    }

    public static boolean joinColumnIgnoresFk(PrimaryKeyJoinColumn a1) {
        return a1.foreignKey().value().equals(ConstraintMode.NO_CONSTRAINT);
    }

}
