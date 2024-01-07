# Welcome anonymous!

## Data Migrator tool

This tool aims to help the developer or business analyst to gather production data, setup a target database and schema (technology agnostic, source and target database technology may differ) then to execute an anonymization sequence: standard anonymization (i.e. replacing column values with random values), custom scripted anonymization (e.g. executing java business logic) and even manual customization.

## Procedure

1. Configure your machine prerequisites: java 21, docker
2. Start a container of your using your source database technology (optional if you have access to the remote database)
3. Import your production dump (at least the tables and foreign keys) into your local database (optional if you have access to the remote database)
4. Create a new anonymization project for your context from the maven data-migrator maven archetype (at first, copy the sample project).
5. Configure the archetype database.properties with your database information.
6. Generate the java entities, java tables metamodel and repositories on the using the `./mvnw clean process-classes -Pentities-from-source-db` command from the transformer project: doing so, new classes will appear on the `/target/generated-sources`directory.
7. Start your target container (optional if you have access to the remote database)
8. Create your custom `CellTransformer` classes (optional if you use common modules that do ot need any customization).
9. Configure your automated anonymization sequence using the orchestration/src/main/resources/application.yml data-migrator.sequence property and reference your `CellTransformer` classes in the data-migrator.sequencer section.
10. Start your target database container (optional if you have access to the remote database)
11. Execute the transformation sequence that will create the target schema and migrated data by executing the `./mvnw clean verify` command.

## Testing

In order to test, you'll need to setup the test source database, inject the data into it, then generate the entities, and finally run the tests that will inject the data into the target database. A well configured  `entities-from-changelog` maven profile can help you to do so.

### Running

The source and target database are started using test-containers when executing the integration tests (./mvnw verify) from within the `orchestration` module.
Still, as it will only succeed when the java beans (entities) are generated, you'll need to put a breakpoint in oe of the test methods and start the testsuite.
Once the breakpoint is hit, you should execute the `./mvnw clean process-classes -Pentities-from-source-schema` command from within the `transformers` module.

### Tweak the reversed engineered entities (optional)

 Hibernates tools are quite good at reverse engineering entities, but can be tweaked to better fit your needs. For instance, you may want to:
 - (optional) adding the mappedBy (to owning side id) annotation on `@ManyToMany` annotation to ensure reproducibility of the anonymization sequence (i.e. the same data will be generated for the same input data).
 - Add `@Basic(fetch = FetchType.LAZY)` on fields that are too big to be loaded in memory (e.g. `@Lob` fields). 
