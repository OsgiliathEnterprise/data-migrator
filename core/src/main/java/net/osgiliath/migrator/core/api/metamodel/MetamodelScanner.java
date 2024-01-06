package net.osgiliath.migrator.core.api.metamodel;

import java.util.Collection;

public interface MetamodelScanner {
    Collection<Class<?>> scanMetamodelClasses();
}
