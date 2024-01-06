package net.osgiliath.migrator.core.metamodel.impl.internal.jpa;

import net.osgiliath.migrator.core.api.metamodel.MetamodelVertexFactory;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.OutboundEdge;
import net.osgiliath.migrator.core.metamodel.helper.JpaEntityHelper;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class JpaMetamodelVertexFactory implements MetamodelVertexFactory {
    private static final Logger log = LoggerFactory.getLogger(JpaMetamodelVertexFactory.class);
    private final JpaEntityHelper hibernateEntityHelper;


    public JpaMetamodelVertexFactory(JpaEntityHelper hibernateEntityHelper){
        this.hibernateEntityHelper = hibernateEntityHelper;
    }

    public MetamodelVertex createMetamodelVertex(Class<?> metamodelClass) {
        log.info("Creating a new metamodel vertex for metamodel class {}", metamodelClass.getName());
        return metamodelClassToEntityVertexAdapter(metamodelClass).orElseThrow(() -> new IllegalArgumentException("Cannot create a metamodel vertex for metamodel class " + metamodelClass.getName()));
    }

        public OutboundEdge createMetamodelEdge(FieldEdge fieldEdge, MetamodelVertex targetMetamodelVertex) {
        log.info("Creating a new field edge {} with target metamodel class {}", fieldEdge.getFieldName(), targetMetamodelVertex.getTypeName());
        return new OutboundEdge(fieldEdge, targetMetamodelVertex, hibernateEntityHelper);
    }


    private Optional<MetamodelVertex> metamodelClassToEntityVertexAdapter(final Class<?> metamodelClass) {
        return Stream.of(metamodelClass.getDeclaredFields())
            .filter((Field f) -> "class_".equals(f.getName()))
            .map(f3 -> f3.getGenericType())
            .filter(t2 -> t2 instanceof ParameterizedType)
            .map(t3 -> ((ParameterizedType) t3).getActualTypeArguments()[0])
            .filter(t4 -> t4 instanceof Class)
            .map(t5 -> (Class<?>) t5)
            .map(c -> internalCreateMetamodelVertex(metamodelClass, c))
            .findAny();
    }

    private MetamodelVertex internalCreateMetamodelVertex(Class<?> metamodelClass, Class<?> entityClass) {
        return new JpaMetamodelVertex(metamodelClass, entityClass, hibernateEntityHelper, this);
    }
}
