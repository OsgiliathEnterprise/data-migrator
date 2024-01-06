package net.osgiliath.migrator.core.configuration.model;

public enum GraphDatasourceType {
    EMBEDDED("embedded"),
    REMOTE("remote");

    private final String value;

    GraphDatasourceType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
