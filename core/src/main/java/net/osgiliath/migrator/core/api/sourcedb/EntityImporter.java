package net.osgiliath.migrator.core.api.sourcedb;

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;

import java.util.List;

public interface EntityImporter {

    List<?> importEntities(MetamodelVertex entityVertex, List<Object> objectToExclude);
}
