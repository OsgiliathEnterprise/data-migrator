package net.osgiliath;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import net.osgiliath.datamigrator.sample.domain.*;
import net.osgiliath.migrator.core.api.metamodel.MetamodelScanner;
import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.metamodel.impl.MetamodelGraphBuilder;
import net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.sample.orchestration.DataMigratorApplication;
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
import java.util.Collection;
import java.util.stream.Collectors;

import static net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY;
import static net.osgiliath.migrator.core.modelgraph.ModelGraphBuilder.MODEL_GRAPH_VERTEX_ENTITY_ID;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = { DataMigratorApplication.class })
class ModelT {
    static {
      System.setProperty("liquibase.duplicateFileMode", "WARN");
    }
    private static final Logger logger = LoggerFactory.getLogger(ModelT.class);

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
    private ModelGraphBuilder modelGraphBuilder;

    @Autowired
    private MetamodelScanner scanner;

    @Test
    void givenFedModelWhenGraphBuilderIsCalledThenGraphModelVerticesArePopulated() throws Exception {
        Collection<Class<?>> metamodelClasses = scanner.scanMetamodelClasses();
        Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph = metamodelGraphBuilder.metamodelGraphFromEntityMetamodel(metamodelClasses);
        try (GraphTraversalSource modelGraph = modelGraphBuilder.modelGraphFromMetamodelGraph(entityMetamodelGraph)) {
            assertThat(modelGraph).isNotNull();
            assertThat(modelGraph.V().hasLabel(Country.class.getSimpleName()).toList()).hasSize(10);
            assertThat(modelGraph.V().hasLabel(Department.class.getSimpleName()).toList()).hasSize(10);
            assertThat(modelGraph.V().hasLabel(Employee.class.getSimpleName()).toList()).hasSize(10);
            assertThat(modelGraph.V().hasLabel(Job.class.getSimpleName()).toList()).hasSize(10);
            assertThat(modelGraph.V().hasLabel(JobHistory.class.getSimpleName()).toList()).hasSize(10);
            assertThat(modelGraph.V().hasLabel(Location.class.getSimpleName()).toList()).hasSize(10);
            assertThat(modelGraph.V().hasLabel(Region.class.getSimpleName()).toList()).hasSize(10);
            assertThat(modelGraph.V().hasLabel(Task.class.getSimpleName()).toList()).hasSize(10);
            assertThat(modelGraph.V().hasLabel(JhiUser.class.getSimpleName()).toList()).hasSize(2);
            assertThat(modelGraph.V().hasLabel(JhiAuthority.class.getSimpleName()).toList()).hasSize(2);
        }
    }

    @Test
    void givenFedModelWhenGraphBuilderIsCalledThenGraphModelEdgesArePopulated() throws Exception {
        Collection<Class<?>> metamodelClasses = scanner.scanMetamodelClasses();
        Graph<MetamodelVertex, FieldEdge> entityMetamodelGraph = metamodelGraphBuilder.metamodelGraphFromEntityMetamodel(metamodelClasses);
        try (GraphTraversalSource modelGraph = modelGraphBuilder.modelGraphFromMetamodelGraph(entityMetamodelGraph)) {
            assertThat(modelGraph).isNotNull();
            assertThat(modelGraph.V().hasLabel(Employee.class.getSimpleName()).has(MODEL_GRAPH_VERTEX_ENTITY_ID,1).out(Employee_.EMPLOYEE).toList()).hasSize(1);
            assertThat(((Employee)((ModelElement)modelGraph.V().hasLabel(Employee.class.getSimpleName()).has(MODEL_GRAPH_VERTEX_ENTITY_ID,1).out(Employee_.EMPLOYEE).values(MODEL_GRAPH_VERTEX_ENTITY).next()).getEntity()).getFirstName()).isEqualTo("Horace");

//            assertThat(modelGraph.V().hasLabel(JhiUser.class.getSimpleName()).has(MODEL_GRAPH_VERTEX_ENTITY_ID,1).out(JhiUser_.JHI_AUTHORITIES).toList()).hasSize(2);
//            assertThat(modelGraph.V().hasLabel(JhiUser.class.getSimpleName()).has(MODEL_GRAPH_VERTEX_ENTITY_ID,1).out(JhiUser_.JHI_AUTHORITIES).values(MODEL_GRAPH_VERTEX_ENTITY).toList().stream().map(a -> ((JhiAuthority)a).getName()).collect(Collectors.toSet())).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");

            assertThat(modelGraph.V().hasLabel(Employee.class.getSimpleName()).has(MODEL_GRAPH_VERTEX_ENTITY_ID,1).in(Job_.EMPLOYEE).toList()).hasSize(4);
            assertThat(modelGraph.V().hasLabel(Employee.class.getSimpleName()).has(MODEL_GRAPH_VERTEX_ENTITY_ID,1).in(Job_.EMPLOYEE).values(MODEL_GRAPH_VERTEX_ENTITY).toList().stream().map(me -> ((ModelElement)me).getEntity()).map(a -> ((Job)a).getJobTitle()).collect(Collectors.toSet())).containsExactlyInAnyOrder("Future Group Manager", "National Markets Producer", "Investor Communications Administrator", "Corporate Markets Director");
        }
    }
}
