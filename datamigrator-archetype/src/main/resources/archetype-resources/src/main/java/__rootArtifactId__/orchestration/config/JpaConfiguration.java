#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.${artifactId}.orchestration.config;

/*-
 * #%L
 * datamigrator-archetype
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

import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.configuration.PerDSJpaProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import net.osgiliath.migrator.core.configuration.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;

import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.*;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "${package}.${artifactId}.repository",
        entityManagerFactoryRef = JpaConfiguration.SINK_ENTITY_MANAGER_FACTORY,
        transactionManagerRef = SINK_TRANSACTION_MANAGER
)
public class JpaConfiguration {

    private final DataMigratorConfiguration dataMigratorConfiguration;
    public static final String SINK_ENTITY_MANAGER_FACTORY = "sinkEntityManagerFactory";
    public static final String SOURCE_ENTITY_MANAGER_FACTORY = "sourceEntityManagerFactory";

    public JpaConfiguration(DataMigratorConfiguration dataMigratorConfiguration) {
        super();
        this.dataMigratorConfiguration = dataMigratorConfiguration;
    }

    @Bean(SINK_ENTITY_MANAGER_FACTORY)
    @Primary
    public LocalContainerEntityManagerFactoryBean sinkEntityManagerFactory(
            @Qualifier(SINK_DATASOURCE) DataSource dataSource,
            EntityManagerFactoryBuilder builder, @Qualifier(SINK_JPA_PROPERTIES) PerDSJpaProperties jpaPropertiesWrapper) {
        return builder
                .dataSource(dataSource)
                .persistenceUnit(SINK_PU)
                .properties(jpaPropertiesWrapper.getProperties())
                .packages(dataMigratorConfiguration.getModelBasePackage())
                .build();
    }

    @Bean(SINK_TRANSACTION_MANAGER)
    @Primary
    public PlatformTransactionManager sinkTransactionManager(
            @Qualifier(SINK_ENTITY_MANAGER_FACTORY) LocalContainerEntityManagerFactoryBean sinkEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(sinkEntityManagerFactory.getObject()));
    }

    @Bean(SOURCE_ENTITY_MANAGER_FACTORY)
    public LocalContainerEntityManagerFactoryBean sourceEntityManagerFactory(
            @Qualifier(SOURCE_DATASOURCE) DataSource dataSource,
            EntityManagerFactoryBuilder builder, @Qualifier(SOURCE_JPA_PROPERTIES) PerDSJpaProperties jpaPropertiesWrapper) {
        return builder
                .dataSource(dataSource)
                .persistenceUnit(SOURCE_PU)
                .properties(jpaPropertiesWrapper.getProperties())
                .packages(dataMigratorConfiguration.getModelBasePackage())
                .build();
    }

    @Bean(DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER)
    public PlatformTransactionManager sourceTransactionManager(
            @Qualifier(SOURCE_ENTITY_MANAGER_FACTORY) LocalContainerEntityManagerFactoryBean sourceEntityManagerFactory
    ) {
        return new JpaTransactionManager(Objects.requireNonNull(sourceEntityManagerFactory.getObject()));
    }

}
