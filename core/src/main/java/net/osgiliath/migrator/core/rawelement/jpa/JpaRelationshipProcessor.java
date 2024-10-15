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
import net.osgiliath.migrator.core.rawelement.RelationshipProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Stream;

@Component
public class JpaRelationshipProcessor implements RelationshipProcessor {

    /**
     * List of many to many owning side chosen randomly (when no mappedBy instruction is set on any of both sides).
     */
    private static final Collection<Class<?>> randomManyToManyOwningSide = new ArrayList<>();

    private final JpaEntityProcessor jpaEntityProcessor;
    private static final Logger log = LoggerFactory.getLogger(JpaRelationshipProcessor.class);

    public JpaRelationshipProcessor(JpaEntityProcessor jpaEntityProcessor) {
        this.jpaEntityProcessor = jpaEntityProcessor;
    }

    /**
     * Returns the Raw value(s) corresponding to the entity referenced by the outboundEdge
     *
     * @param modelElement the model element to get raw value from
     * @param getterMethod the method to call
     * @return Returns the ModelElement(s) corresponding to the entity referenced by the outboundEdge
     */
    // @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    @Override
    public Object getEdgeRawValue(ModelElement modelElement, Method getterMethod) {
        try {
            return getterMethod.invoke(modelElement.rawElement());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ErrorCallingRawElementMethodException(e);
        }
    }

    @Override
    public void setEdgeRawValue(String fieldName, Field metamodelField, MetamodelVertex target, ModelElement modelElement, Object value) {
        log.debug("setting raw value for edge {} to modelElement {}", fieldName, jpaEntityProcessor.getId(modelElement).get());
        jpaEntityProcessor.setFieldValue(modelElement, fieldName, value);
        jpaEntityProcessor.getterMethod(modelElement.vertex(), metamodelField).flatMap(
                method -> inverseRelationshipField(method, target)
        ).ifPresent(inverseField -> {
                    jpaEntityProcessor.getterMethod(target, inverseField).ifPresent(
                            inverseRelationshipMethod -> {
                                RelationshipType type = relationshipType(inverseRelationshipMethod);
                                log.debug("setting raw value for type {}", type);
                                if (value instanceof Collection<?> c) {
                                    for (Object o : c) {
                                        inverseSetRelationship(target, modelElement, inverseField, inverseRelationshipMethod, type, o);
                                    }
                                } else if (value != null) {
                                    inverseSetRelationship(target, modelElement, inverseField, inverseRelationshipMethod, type, value);
                                }
                            }
                    );
                }
        );
    }

    private void inverseSetRelationship(MetamodelVertex target, ModelElement modelElement, Field inverseField, Method inverseRelationshipMethod, RelationshipType type, Object o) {
        ModelElement elt = new ModelElement(target, o);
        if (o != null) {
            if (type.equals(RelationshipType.ONE_TO_MANY) || type.equals(RelationshipType.MANY_TO_MANY)) {
                try {
                    Collection set = (Collection) inverseRelationshipMethod.invoke(o);
                    set.add(modelElement.rawElement());
                    jpaEntityProcessor.setFieldValue(elt, inverseField.getName(), set);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            } else {
                jpaEntityProcessor.setFieldValue(elt, inverseField.getName(), modelElement.rawElement());
            }
        }
    }


    @Override
    public void resetElementRelationships(ModelElement elt) {
        try {
            Arrays.stream(Introspector.getBeanInfo(elt.rawElement().getClass()).getPropertyDescriptors()).map(PropertyDescriptor::getReadMethod).forEach(getterMethod -> {
                try {
                    RelationshipType type = relationshipType(getterMethod);
                    if (isMany(type)) {
                        jpaEntityProcessor.setFieldValue(elt, jpaEntityProcessor.getterToFieldName(getterMethod.getName()), new HashSet<>());
                    } else {
                        jpaEntityProcessor.setFieldValue(elt, jpaEntityProcessor.getterToFieldName(getterMethod.getName()), null);
                    }
                } catch (RawElementFieldOrMethodNotFoundException r) {
                    // Do nothing
                }
            });
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
     * @return the inverse relationship field. Note that result can be wrong: one of the relationship will be taken if many
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
                }).flatMap((Field field) -> {
                    Optional<Method> getterMethodOfFieldOpt = jpaEntityProcessor.getterMethod(targetEntityClass, field);
                    Optional<RelationshipType> inverseRelationshipTypeOpt = getterMethodOfFieldOpt.map(this::relationshipType);
                    return inverseRelationshipTypeOpt.map(inverseRelationshipType -> new AbstractMap.SimpleEntry<>(field, inverseRelationshipType)).stream();
                }).filter(entry -> isInverseRelationshipType(relationshipType, entry.getValue()))
                .map(Map.Entry::getKey).findAny();
    }

