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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.configuration.beans.CustomHikariDatasource;
import net.osgiliath.migrator.core.graph.InGraphVertexResolver;
import net.osgiliath.migrator.core.graph.ModelElementFactory;
import net.osgiliath.migrator.core.graph.OffGraphVertexResolver;
import net.osgiliath.migrator.core.graph.VertexResolver;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.JpaMetamodelVertexFactory;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    public static final String SOURCE_DATASOURCE = "sourceDataSource";
    public static final String SINK_DATASOURCE = "sinkDataSource";
    public static final String SOURCE_TRANSACTION_MANAGER = "sourceTransactionManager";
    public static final String SINK_TRANSACTION_MANAGER = "sinkTransactionManager";

    public static final String SOURCE_PU = "source";
    public static final String SINK_PU = "sink";

    public static final String SOURCE_JPA_PROPERTIES = "sourcePerDsJpaProperties";
    public static final String SINK_JPA_PROPERTIES = "sinkPerDsJpaProperties";

    @Bean
    @ConfigurationProperties("spring.datasource.source")
    public DataSourceProperties sourceDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.sink")
    @Primary
    public DataSourceProperties sinkDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(SOURCE_JPA_PROPERTIES)
    @ConfigurationProperties(prefix = "spring.jpa.source")
    public PerDSJpaProperties sourcePerDsJpaProperties() {
        return new PerDSJpaProperties();
    }

    @Bean(SINK_JPA_PROPERTIES)
    @ConfigurationProperties(prefix = "spring.jpa.sink")
    @Primary
    public PerDSJpaProperties sinkPerDsJpaProperties() {
        return new PerDSJpaProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.source.hikari")
    @Primary
    public HikariConfig sourceHikariConfigProperties() {
        return new HikariConfig();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.sink.hikari")
    @Primary
    public HikariConfig sinkHikariConfigProperties() {
        return new HikariConfig();
    }

    @Bean
    @ConditionalOnProperty(prefix = "data-migrator.graph-datasource", name = "type", havingValue = "embedded")
    public VertexResolver vertexResolverInGraph() {
        return new InGraphVertexResolver();
    }

    @Bean
    @ConditionalOnProperty(prefix = "data-migrator.graph-datasource", name = "type", havingValue = "remote")
    public VertexResolver vertexResolverOffGraph(JpaMetamodelVertexFactory jpaMetamodelVertexFactory, MetamodelScanner metamodelScanner, ModelElementFactory modelElementFactory, RawElementProcessor rawElementProcessor) {
        return new OffGraphVertexResolver(jpaMetamodelVertexFactory, metamodelScanner, modelElementFactory, rawElementProcessor);
    }


    @Bean(SOURCE_DATASOURCE)
    public DataSource sourceDataSource(
            @Qualifier("sourceDataSourceProperties") DataSourceProperties sourceDataSourceProperties,
            @Qualifier("sourceHikariConfigProperties") HikariConfig hikariConfigProperties
    ) {
        return createHikariDatasource(sourceDataSourceProperties, hikariConfigProperties);
    }

    private static HikariDataSource createHikariDatasource(DataSourceProperties dataSourceProperties, HikariConfig hikariConfigProperties) {
        HikariDataSource ds = dataSourceProperties.initializeDataSourceBuilder().type(CustomHikariDatasource.class).build();
        if (null != hikariConfigProperties.getDataSourceClassName()) {
            ds.setDataSourceClassName(hikariConfigProperties.getDataSourceClassName());
        }
        ds.setAutoCommit(hikariConfigProperties.isAutoCommit());
        ds.setPoolName(hikariConfigProperties.getPoolName());
        return ds;
    }

    @Bean(SINK_DATASOURCE)
    @Primary
    public DataSource sinkDataSource(@Qualifier("sinkDataSourceProperties") DataSourceProperties sinkDataSourceProperties,
                                     @Qualifier("sinkHikariConfigProperties") HikariConfig hikariConfigProperties) {
        return createHikariDatasource(sinkDataSourceProperties, hikariConfigProperties);
    }

}
