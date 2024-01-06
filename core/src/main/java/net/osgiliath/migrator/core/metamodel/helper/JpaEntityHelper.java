package net.osgiliath.migrator.core.metamodel.helper;

import net.osgiliath.migrator.core.api.metamodel.RelationshipType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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

    private void addEntityClassAsOwningSideIfMappedByIsNotDefinedOnBothSides(Class<?> entityClass, Method m) {
        if (randomManyToManyOwningSide.contains(entityClass)) {
            return;
        }
        for (Annotation a: m.getDeclaredAnnotations()) {
            if (a instanceof ManyToMany) {
                if (!((ManyToMany) a).mappedBy().isEmpty()) {
                    return;
                }
            }
        }
        Class<?> targetEntityClass = (Class<?>) ((ParameterizedType)m.getGenericReturnType()).getActualTypeArguments()[0];
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
        ).findAny().get();
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
        return Arrays.stream(entityClass.getDeclaredMethods()).filter((Method m) -> m.getName().equals(getterName)).findAny().get();
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

    public List<String> idAndRelationshipsAttributes(Class<?> entityClass) {
        List<String> idAndRelationships = new ArrayList<>();
        idAndRelationships.add(getPrimaryKeyFieldName(entityClass));
        for (Method m: entityClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(OneToMany.class) || m.isAnnotationPresent(ManyToMany.class) || m.isAnnotationPresent(OneToOne.class) || m.isAnnotationPresent(ManyToOne.class)) {
                String attributeName = getterToFieldName(m.getName());
                if (!isDerived(entityClass, attributeName)) {
                    idAndRelationships.add(attributeName);
                }
            }
        }
        return idAndRelationships;
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

    public Method setterMethod(Class<?> entityClass, Field field) {
        final String setterName = fieldToSetter(field.getName());
        return Arrays.stream(entityClass.getDeclaredMethods()).filter((Method m) -> m.getName().equals(setterName)).findAny().get();
    }

    public Field inverseRelationshipField(Method getterMethod, Class<?> targetEntityClass) {
        RelationshipType relationshipType = relationshipType(getterMethod);
        Optional<String> mappedBy = getMappedByValue(getterMethod);
        return mappedBy.map(
                mappedByValue -> Arrays.stream(targetEntityClass.getDeclaredFields()).filter(f -> f.getName().equals(mappedByValue)).findAny().get()
        ).orElseGet(() -> findInverseRelationshipFieldWithoutMappedByInformation(targetEntityClass, getterMethod, relationshipType));
    }

    private Field findInverseRelationshipFieldWithoutMappedByInformation(Class<?> targetEntityClass, Method getterMethod, RelationshipType relationshipType) {
        Class<?> sourceClass = getterMethod.getDeclaringClass();
        for (Field f: targetEntityClass.getDeclaredFields()) {
            if (f.getGenericType().equals(sourceClass)) {
                Method getterMethodOfField = getterMethod(targetEntityClass, f);
                RelationshipType inverseRelationshipType = relationshipType(getterMethodOfField);
                if (isInverseRelationshipType(relationshipType, inverseRelationshipType)) {
                    return f;
                }
            } else if(Collection.class.isAssignableFrom(f.getType())) {
                Class<?> typeOfCollection = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                if (typeOfCollection.equals(sourceClass)) {
                    Method getterMethodOfField = getterMethod(targetEntityClass, f);
                    RelationshipType inverseRelationshipType = relationshipType(getterMethodOfField);
                    if (isInverseRelationshipType(relationshipType, inverseRelationshipType)) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    private static Optional<String> getMappedByValue(Method getterMethod) {
        String mappedBy = null;
        for (Annotation a : getterMethod.getDeclaredAnnotations()) {
            if (a instanceof ManyToMany && !((ManyToMany) a).mappedBy().isEmpty()) {
                mappedBy = ((ManyToMany) a).mappedBy();
            } else if (a instanceof OneToMany && !((OneToMany) a).mappedBy().isEmpty()) {
                mappedBy = ((OneToMany) a).mappedBy();
            } else if (a instanceof OneToOne && !((OneToOne) a).mappedBy().isEmpty()) {
                mappedBy = ((OneToOne) a).mappedBy();
            }
        }
        return Optional.ofNullable(mappedBy);
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
}
