package net.osgiliath.migrator.core.metamodel.helper;

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

import net.osgiliath.migrator.core.api.metamodel.RelationshipType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;

@Component
public class JpaEntityHelper {

    private static Collection<Class<?>> randomManyToManyOwningSide = new ArrayList<>();

    public boolean isDerived(Class<?> entityClass, String attributeName) {
        try {
            Method m = entityClass.getDeclaredMethod(fieldToGetter(attributeName));
            for (Annotation a: m.getDeclaredAnnotations()) {
                if (a instanceof OneToMany) {
                    return !((OneToMany) a).mappedBy().isEmpty();
                } else if (a instanceof ManyToMany) {
                    addEntityClassAsOwningSideIfMappedByIsNotDefinedOnBothSides(entityClass, m);
                    return !((ManyToMany) a).mappedBy().isEmpty() || randomManyToManyOwningSide.contains(((ParameterizedType)m.getGenericReturnType()).getActualTypeArguments()[0]);
                } else if (a instanceof OneToOne) {
                    return !((OneToOne) a).mappedBy().isEmpty();
                }
            }
            return false;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("The relationship scan didn't succeed to find the getter method for the relation attribute", e);
        }
    }

    private void addEntityClassAsOwningSideIfMappedByIsNotDefinedOnBothSides(Class<?> entityClass, Method manyToManyMethod) {
        if (randomManyToManyOwningSide.contains(entityClass)) {
            return;
        }
        boolean isMappedBy = Arrays.stream(manyToManyMethod.getDeclaredAnnotations())
            .filter(a -> a instanceof ManyToMany)
            .anyMatch(a -> !((ManyToMany) a).mappedBy().isEmpty());
        if (isMappedBy) {
            return;
        }
        Class<?> targetEntityClass = (Class<?>) ((ParameterizedType)manyToManyMethod.getGenericReturnType()).getActualTypeArguments()[0];
        for (Method targetEntityClassMethod: targetEntityClass.getDeclaredMethods()) {
            for (Annotation a : targetEntityClassMethod.getDeclaredAnnotations()) {
                if (a instanceof ManyToMany && ((ManyToMany) a).mappedBy().isEmpty()) {
                    Class<?> targetEntityClassManyToManyTargetEntity = (Class<?>) ((ParameterizedType)targetEntityClassMethod.getGenericReturnType()).getActualTypeArguments()[0];
                    if (targetEntityClassManyToManyTargetEntity.equals(entityClass) && !randomManyToManyOwningSide.contains(targetEntityClass)) {
                        randomManyToManyOwningSide.add(entityClass);
                        break;
                    }
                }
            }
        }
    }

    public Method getPrimaryKeyGetterMethod(Class<?> entityClass) {
        return Arrays.stream(entityClass.getDeclaredMethods()).filter(
                m -> Arrays.stream(m.getDeclaredAnnotations()).anyMatch(a -> a instanceof jakarta.persistence.Id)
        ).findAny().orElseThrow(() -> new RuntimeException("No getter for primary key in class" + entityClass));
    }

    public Object getId(Class<?> entityClass, Object entity) {
        Method primaryKeyGetterMethod = getPrimaryKeyGetterMethod(entityClass);
        try {
            return primaryKeyGetterMethod.invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException("The primary key getter method couldn't be invoked", e);
        }
    }

    public Method getterMethod(Class<?> entityClass, Field attribute) {
        final String getterName = fieldToGetter(attribute.getName());
        return Arrays.stream(entityClass.getDeclaredMethods()).filter((Method m) -> m.getName().equals(getterName)).findAny().orElseThrow(() -> new RuntimeException("No getter for field " + attribute.getName() + " in class " + entityClass.getName()));
    }

    private static String fieldToGetter(String attributeName) {
        return "get" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1);
    }

    private String fieldToSetter(String attributeName) {
        return "set" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1);
    }

    public String getPrimaryKeyFieldName(Class<?> entityClass) {
        String primaryKeyGetterName = getPrimaryKeyGetterMethod(entityClass).getName();
        String primaryKeyFieldName = getterToFieldName(primaryKeyGetterName);
        return primaryKeyFieldName;
    }

    private static String getterToFieldName(String getterName) {
        return Character.toLowerCase(getterName.charAt(3)) + getterName.substring(4);
    }

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

    public Optional<Method> setterMethod(Class<?> entityClass, Field field) {
        final String setterName = fieldToSetter(field.getName());
        return Arrays.stream(entityClass.getDeclaredMethods()).filter((Method m) -> m.getName().equals(setterName)).findAny();
    }

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

    private Optional<Field> findInverseRelationshipFieldWithoutMappedByInformation(Class<?> targetEntityClass, Method getterMethod, RelationshipType relationshipType) {
        Class<?> sourceClass = getterMethod.getDeclaringClass();
        return Arrays.stream(targetEntityClass.getDeclaredFields())
            .filter((Field field) -> {
                if (field.getGenericType().equals(sourceClass)) {
                    return true;
                } else if(Collection.class.isAssignableFrom(field.getType())) {
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
            .map(entry -> entry.getKey()).findAny();
    }

    private static Optional<String> getMappedByValue(Method getterMethod) {
        return Arrays.stream(getterMethod.getDeclaredAnnotations()).map(a -> {
            if (a instanceof ManyToMany) {
                return ((ManyToMany) a).mappedBy();
            } else if (a instanceof OneToMany) {
                return ((OneToMany) a).mappedBy();
            } else if (a instanceof OneToOne) {
                return ((OneToOne) a).mappedBy();
            } else {
                return null;
            }
        }).filter(mappedBy -> null != mappedBy && !mappedBy.isEmpty()).findAny();
    }

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

    public Object getFieldValue(Class<?> entityClass, Object entity, String attributeName) {
        Optional<Field> field = attributeToField(entityClass, attributeName);
        return field.map(f -> getterMethod(entityClass, f)).map(getterMethod -> {
            try {
                return getterMethod.invoke(entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        })
        .orElseThrow(() -> new RuntimeException("No field named " + attributeName + " in " + entityClass.getName()));
    }

    private Optional<Field> attributeToField(Class<?> entityClass, String attributeName) {
        return Arrays.stream(entityClass.getDeclaredFields()).filter(f -> f.getName().equals(attributeName)).findAny();
    }

    public void setFieldValue(Class<?> entityClass, Object entity, String attributeName, Object value) {
        Optional<Field> field = attributeToField(entityClass, attributeName);
        field.ifPresentOrElse(f -> setFieldValue(entityClass, entity, f, value), () -> {throw new RuntimeException("No field with name "+ attributeName +" in class " + entityClass.getSimpleName());});
    }

    public void setFieldValue(Class<?> entityClass, Object entity, Field field, Object value) {
        setterMethod(entityClass, field).ifPresentOrElse(setterMethod -> {
            try {
                setterMethod.invoke(entity, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            throw new RuntimeException("No setter with name "+ fieldToSetter(field.getName()) +" in class " + entityClass.getSimpleName());
        });
    }

    public Object getId(Object entity) {
        return getId(entity.getClass(), entity);
    }
}
