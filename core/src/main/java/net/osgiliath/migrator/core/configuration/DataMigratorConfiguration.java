package net.osgiliath.migrator.core.configuration;

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
