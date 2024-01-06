# Welcome anonymous!

## Data Migrator tool

This tool aims to help the developer or business analyst to gather production data, setup a target database and schema (technology agnostic, source and target database technology may differ) then to execute an anonymization sequence: standard anonymisation (i.e. replacing column values with random values), custom scripted anonymisation (e.g. executing java business logic) and even manual customization.

## Procedure

1. Configure your machine prerequisites: java 21, docker
2. Start a container of your using your source database technology (optional if you have access to the remote database)
3. Import your production dump into your local database (optional if you have access to the remote database)
4. Create a new anonymization project for your context from the maven data-migrator maven archetype.
5. Configure the archetype pom.xml properties with your database information.
6. Generate the java entities, java tables metamodel and repositories on the `transformers` module using the `./mvnw clean process-classes -Pentities-from-source-schema` command from the transformer project: doing so, new classes will appear on the `/target/generated-sources`directory.
7. Start your target container (optional if you have access to the remote database)
8. Create your custom spring-data-repositories on the `transformers` module and create your custom anonymization service on the `transformers` module (optional if you use common modules that do ot need any customization.
9. Configure your automated anonymization sequence using the orchestration/src/main/resources/application.yml data-migrator.sequence property.
10. Start your target database container (optional if you have access to the remote database)
11. create the target schema by executing the `./mvnw clean process-classes -Pcreate-target-schema` command.

## Testing

In order to test, you'll need to setup the test source database, inject the data into it, then generate the entities, and finally run the tests that will inject the data into the target database.

### Running

The source and target database are started using test-containers when executing the integration tests (./mvnw verify) from within the `orchestration` module.
Still, as it will only succeed when the java beans (entities) are generated, you'll need to put a breakpoint in oe of the test methods and start the testsuite.
Once the breakpoint is hit, you should execute the `./mvnw clean process-classes -Pentities-from-source-schema` command from within the `transformers` module.

### Fix the transformer module reversed engineered entities

 Hibernates tools are quite good at reverse engineering entities, but are not perfect. You'll need to fix the entities manually. The most common issues are:
 - removing the `, nullable=false, updatable=false` Relationship annotation (a dirty search and replace will do the trick)
 - (optional) adding the mappedBy (ownig side id) annotation on `@ManyToMany` annotation to ensure reproducibility of the anonymization sequence (i.e. the same data will be generated for the same input data).
