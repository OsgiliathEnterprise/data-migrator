# Welcome anonymous!

## Data Migrator tool

This tool aims to help the developer or business analyst to gather production data from a source datasource of any vendor, create the same structure in target database  (same technology or other vendor or version, thanks to orm) then to execute the data transfer with a data value transformation sequence in between: standard anonymization (i.e. replacing column values with random values), custom scripted anonymization, e.g. executing tailor-made java custom business on top of spring data repositories and ai-driven or advanced customization.

## Procedure

1. Configure your machine prerequisites: java 17+ (or 21), docker and maven. 
2. Start a container of your using your source database technology (optional if you have access to the remote database)
3. Import your production dump into your local database (optional if you have access to the remote database)
4. Create a new anonymization project for your context from the maven data-migrator maven archetype (`mvn archetype:generate -DarchetypeGroupId=net.osgiliath.datamigrator.archetype -DarchetypeArtifactId=datamigrator-archetype -DarchetypeVersion=<current version of the archetype>`), ensure to choose a name without special characters (i.e. myanonymizationproject, without '-', '_', ...).
5. Configure the project `database.properties` with your database information.
6. Generate the java entities, java tables metamodel and repositories from the root of your generated project using the `./mvnw clean process-classes -Pentities-from-source-schema` command: doing so, new classes will appear on the `/target/generated-sources`directory.
7. Create your custom business logic (java code) on top of the spring-data-repositories and queries (optional if you use common modules that do ot need any customization).
8. Configure your anonymization sequence using the `src/main/resources/application.yml`  property file with data-migrator sequence and sequencers.
9. Start your target database container (optional if you have access to the remote database)
10. create the target schema by executing the `./mvnw clean process-classes -Pcreate-target-schema` command.

## Additional useful commands

* `./mvnw process-classes -Ptarget-data-changelog-target-db` a liquibase schema changelog (including data) from target database for manual processing (unfortunately, the target/classes/config/liquibase-sink/changelog will have to be modified manually to remove the absolute path part of the <loadData/> sections)
* `./mvnw process-classes -Ptarget-db-from-target-data-changelog -Dliquibase.duplicateFileMode=WARN` a liquibase schema changelog (including data) from target database for manual processing

* `./mvnw clean verify -Pdata-changelog-from-source-db` to generate a liquibase changelog from source DATA
* `./mvnw clean verify -Pentities-from-changelog` to generate a source database and entities from a liquibase changelog
* `./mvnw clean verify -Pschema-changelog-from-source-db` to generate a liquibase changelog from source SCHEMA

## Customization of the anonymization

### Creating your own custom business logic

You can create your own `sequencer` by extending `MetamodelColumnCellTransformer` (cell level) or `JpaEntityColumnTransformer` (entity level) and adding it to the `application.yml` file.

For sequencers that needs some context (they can't be singleton beans), you'll need to create a `FactorySequencer` Factory bean that will create the sequencer instance.

### Fix the generated java beans on reversed engineered entities

 Hibernates tools are quite good at reverse engineering entities, but are not perfect. You'll need to fix the entities manually. The most common issues are:
 - (optional) adding the mappedBy (ownig side id) annotation on `@ManyToMany` annotation to ensure reproducibility of the anonymization sequence (i.e. the same data will be generated for the same input data).
 - (optional) add `@Basic(fetch = FetchType.LAZY)` on blobs and clobs attributes (to avoid heavy memory overloading.

## Integration testing for your anonymization sequence

In order to try this project, the simplest procedure is to setup a source database, inject the data into it, then generate the entities, and finally run the tests that will inject the data into the target database.
the sample-mono project illustrates this procedure (see `pom.xml` test configuration, as well as the test classes).

### Running the test (see sample-mono)

The source and target database are started using test-containers when executing the integration tests (`./mvnw clean verify -Pentities-from-changelog`).
