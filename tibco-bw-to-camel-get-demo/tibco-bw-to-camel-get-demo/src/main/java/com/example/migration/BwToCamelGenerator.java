package com.example.migration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Java-only demonstration: reads the simplified BW-style XML sample
 * and generates an Apache Camel RouteBuilder class for an HTTP GET.
 */
public class BwToCamelGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: BwToCamelGenerator <input-bw-xml> <output-java-file>");
            return;
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(input.toFile());
        Element root = document.getDocumentElement();
        String processName = root.getAttribute("name");

        Element httpClient = (Element) document.getElementsByTagName("HttpClient").item(0);
        if (httpClient == null) {
            throw new IllegalArgumentException("No HttpClient activity found in input XML");
        }

        String activityName = httpClient.getAttribute("name");
        String method = httpClient.getElementsByTagName("Method").item(0).getTextContent().trim();
        String url = httpClient.getElementsByTagName("Url").item(0).getTextContent().trim();

        if (!"GET".equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("This demo supports GET only; found: " + method);
        }
        if (!(url.startsWith("https://") || url.startsWith("http://"))) {
            throw new IllegalArgumentException("Destination must be an http or https URL");
        }

        String className = toClassName(processName) + "Route";
        String routeId = toRouteId(processName);
        String javaSource = render(className, routeId, activityName, url);

        Files.createDirectories(output.getParent());
        Files.writeString(output, javaSource);
        System.out.println("Generated " + output.toAbsolutePath());
    }

    private static String render(String className, String routeId, String activityName, String url) {
        return "package com.example.generated;\\n\\n"
                + "import org.apache.camel.Exchange;\\n"
                + "import org.apache.camel.builder.RouteBuilder;\\n"
                + "import org.springframework.stereotype.Component;\\n\\n"
                + "@Component\\n"
                + "public class " + className + " extends RouteBuilder {\\n\\n"
                + "    @Override\\n"
                + "    public void configure() {\\n"
                + "        from(\\"direct:" + routeId + "\\")\\n"
                + "            .routeId(\\"" + routeId + "\\")\\n"
                + "            .setHeader(Exchange.HTTP_METHOD, constant(\\"GET\\"))\\n"
                + "            .toD(\\"" + escapeJava(url) + "\\")\\n"
                + "            .log(\\"HTTP GET response: ${body}\\");\\n"
                + "    }\\n"
                + "}\\n";
    }

    private static String toClassName(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[^A-Za-z0-9]", " ");
        StringBuilder result = new StringBuilder();
        for (String part : cleaned.trim().split("\\\\s+")) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return result.length() == 0 ? "Generated" : result.toString();
    }

    private static String toRouteId(String value) {
        String cleaned = value == null ? "bw-process" : value
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase();
        return cleaned.isBlank() ? "bw-process" : cleaned;
    }

    private static String escapeJava(String value) {
        return value.replace("\\\\", "\\\\\\\\").replace("\\"", "\\\\\"");
    }
}
