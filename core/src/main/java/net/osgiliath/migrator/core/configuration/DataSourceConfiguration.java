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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

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
    public VertexResolver vertexResolverOffGraph(JpaMetamodelVertexFactory jpaMetamodelVertexFactory, MetamodelScanner metamodelScanner, ModelElementFactory modelElementFactory, RawElementProcessor rawElementProcessor, @Qualifier(SINK_TRANSACTION_MANAGER) PlatformTransactionManager sinkPlatformTxManager) {
        return new OffGraphVertexResolver(jpaMetamodelVertexFactory, metamodelScanner, modelElementFactory, rawElementProcessor, sinkPlatformTxManager);
    }


    @Bean(SOURCE_DATASOURCE)
    public DataSource sourceDataSource(
            @Qualifier("sourceDataSourceProperties") DataSourceProperties sourceDataSourceProperties,
            @Qualifier("sourceHikariConfigProperties") HikariConfig hikariConfigProperties
    ) {
        return createHikariDatasource(sourceDataSourceProperties, hikariConfigProperties);
    }

    private static DataSource createHikariDatasource(DataSourceProperties dataSourceProperties, HikariConfig hikariConfigProperties) {
        CustomHikariDatasource ds = dataSourceProperties.initializeDataSourceBuilder().type(CustomHikariDatasource.class)
                .build();
        if (null != hikariConfigProperties.getCatalog()) {
            ds.setCatalog(hikariConfigProperties.getCatalog());
        }
        ds.setConnectionTimeout(hikariConfigProperties.getConnectionTimeout());
        ds.setValidationTimeout(hikariConfigProperties.getValidationTimeout());
        ds.setIdleTimeout(hikariConfigProperties.getIdleTimeout());
        ds.setLeakDetectionThreshold(hikariConfigProperties.getLeakDetectionThreshold());
        ds.setMaxLifetime(hikariConfigProperties.getMaxLifetime());
        ds.setMaximumPoolSize(hikariConfigProperties.getMaximumPoolSize());
        if (null != ds.getCredentials()) {
            ds.setCredentials(ds.getCredentials());
        }
        ds.setInitializationFailTimeout(hikariConfigProperties.getInitializationFailTimeout());
        if (null != hikariConfigProperties.getConnectionInitSql()) {
            ds.setConnectionInitSql(hikariConfigProperties.getConnectionInitSql());
        }
        if (null != hikariConfigProperties.getConnectionTestQuery()) {
            ds.setConnectionTestQuery(hikariConfigProperties.getConnectionTestQuery());
        }
        if (null != hikariConfigProperties.getDataSourceClassName()) {
            ds.setDataSourceClassName(hikariConfigProperties.getDataSourceClassName());
        }
        if (null != hikariConfigProperties.getDataSourceJNDI()) {
            ds.setDataSourceJNDI(hikariConfigProperties.getDataSourceJNDI());
        }
        if (null != hikariConfigProperties.getDriverClassName()) {
            ds.setDriverClassName(hikariConfigProperties.getDriverClassName());
        }
        if (null != hikariConfigProperties.getExceptionOverrideClassName()) {
            hikariConfigProperties.setExceptionOverrideClassName(hikariConfigProperties.getExceptionOverrideClassName());
        }
        if (null != hikariConfigProperties.getExceptionOverride()) {
            ds.setExceptionOverride(hikariConfigProperties.getExceptionOverride());
        }
        if (null != hikariConfigProperties.getJdbcUrl()) {
            ds.setJdbcUrl(hikariConfigProperties.getJdbcUrl());
        }
        if (null != hikariConfigProperties.getPoolName()) {
            ds.setPoolName(hikariConfigProperties.getPoolName());
        }
        if (null != hikariConfigProperties.getSchema()) {
            ds.setSchema(hikariConfigProperties.getSchema());
        }
        if (null != hikariConfigProperties.getTransactionIsolation()) {
            ds.setTransactionIsolation(hikariConfigProperties.getTransactionIsolation());
        }
        ds.setAutoCommit(hikariConfigProperties.isAutoCommit());
        ds.setReadOnly(hikariConfigProperties.isReadOnly());
        ds.setIsolateInternalQueries(hikariConfigProperties.isIsolateInternalQueries());
        ds.setRegisterMbeans(hikariConfigProperties.isRegisterMbeans());
        ds.setAllowPoolSuspension(hikariConfigProperties.isAllowPoolSuspension());
        if (null != hikariConfigProperties.getThreadFactory()) {
            ds.setThreadFactory(hikariConfigProperties.getThreadFactory());
        }
        if (null != hikariConfigProperties.getScheduledExecutor()) {
            ds.setScheduledExecutor(hikariConfigProperties.getScheduledExecutor());
        }
        if (null != hikariConfigProperties.getMetricsTrackerFactory()) {
            ds.setMetricsTrackerFactory(hikariConfigProperties.getMetricsTrackerFactory());
        }
        if (null != hikariConfigProperties.getMetricRegistry()) {
            ds.setMetricRegistry(hikariConfigProperties.getMetricRegistry());
        }
        if (null != hikariConfigProperties.getHealthCheckRegistry()) {
            ds.setHealthCheckRegistry(hikariConfigProperties.getHealthCheckRegistry());
        }
        if (null != hikariConfigProperties.getHealthCheckProperties()) {
            ds.setHealthCheckProperties(hikariConfigProperties.getHealthCheckProperties());
        }
        ds.setKeepaliveTime(hikariConfigProperties.getKeepaliveTime());
        return ds;
    }

    @Bean(SINK_DATASOURCE)
    @Primary
    public DataSource sinkDataSource(@Qualifier("sinkDataSourceProperties") DataSourceProperties sinkDataSourceProperties,
                                     @Qualifier("sinkHikariConfigProperties") HikariConfig hikariConfigProperties) {
        return createHikariDatasource(sinkDataSourceProperties, hikariConfigProperties);
    }

}
