# TIBCO BW Project → Spring Boot + Apache Camel Migration Assistant

A Java-only portfolio prototype that accepts either:
- an exported TIBCO BW project ZIP archive, or
- an extracted TIBCO BW project folder.

It scans project files, inventories process/schema/mapping/resource files, extracts simple HTTP endpoint and activity information from XML, generates Java model classes from XSD, and creates an analysis report.

## Important scope and honesty note

This is a learning prototype using a **synthetic BW-style project**. TIBCO BW exports differ by version and project configuration. The scanner uses generic XML discovery and deliberately limited heuristics; it is not a complete parser for every BW 5.x / BW 6.x export. It does not claim to reproduce any employer's internal AI tool.

The current version demonstrates the baseline engineering stages:
1. project ingestion (ZIP or folder),
2. project inventory,
3. XML process/activity and endpoint inspection,
4. XSD-to-Java model generation,
5. a mapping inventory and migration report.

The sample Mapper file is synthetic. Full BW Mapper expression conversion and AI-assisted code generation are future extensions, not implemented capabilities in this version.

## Requirements
- JDK 17+
- Maven 3.9+

## Build

```bash
mvn clean package
```

## Analyse an exported project ZIP

```bash
mvn exec:java -Dexec.mainClass="com.example.migration.MigrationAssistant" -Dexec.args="--zip input/sample-bw-project.zip --out target/migration-output"
```

## Analyse an extracted project folder

```bash
mvn exec:java -Dexec.mainClass="com.example.migration.MigrationAssistant" -Dexec.args="--folder input/sample-bw-project --out target/migration-output"
```

Generated files:
- `target/migration-output/project-analysis.md`
- `target/migration-output/java-models/*.java`

## Synthetic sample project

`input/sample-bw-project/` contains:
- `process/OrderLookup.process` — simplified process XML
- `schema/OrderRequest.xsd` — input schema
- `schema/OrderResponse.xsd` — output schema
- `mapping/OrderMapper.xml` — illustrative source-to-target field mappings
- `shared/HttpConnection.xml` — illustrative endpoint/protocol configuration

A ZIP of the same sample project is also included at `input/sample-bw-project.zip`.

## Current limitations / next milestones

- Add version-specific BW export adapters after inspecting real, non-confidential sample exports.
- Resolve BW shared resources and process references more accurately.
- Parse real Mapper/XPath/XSLT expressions and generate equivalent Java transformations.
- Generate Camel routes for HTTP, SOAP, JMS/EMS and SFTP based on verified activity/configuration metadata.
- Add generated-code compilation and tests.
- Add an optional LLM layer with prompt templates, structured outputs, review gates and human approval.

Never upload confidential employer project exports or credentials to a public repository.
