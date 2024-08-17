package net.osgiliath.migrator.core.graph;

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
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.graph.model.MetamodelVertexAndModelElement;
import net.osgiliath.migrator.core.graph.model.MetamodelVertexAndModelElementAndModelElementId;
import net.osgiliath.migrator.core.graph.model.MetamodelVertexAndModelElements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class ModelVertexInformationRetriever {

    private final EntityImporter entityImporter;
    private final ModelElementProcessor modelElementProcessor;

    public ModelVertexInformationRetriever(EntityImporter entityImporter, ModelElementProcessor modelElementProcessor) {
        this.entityImporter = entityImporter;
        this.modelElementProcessor = modelElementProcessor;
    }

    // @Transactional(readOnly = true, transactionManager = SOURCE_TRANSACTION_MANAGER)
    public Collection<MetamodelVertexAndModelElementAndModelElementId> getMetamodelVertexAndModelElementAndModelElementIdStreamForMetamodelVertex(MetamodelVertex mv) {
        return new MetamodelVertexAndModelElements(mv, entityImporter.importEntities(mv, new ArrayList<>()))
                .modelElements().map(modelElement -> new MetamodelVertexAndModelElement(mv, modelElement))
                .flatMap(mvae -> modelElementProcessor.getId(mvae.modelElement()).stream().map(eid -> new MetamodelVertexAndModelElementAndModelElementId(mvae.metamodelVertex(), mvae.modelElement(), eid))
                )
                .collect(Collectors.toSet());
    }
}
