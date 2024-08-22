package net.osgiliath.migrator.core.api.sourcedb;

/*-
 * #%L
 * data-migrator-core
 * %%
 * Copyright (C) 2024 Osgiliath Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Imports entities from the source database.
 */
public interface EntityImporter {

    /**
     * Imports entities from the source database.
     *
     * @param entityVertex    the metamodel vertex representing the entity.
     * @param objectToExclude list of objects to exclude from the import.
     * @return the list of imported entities from the DB.
     */
    Stream<ModelElement> importEntities(MetamodelVertex entityVertex, Collection<ModelElement> objectToExclude);
}
