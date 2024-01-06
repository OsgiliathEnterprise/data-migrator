package net.osgiliath.migrator.core.api.metamodel;

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;

public interface MetamodelVertexFactory {
    MetamodelVertex createMetamodelVertex(Class<?> metamodelClass);
}
