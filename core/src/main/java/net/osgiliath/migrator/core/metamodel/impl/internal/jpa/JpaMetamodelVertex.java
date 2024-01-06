package net.osgiliath.migrator.core.metamodel.impl.internal.jpa;

import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JpaMetamodelVertex implements MetamodelVertex {

    private static final Logger log = LoggerFactory.getLogger(JpaMetamodelVertex.class);

    private final Class<?> metamodelClass;

    private final Class<?> entityClass;
    private final JpaEntityHelper jpaEntityHelper;
    private final JpaMetamodelVertexFactory jpaMetamodelVertexFactory;


    private Map<Graph, Collection<OutboundEdge>> outboundEdges = new HashMap<>();

    public JpaMetamodelVertex(Class<?> metamodelClass, Class<?> entityClass, JpaEntityHelper jpaEntityHelper, JpaMetamodelVertexFactory jpaMetamodelVertexFactory) {
        this.metamodelClass = metamodelClass;
        this.entityClass = entityClass;
        this.jpaEntityHelper = jpaEntityHelper;
        this.jpaMetamodelVertexFactory = jpaMetamodelVertexFactory;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    private Class<?> getMetamodelClass() {
        return metamodelClass;
    }

    public Collection<OutboundEdge> getOutboundEdges(Graph<MetamodelVertex, FieldEdge> graph) {
        if (!outboundEdges.containsKey(graph)) {
            outboundEdges.put(graph, computeOutboundEdges(graph));
        }
        return outboundEdges.get(graph);
    }

    private class FieldAndTargetType {
        private final Field field;
        private final Type targetType;

        public FieldAndTargetType(Field field, Type targetType) {
            this.field = field;
            this.targetType = targetType;
        }

        public Field getField() {
            return field;
        }

        public Type getTargetType() {
            return targetType;
        }
    }

    private Collection<OutboundEdge> computeOutboundEdges(Graph<MetamodelVertex, FieldEdge> graph) {
        Collection<OutboundEdge> outboundEdges = new HashSet<>();
        Stream.of(getMetamodelClass().getDeclaredFields()).flatMap(f -> targetTypeOfMetamodelField(f).map(targetType -> new FieldAndTargetType(f, targetType)).stream())
            .flatMap(t ->
                graph.vertexSet().stream().filter(candidateVertex -> ((JpaMetamodelVertex)candidateVertex).getEntityClass().equals(t.getTargetType()))
                    .filter(targetMetamodelVertex -> !jpaEntityHelper.isDerived(getEntityClass(), t.getField().getName()))
                    .map(targetMetamodelVertex -> jpaMetamodelVertexFactory.createMetamodelEdge(new FieldEdge(t.getField()), targetMetamodelVertex))
            ).collect(Collectors.toCollection(() -> outboundEdges));
        return outboundEdges;
    }

    public boolean isEntity() {
        return getEntityClass().isAnnotationPresent(jakarta.persistence.Entity.class);
    }

    @Override
    public Method relationshipGetter(FieldEdge fieldEdge) {
        return jpaEntityHelper.getterMethod(getEntityClass(), fieldEdge.getMetamodelField());
    }

    @Override
    public Object getId(Object entity) {
        return jpaEntityHelper.getId(getEntityClass(), entity);
    }

    @Override
    public Map<String, Object> getAdditionalModelVertexProperties(Object entity) {
        return new HashMap<>();
    }

    @Override
    public String getTypeName() {
        return getEntityClass().getSimpleName();
    }

    private Optional<Type> targetTypeOfMetamodelField(Field f) {
        Type t  = f.getGenericType();
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            Type[] types = pt.getActualTypeArguments();
            if (types.length == 2) {
                return Optional.of(types[1]);
            }
        }
        return Optional.empty();
    }

        @Override
    public String toString() {
        return "ClassVertex{" +
            "metamodelClass=" + metamodelClass.getName() +
            ", entityClass=" + entityClass.getName() +
            '}';
    }
}
