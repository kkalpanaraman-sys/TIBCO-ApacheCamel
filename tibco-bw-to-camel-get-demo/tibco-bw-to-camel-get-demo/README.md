# TIBCO BW → Spring Boot + Apache Camel (HTTP GET demo)

A small **Java-only** portfolio project showing one narrow migration case:

**Source:** a simplified TIBCO BusinessWorks-style process that sends an HTTP `GET` request.  
**Target:** Java + Spring Boot + Apache Camel.

The Java generator reads the sample XML and generates a Camel `RouteBuilder`. This is a learning prototype, not a production-grade parser for arbitrary TIBCO `.process` files.

## Requirements
- JDK 17+
- Maven 3.9+

## Run the generator

```bash
mvn clean package
mvn exec:java -Dexec.mainClass="com.example.migration.BwToCamelGenerator" -Dexec.args="input/sample-bw-http-get.xml target/generated-sources/HttpGetRoute.java"
```

Generated source: `target/generated-sources/HttpGetRoute.java`

## Mapping in this demo

| TIBCO BW concept | Camel / Java |
|---|---|
| HTTP Client / Send HTTP Request | Camel HTTP endpoint |
| Method = GET | `Exchange.HTTP_METHOD = GET` |
| Destination URL | `toD("https://...")` |
| Process entry | `from("direct:...")` |
| HTTP response | Camel exchange body |

The generated class is intended to be copied into a Spring Boot application that has Apache Camel Spring Boot dependencies.

## Scope
Only one simple HTTP GET activity is supported. Real BW projects may also require shared resources, variables, mappings, schemas, credentials, TLS, retries, fault handlers, status-code handling, and deployment configuration. Review and test all generated code before using it in a real migration.
