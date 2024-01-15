package net.osgiliath.migrator.core.modelgraph.model;

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;

public class MetamodelVertexAndModelElementAndModelElementId {
    private final MetamodelVertex metamodelVertex;
    private final ModelElement modelElement;
    private final Object id;

    public MetamodelVertexAndModelElementAndModelElementId(MetamodelVertex metamodelVertex, ModelElement modelElement, Object id) {
        this.metamodelVertex = metamodelVertex;
        this.modelElement = modelElement;
        this.id = id;
    }

    public MetamodelVertex getMetamodelVertex() {
        return metamodelVertex;
    }

    public ModelElement getModelElement() {
        return modelElement;
    }

    public Object getId() {
        return id;
    }
}
