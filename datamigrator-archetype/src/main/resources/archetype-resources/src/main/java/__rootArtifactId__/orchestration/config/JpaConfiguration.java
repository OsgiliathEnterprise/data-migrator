#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.${artifactId}.orchestration.config;

import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.configuration.PerDSJpaProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
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

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
  basePackages = "${package}.${artifactId}.repository",
  entityManagerFactoryRef = "sinkEntityManagerFactory",
  transactionManagerRef = "sinkTransactionManager"
)
public class JpaConfiguration {

    private final DataMigratorConfiguration dataMigratorConfiguration;

    public JpaConfiguration(DataMigratorConfiguration dataMigratorConfiguration) {
        super();
        this.dataMigratorConfiguration = dataMigratorConfiguration;
    }

    @Bean("sinkEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean sinkEntityManagerFactory(
      @Qualifier("sinkDataSource") DataSource dataSource,
      EntityManagerFactoryBuilder builder, @Qualifier("sinkPerDsJpaProperties") PerDSJpaProperties jpaPropertiesWrapper) {
        return builder
            .dataSource(dataSource)
            .persistenceUnit("sink")
            .properties(jpaPropertiesWrapper.getProperties())
            .packages(dataMigratorConfiguration.getModelBasePackage())
            .build();
    }

    @Bean("sinkTransactionManager")
    @Primary
    public PlatformTransactionManager sinkTransactionManager(
        @Qualifier("sinkEntityManagerFactory") LocalContainerEntityManagerFactoryBean sinkEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(sinkEntityManagerFactory.getObject()));
    }

    @Bean("sourceEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean sourceEntityManagerFactory(
            @Qualifier("sourceDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder, @Qualifier("sourcePerDsJpaProperties")PerDSJpaProperties jpaPropertiesWrapper) {
        return builder
            .dataSource(dataSource)
            .persistenceUnit("source")
            .properties(jpaPropertiesWrapper.getProperties())
            .packages(dataMigratorConfiguration.getModelBasePackage())
            .build();
    }

    @Bean("sourceTransactionManager")
    public PlatformTransactionManager sourceTransactionManager(
        @Qualifier("sourceEntityManagerFactory") LocalContainerEntityManagerFactoryBean sourceEntityManagerFactory
    ) {
        return new JpaTransactionManager(Objects.requireNonNull(sourceEntityManagerFactory.getObject()));
    }

}
