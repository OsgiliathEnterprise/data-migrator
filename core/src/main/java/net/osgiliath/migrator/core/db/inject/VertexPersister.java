package net.osgiliath.migrator.core.db.inject;

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

import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class VertexPersister {

    private final ModelElementProcessor modelElementProcessor;
    private final ApplicationContext context;
    private static Logger log = LoggerFactory.getLogger(VertexPersister.class);

    public VertexPersister(ModelElementProcessor modelElementProcessor, ApplicationContext context) {
        this.modelElementProcessor = modelElementProcessor;
        this.context = context;
    }

    @SuppressWarnings("java:S3864")
    public Stream<ModelElement> persistVertices(Stream<ModelElement> entities) {
        log.info("******************** Persisting a batch of entities ****************");

        try {
            return entities
                    .peek((ModelElement me) -> {
                        if (log.isInfoEnabled()) {
                            log.info("Persisting entity of type {}", me.vertex().getTypeName());
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Persisting with id {}", me.vertex().getTypeName(), modelElementProcessor.getId(me).get());
                        }
                    })
                    .map((ModelElement me) -> {
                        CrudRepository elementRepo = getCrudRepositoryForVertex(me);
                        elementRepo.save(me.rawElement());
                        //  me.setRawElement(em.merge(me.rawElement()));
                        return me;
                    });
        } catch (Exception e) {
            log.error("******************** ERROR Persisting last batch of entities ****************");
            log.warn("Unable to persist last batch of entities, you may retry once", e);
            log.error("******************** End error ****************");
        }
        return Stream.empty();
    }

    private CrudRepository getCrudRepositoryForVertex(ModelElement me) {
        String[] typeDotSplitted = me.vertex().getTypeName().split("\\.");
        String typeName = typeDotSplitted[typeDotSplitted.length - 1];
        String typeToBean = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
        CrudRepository elementRepo = ((CrudRepository) context.getBean(typeToBean + "Repository"));
        return elementRepo;
    }
}
