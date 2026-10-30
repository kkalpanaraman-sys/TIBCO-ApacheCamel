# TIBCO BW → Spring Boot + Apache Camel Migration Assistant

A Java-only portfolio prototype that accepts either an exported project ZIP or an extracted project folder. It inventories files, inspects simplified process/connection/mapping XML, generates Java model classes from simple XSD definitions, and generates a runnable Spring Boot + Apache Camel example for the included HTTP GET flow.

## What this version demonstrates

<<<<<<< HEAD
1. ZIP or folder ingestion (with ZIP path traversal protection).
2. Basic XML inventory and process activity / endpoint clues.
3. Basic shared connection configuration inspection.
4. Simple XSD-to-Java model generation.
5. Illustrative mapping field inventory.
6. **End-to-end target-code generation:** Spring Boot application, Camel HTTP GET route, Java response mapper and a generated service README.

## Honest scope
=======
## Important scope:

This is a learning prototype using a **synthetic BW-style project**.
>>>>>>> d12e8813d843bb8c8e9848221777cf480f7ebc7d

The included project is **synthetic BW-style XML**, not a confidential or native TIBCO export. The generator creates a working target-side example for the known sample flow:

<<<<<<< HEAD
`HTTP GET → map orderId/status/total → return OrderView JSON`

The mapping rules in `OrderMapper.xml` are illustrative (`copy`, `upper-case`, `decimal-copy`). The generated service expects an upstream JSON response containing `orderId`, `status`, and `total`.

 The generated Camel route is a target template based on the sample flow. Do not describe this prototype as the employer's internal AI tool or as production-ready migration automation, Just a mock AI tool
=======

>>>>>>> d12e8813d843bb8c8e9848221777cf480f7ebc7d

## Requirements

- JDK 17+
- Maven 3.9+
- Internet access on first Maven build to download dependencies for the generated service

## Build the migration assistant

```bash
mvn clean package
```

## Run against the included sample ZIP

```bash
mvn exec:java -Dexec.mainClass="com.example.migration.MigrationAssistant" -Dexec.args="--zip input/sample-bw-project.zip --out target/migration-output"
```

Or use an extracted folder:

```bash
mvn exec:java -Dexec.mainClass="com.example.migration.MigrationAssistant" -Dexec.args="--folder input/sample-bw-project --out target/migration-output"
```

## Generated output

- `target/migration-output/project-analysis.md`
- `target/migration-output/java-models/*.java`
- `target/migration-output/generated-service/` — standalone Spring Boot + Apache Camel Maven project

## Run the generated service

Start an upstream test API that responds to `GET /orders/{id}` with JSON such as:

```json
{"orderId":"1001","status":"processing","total":49.95}
```

Then, from the generated service directory:

```bash
# Optional: point to your upstream API (default http://localhost:9090)
export ORDERS_API_BASE_URL=http://localhost:9090
mvn spring-boot:run
```

Call:

```bash
curl http://localhost:8080/orders/1001
```

Expected response shape:

```json
{"orderReference":"1001","orderStatus":"PROCESSING","totalAmount":49.95}
```

The generated route uses Apache Camel Platform HTTP for the inbound REST endpoint and Camel HTTP for the outbound GET. Review TLS, authentication, timeouts, retries, error handling, logging and data validation before any production use.

## Next engineering milestones

- Add version-specific BW export adapters using non-confidential sample exports.
- Parse native Mapper expressions and generate equivalent transformations.
- Resolve process references and shared connection resources.
- Add generated-service integration tests with a mock upstream API.
- Add support for more activity types (SOAP, JMS/EMS, SFTP).
- Add an optional AI layer for assisted analysis/code suggestions with structured output, developer review and validation gates.

Never upload confidential employer project exports, credentials, certificates or customer data to a public repository.
