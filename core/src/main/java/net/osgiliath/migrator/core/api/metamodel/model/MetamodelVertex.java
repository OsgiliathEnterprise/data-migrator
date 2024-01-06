package net.osgiliath.migrator.core.api.metamodel.model;

import org.jgrapht.Graph;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public interface MetamodelVertex {

    Collection<OutboundEdge> getOutboundEdges(Graph<MetamodelVertex, FieldEdge> graph);

    String getTypeName();

    boolean isEntity();

    Method relationshipGetter(FieldEdge fieldEdge);

    Object getId(Object entity);

    /**
     * @return Additional properties to display in the model graph (Key: property name, Value: property value).
     */
    Map<String, Object> getAdditionalModelVertexProperties(Object entity);
}
