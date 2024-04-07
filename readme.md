![tests and snapshot deployment](https://github.com/OsgiliathEnterprise/data-migrator/actions/workflows/maven.yml/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/net.osgiliath.datamigrator/data-migrator)

# Welcome anonymous!

## Data Migrator tool

This tool aims to help the developer or business analyst to gather production data from a source datasource of any
vendor, create the same structure in target database  (same technology or other vendor or version, thanks to orm) then
to execute the data transfer with a data value transformation sequence in between: standard anonymization (i.e.
replacing column values with random values), or custom scripted anonymization, e.g. executing tailor-made java custom
business on top of spring data repositories or Tinkerpop's graph queries and even ai-driven.

## Procedure

1. Configure your machine prerequisites: java 17+ (or 21), docker and maven.
2. Start a container of your using your source database technology (optional if you have access to the remote database)
3. Import your production dump into your local database (optional if you have access to the remote database)
4. Create a new anonymization project for your context from the maven data-migrator maven
   archetype (`mvn archetype:generate -DarchetypeGroupId=net.osgiliath.datamigrator.archetype -DarchetypeArtifactId=datamigrator-archetype -DarchetypeVersion=<current version of the archetype>`),
   ensure to choose a name without special characters (i.e. myanonymizationproject, without '-', '_', ...).
5. Configure the project `database.properties` with your databases information.
6. Generate the java entities, java tables metamodel and repositories from the root of your generated project using
   the `./mvnw clean process-classes -Pentities-from-source-schema` command: doing so, new classes will appear on
   the `/target/generated-sources`directory.
7. Create your custom business logic (java code) on top of the spring-data-repositories and queries (optional if you use
   common modules that do ot need any customization).
8. Configure your anonymization sequence using the `src/main/resources/application.yml`  property file with
   data-migrator sequence and sequencers.
9. Start your target database container (optional if you have access to the remote database).
10. Launch the sequencing: `./mvnw package && cd target && java -jar <yourjar>.jar`.

## Additional useful commands

* `./mvnw process-classes -Ptarget-data-changelog-target-db` a liquibase schema changelog (including data) from target
  database for manual processing (unfortunately, the target/classes/config/liquibase-sink/changelog will have to be
  modified manually to remove the absolute path part of the <loadData/> sections, i.e. `/config/liquibase-sink/data`,
  and a master.xml file has to be
  created referencing the changelog file)
* `./mvnw process-classes -Ptarget-db-from-target-data-changelog -Dliquibase.duplicateFileMode=WARN` a liquibase schema
  changelog (including data) from target database for manual processing

* `./mvnw clean verify -Pdata-changelog-from-source-db` to generate a liquibase changelog from source DATA
* `./mvnw clean verify -Pentities-from-changelog` to generate a source database and entities from a liquibase changelog
* `./mvnw clean verify -Pschema-changelog-from-source-db` to generate a liquibase changelog from source SCHEMA

## Customization of the anonymization

### Creating your own custom business logic

You can create your own `sequencer` by extending `MetamodelColumnCellTransformer` (cell level)
or `JpaEntityColumnTransformer` (entity level) and adding it to the `application.yml` file.

For sequencers that needs some context (they can't be singleton beans), you'll need to create a `FactorySequencer`
Factory bean that will create the sequencer instance.

### Fix the generated java beans on reversed engineered entities

Hibernates tools are quite good at reverse engineering entities, but are not perfect. You'll need to fix the entities
manually. The most common issues are:

- (optional) adding the mappedBy (ownig side id) annotation on `@ManyToMany` annotation to ensure reproducibility of the
  anonymization sequence (i.e. the same data will be generated for the same input data).
- (optional) add `@Basic(fetch = FetchType.LAZY)` on blobs and clobs attributes (to avoid heavy memory overloading.
- Remove `@Version` annotation if it's not an hibernate version column (i.e. it's a business version column)
- Escape the column names that are reserved words in the target database (e.g. ` @Column(name = "\"schema\"")` in
  postgresql)

## Integration testing for your anonymization sequence

In order to try this project, the simplest procedure is to setup a source database, inject the data into it, then
generate the entities, and finally run the tests that will inject the data into the target database.
the sample-mono project illustrates this procedure (see `pom.xml` test configuration, as well as the test classes).

### Running the test (see sample-mono)

The source and target database are started using test-containers when executing the integration
tests (`./mvnw clean verify -Pentities-from-changelog`).

# Standard anonymization

## Fake data generation

Here's how you can configure the fake data generation (will replace all the values of the column with fake data):

```yaml
    - name: column-anonymize-1
      type: factory
      transformer-class: net.osgiliath.migrator.modules.faker.ColumnFaker
      entity-class: Employee
      column-transformation-definitions:
        - column-name: firstName
          consistent-key: True # will reuse value of a previously faked entry
          options:
            - faker: dragon_ball.characters

```

There are a lot of options for the faker, see
the [faker documentation](https://www.datafaker.net/documentation/getting-started/).

## Row limiting

You can limit the number of rows to be injected in the target database using the `row-limit` sequencer:

```yaml
    - name: rows-minimize-1
      type: bean
      transformer-class: net.osgiliath.migrator.modules.rowlimiter.RowLimiter
      sequencer-options:
        rows-to-keep: 3
```

# TIPS & TRICKS

## Generated entities

This framework heavily relies on Hibernate tools for entity retro-engineering. This tool has some weird behavior when
It sometimes generate `Clob` type instead of a standard String: prefer replacing the generated entity's field and
methods with the
proper [`@JdbcTypeCode`](https://docs.jboss.org/hibernate/stable/orm/userguide/html_single/Hibernate_User_Guide.html#basic-String)
