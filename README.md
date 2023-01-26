# yti-datamodel-api

Interoperability workbench data model editor backend

## Requirements
- Java 17
- Docker
- Gradle 7.6

## Development

### Checkout library projects
- yti-spring-migration
- yti-spring-security

Checkout corresponding tag for version defined in build.gradle, e.g. `git checkout v0.2.0`
Install package to local maven repository `./gradlew publishToMavenLocal`

### Checkout other required projects
- yti-groupmanagement
- yti-fuseki

For each project run `build.sh` located at the project root folder. The script builds a docker image for the project.

### Checkout yti-compose project

Start dependant containers
```
docker-compose up -d yti-groupmanagement yti-fuseki yti-datamodel-elasticsearch
```

### Populate data to the database
```
./src/scripts/init.admin.sh
```

## API docs
http://localhost:9004/datamodel-api/swagger-ui/index.html
