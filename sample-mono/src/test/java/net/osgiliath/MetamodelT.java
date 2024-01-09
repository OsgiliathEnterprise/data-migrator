package net.osgiliath;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import net.osgiliath.domain.*;
import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.sourcedb.EntityImporter;
import net.osgiliath.migrator.core.db.inject.SinkEntityInjector;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphBuilder;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphRequester;
import net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder;
import net.osgiliath.migrator.core.modelgraph.model.ModelElement;
import net.osgiliath.migrator.core.processing.SequenceProcessor;
import net.osgiliath.migrator.sample.orchestration.DataMigratorApplication;
import net.osgiliath.repository.CountryRepository;
import net.osgiliath.repository.EmployeeRepository;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY;
import static net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = { DataMigratorApplication.class })
class MetamodelT {
    static {
      System.setProperty("liquibase.duplicateFileMode", "WARN");
    }
    private static final Logger logger = LoggerFactory.getLogger(MetamodelT.class);

    @Container
    static MySQLContainer mySQLSourceContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));
            // .withExposedPorts(64449);

    @Container
    static MySQLContainer mySQLTargetContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

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

        DataSource  ds = DataSourceBuilder.create()
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
    private MetamodelGraphRequester graphRequester;

    @Test
    void givenHrMetaClassesWhenMetamodelScannerScanIsCalledThenMetaclassesAreRetreived() {
        Collection<Class<?>> metamodelClasses = scanner.scanMetamodelClasses();
        assertThat(metamodelClasses).isNotEmpty();
        assertThat(metamodelClasses).contains(
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
        Graph<MetamodelVertex, FieldEdge> graph = metamodelGraphBuilder.metamodelGraphFromEntityMetamodel(metamodelClasses);
        graphRequester.displayGraphWithGraphiz(graph);
        assertThat(graph).isNotNull();
        assertThat(graph.vertexSet()).hasSize(11);
        assertThat(graph.edgeSet()).hasSize(11);
    }

    @Test
    void givenFedModelWhenEntityImporterIsCalledThenEntityResultSetIsNotEmpty() {
        Collection<Class<?>> metamodelClasses = scanner.scanMetamodelClasses();
        Graph<MetamodelVertex, FieldEdge> graph = metamodelGraphBuilder.metamodelGraphFromEntityMetamodel(metamodelClasses);
        MetamodelVertex entityVertex = graph.vertexSet().stream().filter(v -> v.getTypeName().equals("Employee")).findFirst().get();
        List<ModelElement> entities = entityImporter.importEntities(entityVertex, new ArrayList<>());
        assertThat(entities).isNotEmpty();
    }
}
