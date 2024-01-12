package net.osgiliath.migrator.core.configuration;

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

import net.osgiliath.migrator.core.configuration.model.GraphDatasource;
import net.osgiliath.migrator.core.configuration.model.GraphDatasourceType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Data migrator configuration.
 */
@ConfigurationProperties(prefix = "data-migrator")
public class DataMigratorConfiguration {

    /**
     * Package to scan the JPA entities representing tables.
     */
    private String modelBasePackage;

    /**
     * TinkerGraph datasource configuration.
     */
    private GraphDatasource graphDatasource = new GraphDatasource(GraphDatasourceType.EMBEDDED);
    /**
     * Sequence of transformations to apply.
     */
    private List<String> sequence;

    /**
     * List of sequencers being able to handle the sequence.
     */
    private List<? extends TransformationConfigurationDefinition> sequencers;

    /**
     * Graph datasource configuration.
     * @return
     */
    public GraphDatasource getGraphDatasource() {
        return graphDatasource;
    }

    /**
     * Graph datasource configuration.
     * @param graphDatasource the graph datasource technology to set.
     */
    public void setGraphDatasource(GraphDatasource graphDatasource) {
        this.graphDatasource = graphDatasource;
    }

    /**
     * Package to scan the JPA entities representing tables.
     * @return the package to scan.
     */
    public String getModelBasePackage() {
        return modelBasePackage;
    }

    /**
     * Package to scan the JPA entities representing tables.
     * @param modelBasePackage the package to scan.
     */
    public void setModelBasePackage(String modelBasePackage) {
        this.modelBasePackage = modelBasePackage;
    }

    /**
     * Sequence of transformations to apply.
     * @return the sequence of transformations to apply.
     */
    public List<String> getSequence() {
        return sequence;
    }

    /**
     * Sequence of transformations to apply.
     * @param sequence the sequence of transformations to apply.
     */
    public void setSequence(List<String> sequence) {
        this.sequence = sequence;
    }

    /**
     * List of sequencers being able to handle the sequence.
     * @return the list of sequencers.
     */
    public List<? extends TransformationConfigurationDefinition> getSequencers() {
        return sequencers;
    }

    /**
     * List of sequencers being able to handle the sequence.
     * @param sequencers the list of sequencers.
     */
    public void setSequencers(List<? extends TransformationConfigurationDefinition> sequencers) {
        this.sequencers = sequencers;
    }
}
