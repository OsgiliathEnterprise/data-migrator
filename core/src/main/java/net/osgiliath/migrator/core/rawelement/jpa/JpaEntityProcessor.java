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
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_PU;

/**
 * JPA entity helper containing JPA reflection queries.
 */
@Component
public class JpaEntityProcessor implements RawElementProcessor {

    /**
     * List of many to many owning side chosen randomly (when no mappedBy instruction is set on any of both sides).
     */
    private static final Collection<Class<?>> randomManyToManyOwningSide = new ArrayList<>();

    @PersistenceContext(unitName = SOURCE_PU)
    private EntityManager entityManager;

    /**
     * Assess if the class relationship is derived (not the owner side).
     *
     * @param entityClass   the entity class.
     * @param attributeName the attribute name.
     * @return the entity class name.
     */
    public boolean isDerived(Class<?> entityClass, String attributeName) {
        try {
            Method m = entityClass.getDeclaredMethod(fieldToGetter(attributeName));
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
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("The relationship scan didn't succeed to find the getter method for the relation attribute", e);
        }
    }

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
        for (Method targetEntityClassMethod : targetEntityClass.getDeclaredMethods()) {
            for (Annotation a : targetEntityClassMethod.getDeclaredAnnotations()) {
                if (a instanceof ManyToMany mtm && mtm.mappedBy().isEmpty()) {
                    Class<?> targetEntityClassManyToManyTargetEntity = (Class<?>) ((ParameterizedType) targetEntityClassMethod.getGenericReturnType()).getActualTypeArguments()[0];
                    if (targetEntityClassManyToManyTargetEntity.equals(entityClass) && !randomManyToManyOwningSide.contains(targetEntityClass)) {
                        randomManyToManyOwningSide.add(entityClass);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Gets the primary key getter entity method.
     *
     * @param entityClass the entity class.
     * @return the primary key getter method.
     */
    public Optional<Method> getPrimaryKeyGetterMethod(Class<?> entityClass) {
        return Arrays.stream(entityClass.getDeclaredMethods()).filter(
                m -> Arrays.stream(m.getDeclaredAnnotations()).anyMatch(a -> a instanceof jakarta.persistence.Id)
        ).findAny();
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
        return getId(((JpaMetamodelVertex) metamodelVertex).entityClass(), entity);
    }

    /**
     * Gets the primary key value.
     *
     * @param entityClass the entity class.
     * @param entity      the entity.
     * @return the primary key value.
     */
    @Override
    public Optional<Object> getId(Class<?> entityClass, Object entity) {
        return getPrimaryKeyGetterMethod(entityClass).map(
                primaryKeyGetterMethod -> {
                    try {
                        return primaryKeyGetterMethod.invoke(entity);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Override
    public Method getterMethod(MetamodelVertex entityClass, Field attribute) {
        return getterMethod(((JpaMetamodelVertex) entityClass).entityClass(), attribute);
    }

    /**
     * Gets the getter method for a field.
     *
     * @param entityClass the entity class.
     * @param attribute   the attribute.
     * @return the getter method.
     */
    private Method getterMethod(Class<?> entityClass, Field attribute) {
        final String getterName = fieldToGetter(attribute.getName());
        return Arrays.stream(entityClass.getDeclaredMethods()).filter((Method m) -> m.getName().equals(getterName)).findAny().orElseThrow(() -> new RuntimeException("No getter for field " + attribute.getName() + " in class " + entityClass.getName()));
    }

    /**
     * Gets the getter method name for a field.
     *
     * @param attributeName the attribute name to get the getter name.
     * @return the getter name.
     */
    private static String fieldToGetter(String attributeName) {
        return "get" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1);
    }

    /**
     * Gets the setter method name for a field.
     *
     * @param attributeName the attribute name to get the setter name.
     * @return the setter name.
     */
    private String fieldToSetter(String attributeName) {
        return "set" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1);
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
    private static String getterToFieldName(String getterName) {
        return Character.toLowerCase(getterName.charAt(3)) + getterName.substring(4);
    }

    /**
     * Gets Relationship type of a relationship between two entities.
     *
     * @param getterMethod the getter method of the relationship.
     * @return the type of the relationship (one to one, one to many, many to many).
     */
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
            throw new RuntimeException("The getter method " + getterMethod.getName() + " is not a relationship");
        }
    }

    /**
     * Gets the Setter method for a field
     *
     * @param entityClass the entity class.
     * @param field       the field.
     * @return the setter method.
     */
    public Optional<Method> setterMethod(Class<?> entityClass, Field field) {
        final String setterName = fieldToSetter(field.getName());
        return Arrays.stream(entityClass.getDeclaredMethods()).filter((Method m) -> m.getName().equals(setterName)).findAny();
    }

    /**
     * Gets the inverse relationship field.
     *
     * @param getterMethod      the getter method that targets an entity (relationship).
     * @param targetEntityClass the target entity class.
     * @return the inverse relationship field.
     */
    public Optional<Field> inverseRelationshipField(Method getterMethod, Class<?> targetEntityClass) {
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

    @Override
    public Object getFieldValue(MetamodelVertex metamodelVertex, Object entity, String attributeName) {
        return getFieldValue(((JpaMetamodelVertex) metamodelVertex).entityClass(), entity, attributeName);
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
                        if (typeOfCollection.equals(sourceClass)) {
                            return true;
                        }
                    }
                    return false;
                }).map((Field field) -> {
                    Method getterMethodOfField = getterMethod(targetEntityClass, field);
                    RelationshipType inverseRelationshipType = relationshipType(getterMethodOfField);
                    return new AbstractMap.SimpleEntry<>(field, inverseRelationshipType);
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
        return Arrays.stream(getterMethod.getDeclaredAnnotations()).map(a -> {
            if (a instanceof ManyToMany mtm) {
                return mtm.mappedBy();
            } else if (a instanceof OneToMany otm) {
                return otm.mappedBy();
            } else if (a instanceof OneToOne oto) {
                return oto.mappedBy();
            } else {
                return null;
            }
        }).filter(mappedBy -> null != mappedBy && !mappedBy.isEmpty()).findAny();
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
            throw new RuntimeException("The relationship type " + relationshipType + " is not supported");
        }
    }

    /**
     * Gets the field value.
     *
     * @param entityClass   the entity class.
     * @param entity        the entity.
     * @param attributeName the attribute name to get value from.
     * @return the field value.
     */
    private Object getFieldValue(Class<?> entityClass, Object entity, String attributeName) {
        Optional<Field> field = attributeToField(entityClass, attributeName);
        return field.map(f -> getterMethod(entityClass, f)).map(getterMethod -> {
                    Method attachedGetterMethod = null;
                    Object entityToUse = entity;
                    try {
                        if (isDetached(entityClass, entityToUse)) {
                            Session session = entityManager.unwrap(Session.class);
                            entityToUse = session.merge(entityToUse); // reattach entity to session (otherwise lazy loading won't work)
                            entityManager.refresh(entityToUse);
                        }
                        try {
                            attachedGetterMethod = entityToUse.getClass().getMethod(getterMethod.getName(), getterMethod.getParameterTypes());
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                        Object res = attachedGetterMethod.invoke(entityToUse);
                        return res;
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("No field named " + attributeName + " in " + entityClass.getName()));
    }

    public boolean isDetached(Class entityClass, Object entity) {
        Optional<Object> idValue = getId(entityClass, entity);
        return idValue.isPresent()  // must not be transient
                && !entityManager.contains(entity)  // must not be managed now
                && entityManager.find(entityClass, idValue.get()) != null;  // must not have been removed
    }

    /**
     * Gets the field value regarding an attribute name.
     *
     * @param entityClass the entity class.
     * @return the field.
     */
    private Optional<Field> attributeToField(Class<?> entityClass, String attributeName) {
        return Arrays.stream(entityClass.getDeclaredFields()).filter(f -> f.getName().equals(attributeName)).findAny();
    }

    /**
     * Sets the field value.
     *
     * @param entityClass   the entity class.
     * @param entity        the entity to set value.
     * @param attributeName the attribute name to set value.
     * @param value         the value to set.
     */
    @Override
    public void setFieldValue(Class<?> entityClass, Object entity, String attributeName, Object value) {
        Optional<Field> field = attributeToField(entityClass, attributeName);
        field.ifPresentOrElse(f -> setFieldValue(entityClass, entity, f, value), () -> {
            throw new RuntimeException("No field with name " + attributeName + " in class " + entityClass.getSimpleName());
        });
    }

    @Override
    public void setFieldValue(MetamodelVertex metamodelVertex, Object entity, String attributeName, Object value) {
        setFieldValue(((JpaMetamodelVertex) metamodelVertex).entityClass(), entity, attributeName, value);
    }

    /**
     * Sets the field value.
     *
     * @param entityClass the entity class.
     * @param entity      the entity to set value.
     * @param field       the field to set value.
     * @param value       the value to set.
     */
    public void setFieldValue(Class<?> entityClass, Object entity, Field field, Object value) {
        setterMethod(entityClass, field).ifPresentOrElse(setterMethod -> {
            try {
                entity.getClass().getMethod(setterMethod.getName(), setterMethod.getParameterTypes()).invoke(entity, value);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            throw new RuntimeException("No setter with name " + fieldToSetter(field.getName()) + " in class " + entityClass.getSimpleName());
        });
    }
}
