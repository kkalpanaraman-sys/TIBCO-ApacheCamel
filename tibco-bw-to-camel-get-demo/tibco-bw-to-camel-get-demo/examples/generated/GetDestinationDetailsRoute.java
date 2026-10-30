package com.example.generated;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/** Example output from the demo generator. */
@Component
public class GetDestinationDetailsRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:get-destination-details")
            .routeId("get-destination-details")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .toD("https://api.example.com/destinations/123")
            .log("HTTP GET response: ${body}");
    }
}
