package net.osgiliath;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import net.osgiliath.datamigrator.sample.domain.Employee;
import net.osgiliath.datamigrator.sample.domain.Job;
import net.osgiliath.datamigrator.sample.repository.EmployeeRepository;
import net.osgiliath.datamigrator.sample.repository.JobRepository;
import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.db.inject.SinkEntityInjector;
import net.osgiliath.migrator.core.graph.TinkerpopModelGraphBuilder;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphBuilder;
import net.osgiliath.migrator.sample.orchestration.DataMigratorApplication;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;

import static net.osgiliath.Constants.mysqlTimeoutInSecond;
import static net.osgiliath.Constants.mysqlVersion;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = {DataMigratorApplication.class})
@DirtiesContext
class InjectionIT {
    static {
        System.setProperty("liquibase.duplicateFileMode", "WARN");
    }

    private static final Logger logger = LoggerFactory.getLogger(InjectionIT.class);

    @Container
    static MySQLContainer mySQLSourceContainer = (MySQLContainer) new MySQLContainer(DockerImageName.parse("mysql:" + mysqlVersion))
            .withConnectTimeoutSeconds(mysqlTimeoutInSecond.intValue());
    ;
    // .withExposedPorts(64449);

    @Container
    static MySQLContainer mySQLTargetContainer = (MySQLContainer) new MySQLContainer(DockerImageName.parse("mysql:" + mysqlVersion))
            .withConnectTimeoutSeconds(mysqlTimeoutInSecond.intValue());

    @DynamicPropertySource
    static void mySQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.source.url", mySQLSourceContainer::getJdbcUrl);
        registry.add("spring.datasource.source.username", mySQLSourceContainer::getUsername);
        registry.add("spring.datasource.source.password", mySQLSourceContainer::getPassword);
        registry.add("spring.datasource.source.driver-class-name", mySQLSourceContainer::getDriverClassName);
        registry.add("spring.datasource.source.type", () -> "com.zaxxer.hikari.HikariDataSource");
        registry.add("spring.datasource.source.hikari.poolName", () -> "sourceHikari");
        registry.add("spring.datasource.source.hikari.auto-commit", () -> false);

        registry.add("spring.datasource.sink.url", mySQLTargetContainer::getJdbcUrl);
        registry.add("spring.datasource.sink.username", mySQLTargetContainer::getUsername);
        registry.add("spring.datasource.sink.password", mySQLTargetContainer::getPassword);
        registry.add("spring.datasource.sink.driver-class-name", mySQLTargetContainer::getDriverClassName);
        registry.add("spring.datasource.sink.type", () -> "com.zaxxer.hikari.HikariDataSource");
        registry.add("spring.datasource.sink.hikari.poolName", () -> "sinkHikari");
        registry.add("spring.datasource.sink.hikari.auto-commit", () -> false);
        DataSource ds = DataSourceBuilder.create()
                .url(mySQLSourceContainer.getJdbcUrl())
                .username(mySQLSourceContainer.getUsername())
                .password(mySQLSourceContainer.getPassword())
                .driverClassName(mySQLSourceContainer.getDriverClassName())
                .build();
        try {
            logger.warn("Starting Liquibase import");
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setChangeLog("classpath:/config/liquibase/master.xml");
            liquibase.setContexts("test,faker");
            liquibase.setDataSource(ds);
            liquibase.afterPropertiesSet();
        } catch (LiquibaseException e) {
            logger.error("Failed to import liquibase datasource", e.getMessage(), e);
        }
    }

    @Autowired
    private MetamodelGraphBuilder metamodelGraphBuilder;

    @Autowired
    private TinkerpopModelGraphBuilder modelGraphBuilder;

    @Autowired
    private MetamodelScanner scanner;

    @Autowired
    private SinkEntityInjector sinkEntityInjector;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void givenFedGraphWhenEntityProcessorIsCalledThenTargetDatabaseIsPopulatedExcludingCyclicPath() throws Exception {
        Collection<Class<?>> metamodelClasses = scanner.scanMetamodelClasses();
        Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> entityMetamodelGraph = metamodelGraphBuilder.metamodelGraphFromRawElementClasses(metamodelClasses);
        try (GraphTraversalSource modelGraph = modelGraphBuilder.modelGraphFromMetamodelGraph(entityMetamodelGraph)) {
            sinkEntityInjector.persist(modelGraph, entityMetamodelGraph);
            List<Employee> employees = employeeRepository.findAll();
            assertThat(employees).hasSize(12); // One employee has a cycle
            List<Job> jobs = jobRepository.findAll();
            assertThat(jobs).hasSize(10); // has not been limited because intermediairy employee should have been removed

        }
    }

}
