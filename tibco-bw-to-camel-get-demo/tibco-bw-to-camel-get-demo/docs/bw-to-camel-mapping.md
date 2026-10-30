# BW HTTP GET → Camel mapping

The sample process is named `GetDestinationDetails`. It contains an HTTP client activity named `SendGetRequest`, with method `GET` and destination `https://api.example.com/destinations/123`.

The generated Camel route:

```java
from("direct:get-destination-details")
    .routeId("get-destination-details")
    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
    .toD("https://api.example.com/destinations/123")
    .log("HTTP GET response: ${body}");
```

The route sets the HTTP method, calls the destination, and exposes the HTTP response as the Camel exchange body. This first version does not yet model BW transitions, mappings, authentication, TLS, retries, or exception handling.
