package net.osgiliath.migrator.core.processing.model;

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.modelgraph.model.ModelElement;

public class SequencerBeanMetamodelVertexAndEntity {
    private final Object bean;
    private final MetamodelVertex metamodelVertex;
    private final ModelElement entity;

    public SequencerBeanMetamodelVertexAndEntity(Object bean, MetamodelVertex metamodelVertex, ModelElement entity) {
        this.bean = bean;
        this.metamodelVertex = metamodelVertex;
        this.entity = entity;
    }

    public Object getBean() {
        return bean;
    }

    public MetamodelVertex getMetamodelVertex() {
        return metamodelVertex;
    }

    public ModelElement getEntity() {
        return entity;
    }
}
