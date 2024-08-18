package net.osgiliath.migrator.core.configuration.model;

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

/**
 * The tinkerpop Graph datasource configuration.
 */
public class GraphDatasource {

    /**
     * The datasource type (embedded or remote).
     */
    private GraphDatasourceType type;

    private int port = 8182;

    private String host = "127.0.0.1";

    /**
     * Constructor.
     *
     * @param graphDatasourceType the datasource type (embedded or remote).
     */
    public GraphDatasource(GraphDatasourceType graphDatasourceType) {
        this.type = graphDatasourceType;
    }

    /**
     * Gets the datasource type (embedded or remote).
     *
     * @return the datasource type (embedded or remote).
     */
    public GraphDatasourceType getType() {
        return type;
    }

    /**
     * Sets the datasource type (embedded or remote).
     *
     * @param type the datasource type (embedded or remote).
     */
    public void setType(GraphDatasourceType type) {
        this.type = type;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
