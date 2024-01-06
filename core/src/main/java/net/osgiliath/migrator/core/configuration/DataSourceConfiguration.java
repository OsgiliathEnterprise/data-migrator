package net.osgiliath.migrator.core.configuration;

import net.osgiliath.migrator.core.configuration.beans.CustomHikariDatasource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    public DataSourceConfiguration() {}

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

    @Bean("sourcePerDsJpaProperties")
    @ConfigurationProperties(prefix = "spring.jpa.source")
    public PerDSJpaProperties sourcePerDsJpaProperties() {
        return new PerDSJpaProperties();
    }

    @Bean("sinkPerDsJpaProperties")
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


    @Bean("sourceDataSource")
    public DataSource sourceDataSource(
            @Qualifier("sourceDataSourceProperties") DataSourceProperties sourceDataSourceProperties,
            @Qualifier("sourceHikariConfigProperties") HikariConfig hikariConfigProperties
    ) {
        HikariDataSource ds = sourceDataSourceProperties.initializeDataSourceBuilder().type(CustomHikariDatasource.class).build();
        if (null != hikariConfigProperties.getDataSourceClassName()) {
            ds.setDataSourceClassName(hikariConfigProperties.getDataSourceClassName());
        }
        ds.setAutoCommit(hikariConfigProperties.isAutoCommit());
        ds.setPoolName(hikariConfigProperties.getPoolName());
        return ds;
    }

    @Bean("sinkDataSource")
    @Primary
    public DataSource sinkDataSource(@Qualifier("sinkDataSourceProperties") DataSourceProperties sinkDataSourceProperties,
                                     @Qualifier("sinkHikariConfigProperties") HikariConfig hikariConfigProperties) {
        HikariDataSource ds = sinkDataSourceProperties.initializeDataSourceBuilder().type(CustomHikariDatasource.class).build();
        if (null != hikariConfigProperties.getDataSourceClassName()) {
            ds.setDataSourceClassName(hikariConfigProperties.getDataSourceClassName());
        }
        ds.setAutoCommit(hikariConfigProperties.isAutoCommit());
        ds.setPoolName(hikariConfigProperties.getPoolName());
        return ds;
    }

}
