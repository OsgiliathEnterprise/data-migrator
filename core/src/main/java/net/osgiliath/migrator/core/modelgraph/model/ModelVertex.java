package net.osgiliath.migrator.core.modelgraph.model;

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;

public class ModelVertex {
    private final MetamodelVertex metamodelVertex;
    private final Object entity;

    public ModelVertex(MetamodelVertex metamodelVertex, Object entity) {
        this.metamodelVertex = metamodelVertex;
        this.entity = entity;
    }

    public MetamodelVertex getMetamodelVertex() {
        return metamodelVertex;
    }


    public Object getEntity() {
        return entity;
    }

}
