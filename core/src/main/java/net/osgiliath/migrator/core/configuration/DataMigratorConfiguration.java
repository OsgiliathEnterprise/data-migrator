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

@ConfigurationProperties(prefix = "data-migrator")
public class DataMigratorConfiguration {

    private String modelBasePackage;

    private GraphDatasource graphDatasource = new GraphDatasource(GraphDatasourceType.EMBEDDED);
    private List<String> sequence;

    private List<? extends AbstractTransformationConfigurationDefinition> sequencers;

    public GraphDatasource getGraphDatasource() {
        return graphDatasource;
    }

    public void setGraphDatasource(GraphDatasource graphDatasource) {
        this.graphDatasource = graphDatasource;
    }

    public String getModelBasePackage() {
        return modelBasePackage;
    }

    public void setModelBasePackage(String modelBasePackage) {
        this.modelBasePackage = modelBasePackage;
    }

    public List<String> getSequence() {
        return sequence;
    }

    public void setSequence(List<String> sequence) {
        this.sequence = sequence;
    }

    public List<? extends AbstractTransformationConfigurationDefinition> getSequencers() {
        return sequencers;
    }

    public void setSequencers(List<? extends AbstractTransformationConfigurationDefinition> sequencers) {
        this.sequencers = sequencers;
    }
}
