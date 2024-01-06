package net.osgiliath.migrator.core.api.metamodel.model;

import net.osgiliath.migrator.core.api.metamodel.RelationshipType;
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertex;
import jakarta.persistence.Persistence;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class OutboundEdge {

    private final FieldEdge fieldEdge;
    private final MetamodelVertex targetVertex;
    private final JpaEntityHelper hibernateEntityHelper;

    public OutboundEdge(FieldEdge fieldEdge, MetamodelVertex targetVertex, JpaEntityHelper hibernateEntityHelper) {
        this.fieldEdge = fieldEdge;
        this.targetVertex = targetVertex;
        this.hibernateEntityHelper = hibernateEntityHelper;
    }

    public MetamodelVertex getTargetVertex() {
        return targetVertex;
    }

    public FieldEdge getFieldEdge() {
        return fieldEdge;
    }

    public void setEdgeBetweenEntities(MetamodelVertex sourceMetamodelVertex, Object sourceEntity, Object targetEntity) throws InvocationTargetException, IllegalAccessException {
        Field field = fieldEdge.getMetamodelField();
        Method getterMethod = hibernateEntityHelper.getterMethod(((JpaMetamodelVertex)sourceMetamodelVertex).getEntityClass(), field);
        RelationshipType relationshipType = hibernateEntityHelper.relationshipType(getterMethod);
        switch (relationshipType) {
            case ONE_TO_ONE -> {
                hibernateEntityHelper.setterMethod(((JpaMetamodelVertex)sourceMetamodelVertex).getEntityClass(), field).invoke(sourceEntity, targetEntity);
                Field inverseField = hibernateEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex)targetVertex).getEntityClass());
                if (inverseField != null) {
                    hibernateEntityHelper.setterMethod(((JpaMetamodelVertex)targetVertex).getEntityClass(), inverseField).invoke(targetEntity, sourceEntity);
                }
            }
            case ONE_TO_MANY -> {
                Set<Object> set = (Set<Object>) getterMethod.invoke(sourceEntity);
                set.add(targetEntity);
                Field inverseField = hibernateEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex)targetVertex).getEntityClass());
                if (inverseField != null) {
                    hibernateEntityHelper.setterMethod(((JpaMetamodelVertex)targetVertex).getEntityClass(), inverseField).invoke(targetEntity, sourceEntity);
                }
            }
            case MANY_TO_ONE -> {
                hibernateEntityHelper.setterMethod(((JpaMetamodelVertex)sourceMetamodelVertex).getEntityClass(), field).invoke(sourceEntity, targetEntity);
                Field inverseField = hibernateEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex)targetVertex).getEntityClass());
                Method inverseGetterMethod = hibernateEntityHelper.getterMethod(((JpaMetamodelVertex)targetVertex).getEntityClass(), inverseField);
                if (inverseGetterMethod != null) {
                    Set inverseCollection = (Set) inverseGetterMethod.invoke(targetEntity);
                    if (inverseCollection != null) {
                        if (!Persistence.getPersistenceUtil().isLoaded(targetEntity,inverseField.getName())) {
                            inverseCollection = new HashSet(0);
                        }
                        inverseCollection.add(sourceEntity);
                        hibernateEntityHelper.setterMethod(((JpaMetamodelVertex)targetVertex).getEntityClass(), inverseField).invoke(targetEntity, inverseCollection);
                    }
                }
            }
            case MANY_TO_MANY -> {
                Set<Object> set = (Set<Object>) getterMethod.invoke(sourceEntity);
                set.add(targetEntity);
                Field inverseField = hibernateEntityHelper.inverseRelationshipField(getterMethod, ((JpaMetamodelVertex)targetVertex).getEntityClass());
                Method inverseGetterMethod = hibernateEntityHelper.getterMethod(((JpaMetamodelVertex)targetVertex).getEntityClass(), inverseField);
                if (inverseGetterMethod != null) {
                    Set inverseCollection = (Set) inverseGetterMethod.invoke(targetEntity);
                    if (inverseCollection != null) {
                        if (!Persistence.getPersistenceUtil().isLoaded(targetEntity,inverseField.getName())) {
                            inverseCollection = new HashSet(0);
                        }
                        inverseCollection.add(sourceEntity);
                        hibernateEntityHelper.setterMethod(((JpaMetamodelVertex)targetVertex).getEntityClass(), inverseField).invoke(targetEntity, inverseCollection);
                    }
                }
            }
        }
    }
}
