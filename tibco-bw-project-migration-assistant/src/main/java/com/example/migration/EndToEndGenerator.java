package com.example.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Generates a runnable Spring Boot + Camel example for the repository's synthetic HTTP GET flow. */
final class EndToEndGenerator {
    private EndToEndGenerator() {}

    static void generate(Path root, List<String> report) throws IOException {
        write(root, "pom.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example.generated</groupId><artifactId>order-lookup-service</artifactId><version>1.0.0</version>
                  <parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>3.3.5</version><relativePath/></parent>
                  <properties><java.version>17</java.version><camel.version>4.8.0</camel.version></properties>
                  <dependencyManagement><dependencies><dependency><groupId>org.apache.camel.springboot</groupId><artifactId>camel-spring-boot-bom</artifactId><version>${camel.version}</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>
                  <dependencies>
                    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId></dependency>
                    <dependency><groupId>org.apache.camel.springboot</groupId><artifactId>camel-spring-boot-starter</artifactId></dependency>
                    <dependency><groupId>org.apache.camel.springboot</groupId><artifactId>camel-platform-http-starter</artifactId></dependency>
                    <dependency><groupId>org.apache.camel.springboot</groupId><artifactId>camel-http-starter</artifactId></dependency>
                    <dependency><groupId>org.apache.camel.springboot</groupId><artifactId>camel-jackson-starter</artifactId></dependency>
                    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
                  </dependencies>
                  <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
                </project>
                """);
        write(root, "src/main/java/com/example/generated/OrderLookupApplication.java", """
                package com.example.generated;
                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                @SpringBootApplication
                public class OrderLookupApplication {
                    public static void main(String[] args) { SpringApplication.run(OrderLookupApplication.class, args); }
                }
                """);
        write(root, "src/main/java/com/example/generated/OrderView.java", """
                package com.example.generated;
                import java.math.BigDecimal;
                public record OrderView(String orderReference, String orderStatus, BigDecimal totalAmount) {}
                """);
        write(root, "src/main/java/com/example/generated/OrderResponseMapper.java", """
                package com.example.generated;
                import java.math.BigDecimal;
                import java.util.Map;
                import org.springframework.stereotype.Component;
                @Component
                public class OrderResponseMapper {
                    // Generated from the illustrative OrderMapper.xml rules:
                    // orderId -> orderReference (copy)
                    // status -> orderStatus (upper-case)
                    // total -> totalAmount (decimal-copy)
                    public OrderView map(Map<String, Object> source) {
                        String id = stringValue(source.get("orderId"));
                        String status = stringValue(source.get("status"));
                        BigDecimal total = new BigDecimal(stringValue(source.get("total")));
                        return new OrderView(id, status == null ? null : status.toUpperCase(), total);
                    }
                    private String stringValue(Object value) {
                        if (value == null) throw new IllegalArgumentException("Required mapping value is missing");
                        return value.toString();
                    }
                }
                """);
        write(root, "src/main/java/com/example/generated/OrderLookupRoute.java", """
                package com.example.generated;
                import java.util.Map;
                import org.apache.camel.builder.RouteBuilder;
                                import org.springframework.stereotype.Component;
                import com.fasterxml.jackson.core.type.TypeReference;
                import com.fasterxml.jackson.databind.ObjectMapper;
                @Component
                public class OrderLookupRoute extends RouteBuilder {
                    private final OrderResponseMapper mapper;
                    private final ObjectMapper objectMapper;
                    public OrderLookupRoute(OrderResponseMapper mapper, ObjectMapper objectMapper) {
                        this.mapper = mapper; this.objectMapper = objectMapper;
                    }
                    @Override public void configure() {
                        restConfiguration().component("platform-http");
                        rest("/orders").get("/{id}").to("direct:lookupOrder");
                        from("direct:lookupOrder")
                            .setHeader("orderId", simple("${header.id}"))
                            .setHeader("Accept", constant("application/json"))
                            .toD("{{orders.api-base-url}}/orders/${header.orderId}?bridgeEndpoint=true")
                            .process(exchange -> {
                                Map<String, Object> body = objectMapper.readValue(
                                    exchange.getIn().getBody(String.class), new TypeReference<Map<String, Object>>() {});
                                exchange.getMessage().setBody(mapper.map(body));
                            });
                    }
                }
                """);
        write(root, "src/main/resources/application.properties", """
                server.port=8080
                # Replace with the target API base URL for your environment.
                orders.api-base-url=${ORDERS_API_BASE_URL:http://localhost:9090}
                """);
        write(root, "README.md", """
                # Generated Order Lookup Spring Boot + Apache Camel service

                This is generated from the repository's **synthetic BW-style HTTP GET example**.

                ## Run
                1. Start a mock/real upstream API that returns JSON with `orderId`, `status`, and `total` fields.
                2. Set `ORDERS_API_BASE_URL` to its base URL (default `http://localhost:9090`).
                3. Run `mvn spring-boot:run` from this directory.
                4. Call `GET http://localhost:8080/orders/1001`.

                The Camel route calls `${ORDERS_API_BASE_URL}/1001`, deserializes JSON, applies the three mapping rules shown in `input/sample-bw-project/mapping/OrderMapper.xml`, and returns an `OrderView` JSON response.

                ## Important
                This generated example demonstrates the end-to-end target pattern. The current generator uses the known sample flow and illustrative mapping. It does not yet parse arbitrary native TIBCO BW process definitions or native Mapper/XPath expressions. Review endpoint security, TLS, authentication, error handling, retries, timeouts and schemas before production use.
                """);
        report.add("- Generated runnable service template: `generated-service/` (Spring Boot + Camel HTTP GET route and mapping bean).");
    }

    private static void write(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
