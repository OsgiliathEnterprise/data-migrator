package net.osgiliath.migrator.core.processing.model;

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.modelgraph.model.ModelElement;

import java.util.Collection;

public class SequencersBeansMetamodelVertexAndEntity {
    private final Collection beans;
    private final MetamodelVertex metamodelVertex;
    private final ModelElement entity;

    public SequencersBeansMetamodelVertexAndEntity(Collection beans, MetamodelVertex metamodelVertex, ModelElement entity) {
        this.beans = beans;
        this.metamodelVertex = metamodelVertex;
        this.entity = entity;
    }

    public Collection getBeans() {
        return beans;
    }

    public MetamodelVertex getMetamodelVertex() {
        return metamodelVertex;
    }

    public ModelElement getEntity() {
        return entity;
    }
}
