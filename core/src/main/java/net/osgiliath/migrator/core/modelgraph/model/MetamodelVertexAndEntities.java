package net.osgiliath.migrator.core.modelgraph.model;

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;

import java.util.Collection;

public class MetamodelVertexAndEntities {

    private final MetamodelVertex metamodelVertex;
    private final Collection<?> entities;

    public MetamodelVertexAndEntities(MetamodelVertex metamodelVertex, Collection<?> entities) {
        this.metamodelVertex = metamodelVertex;
        this.entities = entities;
    }

    public MetamodelVertex getMetamodelVertex() {
        return metamodelVertex;
    }

    public Collection<?> getEntities() {
        return entities;
    }

}
