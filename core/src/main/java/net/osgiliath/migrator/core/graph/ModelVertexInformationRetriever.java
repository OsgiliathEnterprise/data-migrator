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
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.graph.model.MetamodelVertexAndModelElement;
import net.osgiliath.migrator.core.graph.model.MetamodelVertexAndModelElementAndModelElementId;
import net.osgiliath.migrator.core.graph.model.MetamodelVertexAndModelElements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;

@Component
public class ModelVertexInformationRetriever {

    private final EntityImporter entityImporter;
    private final ModelElementProcessor modelElementProcessor;
    private final TransactionTemplate sourceTxTemplate;

    public ModelVertexInformationRetriever(EntityImporter entityImporter, ModelElementProcessor modelElementProcessor, @Qualifier(SOURCE_TRANSACTION_MANAGER) PlatformTransactionManager sourcePlatformTxManager) {
        this.entityImporter = entityImporter;
        this.modelElementProcessor = modelElementProcessor;
        this.sourceTxTemplate = new TransactionTemplate(sourcePlatformTxManager);
        sourceTxTemplate.setReadOnly(true);

    }

    public Stream<MetamodelVertexAndModelElementAndModelElementId> getMetamodelVertexAndModelElementAndModelElementIdStreamForMetamodelVertex(MetamodelVertex mv) {
        return getMetamodelVertexAndModelElementAndModelElementIdStreamForMetamodelVertexStream(mv);
    }

    private Stream<MetamodelVertexAndModelElementAndModelElementId> getMetamodelVertexAndModelElementAndModelElementIdStreamForMetamodelVertexStream(MetamodelVertex mv) {
        return new MetamodelVertexAndModelElements(mv, getModelElementsOfVertex(mv).stream())
                .modelElements().map(modelElement -> new MetamodelVertexAndModelElement(mv, modelElement))
                .flatMap(mvae -> modelElementProcessor.getId(mvae.modelElement()).stream().map(eid -> new MetamodelVertexAndModelElementAndModelElementId(mvae.metamodelVertex(), mvae.modelElement(), eid))
                );
    }

    private Collection<ModelElement> getModelElementsOfVertex(MetamodelVertex mv) {
        return sourceTxTemplate.execute(status -> entityImporter.importEntities(mv, new ArrayList<>()).collect(Collectors.toSet())
        );
    }
}
