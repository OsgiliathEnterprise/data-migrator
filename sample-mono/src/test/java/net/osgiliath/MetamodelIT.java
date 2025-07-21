package net.osgiliath;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import net.osgiliath.datamigrator.sample.domain.*;
import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphBuilder;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelRequester;
import net.osgiliath.migrator.sample.orchestration.DataMigratorApplication;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static net.osgiliath.Constants.mysqlTimeoutInSecond;
import static net.osgiliath.Constants.mysqlVersion;
import static net.osgiliath.migrator.core.configuration.DataSourceConfiguration.SOURCE_TRANSACTION_MANAGER;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = {DataMigratorApplication.class})
@DirtiesContext
class MetamodelIT {
    static {
        System.setProperty("liquibase.duplicateFileMode", "WARN");
    }

    private static final Logger logger = LoggerFactory.getLogger(MetamodelIT.class);

    @Container
    static MySQLContainer mySQLSourceContainer = (MySQLContainer) new MySQLContainer(DockerImageName.parse("mysql:" + mysqlVersion)).withConnectTimeoutSeconds(mysqlTimeoutInSecond.intValue());
    // .withExposedPorts(64449);

    @Container
    static MySQLContainer mySQLTargetContainer = (MySQLContainer) new MySQLContainer(DockerImageName.parse("mysql:" + mysqlVersion)).withConnectTimeoutSeconds(mysqlTimeoutInSecond.intValue());

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
    private EntityImporter entityImporter;

    @Autowired
    private MetamodelScanner scanner;

    @Autowired
    private MetamodelRequester graphRequester;

    @Test
    void givenHrMetaClassesWhenMetamodelScannerScanIsCalledThenMetaclassesAreRetreived() {
        Collection<Class<?>> metamodelClasses = scanner.scanMetamodelClasses();
        assertThat(metamodelClasses).isNotEmpty().contains(
                Country_.class,
                Department_.class,
                Employee_.class,
                Job_.class,
                JobHistory_.class,
                Location_.class,
                Region_.class
        );
    }

    @Test
    void givenHrMetaClassesAndEntitiesWhenGraphBuilderIsCalledThenGraphIsBuilt() {
        Collection<Class<?>> metamodelClasses = scanner.scanMetamodelClasses();
        Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> graph = metamodelGraphBuilder.metamodelGraphFromRawElementClasses(metamodelClasses);
        graphRequester.displayGraphWithGraphiz(graph);
        assertThat(graph).isNotNull();
        assertThat(graph.vertexSet()).hasSize(11);
        assertThat(graph.edgeSet()).hasSize(12);
    }

    @Test
    void givenHrMetaClassesAndEntitiesWhenMetamodelGraphClustererIsCalledThenGraphClusterIsBuilt() {
        Collection<Class<?>> metamodelClasses = scanner.scanMetamodelClasses();
        Graph<MetamodelVertex, FieldEdge<MetamodelVertex>> graph = metamodelGraphBuilder.metamodelGraphFromRawElementClasses(metamodelClasses);
        Collection<Graph<MetamodelVertex, FieldEdge<MetamodelVertex>>> clusteredEntityMetamodelGraph = metamodelGraphBuilder.clusterGraphs(graph);
        assertThat(clusteredEntityMetamodelGraph.size()).isEqualTo(3);
    }

    @Transactional(transactionManager = SOURCE_TRANSACTION_MANAGER, readOnly = true)
    @Test
    public void givenFedModelWhenEntityImporterIsCalledThenEntityResultSetIsNotEmpty() {
        Collection<Class<?>> metamodelClasses = scanner.scanMetamodelClasses();
        Graph<MetamodelVertex, FieldEdge> graph = metamodelGraphBuilder.metamodelGraphFromRawElementClasses(metamodelClasses);
        MetamodelVertex entityVertex = graph.vertexSet().stream().filter(v -> v.getTypeName().equals(Employee.class.getName())).findFirst().get();
        List<ModelElement> entities = entityImporter.importEntities(entityVertex, new ArrayList<>()).collect(Collectors.toUnmodifiableList());
        assertThat(entities).isNotEmpty();
    }
}