    /**
     * Gets the mappedBy value.
     *
     * @param getterMethod the getter method to get the information from (the annotation value).
     * @return the mappedBy value.
     */
    private Optional<String> getMappedByValue(Method getterMethod) {
        return Arrays.stream(getterMethod.getDeclaredAnnotations()).map(a ->
                switch (a) {
                    case ManyToMany mtm -> {
                        if (mtm.mappedBy() != null && !mtm.mappedBy().isEmpty()) {
                            yield mtm.mappedBy();
                        } else {
                            // Finding the opposite Class
                            Class<?> targetClass = (Class<?>) ((ParameterizedType) getterMethod.getGenericReturnType()).getActualTypeArguments()[0];
                            boolean mappedByFound = false;
                            try {
                                mappedByFound = Arrays.stream(Introspector.getBeanInfo(targetClass)
                                                .getPropertyDescriptors()).map(PropertyDescriptor::getReadMethod)
                                        .filter(method -> Collection.class.isAssignableFrom(method.getReturnType()) && ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0].equals(getterMethod.getGenericReturnType()))
                                        .flatMap(targetEntityClassMethod -> Stream.of(targetEntityClassMethod.getDeclaredAnnotations()))
                                        .filter(annotation -> annotation instanceof ManyToMany && !((ManyToMany) annotation).mappedBy().isEmpty())
                                        .filter(tcmtm -> ((ManyToMany) tcmtm).mappedBy().equals(jpaEntityProcessor.fieldNameOfGetter(getterMethod)))
                                        .count() > 0;
                            } catch (IntrospectionException e) {
                                throw new RuntimeException(e);
                            }
                            if (mappedByFound) {
                                yield "";
                            }
                            addEntityClassAsOwningSideIfMappedByIsNotDefinedOnBothSides(getterMethod.getDeclaringClass(), getterMethod);
                            yield randomManyToManyOwningSide.stream()
                                    .filter(lst -> getterMethod.getDeclaringClass().equals(lst)).findAny()
                                    .map(type ->
                                            this
                                                    .findInverseRelationshipFieldWithoutMappedByInformation((Class<?>) ((ParameterizedType) getterMethod.getGenericReturnType()).getActualTypeArguments()[0], getterMethod, RelationshipType.MANY_TO_MANY)
                                                    .get().getName())
                                    .orElse("");
                        }
                    }
                    case OneToMany otm -> otm.mappedBy();
                    case OneToOne oto -> oto.mappedBy();
                    default -> "";
                }
        ).filter(mappedBy -> !mappedBy.isEmpty()).findAny();
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
        try {
            log.debug("finding inverse many to many method: {} for class {}", manyToManyMethod.getName(), targetEntityClass.getSimpleName());
            Arrays.stream(Introspector.getBeanInfo(targetEntityClass)
                            .getPropertyDescriptors()).map(PropertyDescriptor::getReadMethod)
                    .filter(method -> Collection.class.isAssignableFrom(method.getReturnType()) && entityClass.equals(((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0]))
                    .flatMap(method -> Arrays.stream(method.getDeclaredAnnotations()))
                    .filter(a -> a instanceof ManyToMany mtm && mtm.mappedBy().isEmpty())
                    .forEach(annotation -> {
                        if (!randomManyToManyOwningSide.contains(targetEntityClass)) {
                            if (entityClass.getName().compareToIgnoreCase(targetEntityClass.getName()) < 1) {
                                randomManyToManyOwningSide.add(entityClass);
                            }
                        }
                    });
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Returns true if the target relationship is a collections, false if it's a single element.
     *
     * @param type the field edge to get the type of relationship for
     * @return true if it's a many relationship.
     */
    private boolean isMany(RelationshipType type) {
        return type.equals(RelationshipType.MANY_TO_MANY) || type.equals(RelationshipType.ONE_TO_MANY);
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
                    .flatMap(m -> getMappedByValue(m).stream())
                    .count() > 0

                    /*.anyMatch(
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
                    )*/;
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

    private static boolean joinTableIgnoresFk(JoinTable a1) {
        boolean ignores = a1.foreignKey().value().equals(ConstraintMode.NO_CONSTRAINT);
        return ignores || a1.foreignKey().value().equals(ConstraintMode.NO_CONSTRAINT) || Stream.of(a1.joinColumns()).anyMatch(JpaRelationshipProcessor::joinColumnIgnoresFk);
    }

    private static boolean joinColumnsIgnoresFk(JoinColumns a1) {
        return Stream.of(a1.value()).anyMatch(JpaRelationshipProcessor::joinColumnIgnoresFk);
    }

    private static boolean joinColumnIgnoresFk(JoinColumn a1) {
        return a1.foreignKey().value().equals(ConstraintMode.NO_CONSTRAINT);
    }

    private static boolean joinColumnIgnoresFk(PrimaryKeyJoinColumn a1) {
        return a1.foreignKey().value().equals(ConstraintMode.NO_CONSTRAINT);
    }
}
