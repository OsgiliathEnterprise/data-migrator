package net.osgiliath.migrator.core.configuration.model;

public class GraphDatasource {

    private GraphDatasourceType type;

    public GraphDatasource(GraphDatasourceType graphDatasourceType) {
        this.type = graphDatasourceType;
    }

    public GraphDatasourceType getType() {
        return type;
    }

    public void setType(GraphDatasourceType type) {
        this.type = type;
    }
}
