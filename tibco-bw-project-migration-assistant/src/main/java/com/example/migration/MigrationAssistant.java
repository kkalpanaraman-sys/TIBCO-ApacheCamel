package com.example.migration;

import org.w3c.dom.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MigrationAssistant {
    private static final List<String> XML_EXTENSIONS = List.of(".xml", ".process", ".xsd", ".wsdl");

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parseArgs(args);
        Path out = Path.of(options.getOrDefault("--out", "target/migration-output"));
        Path project;

        if (options.containsKey("--zip")) {
            project = Files.createTempDirectory("bw-project-");
            extractZipSafely(Path.of(options.get("--zip")), project);
        } else if (options.containsKey("--folder")) {
            project = Path.of(options.get("--folder"));
        } else {
            System.out.println("Usage: --zip <project.zip> OR --folder <project-folder> [--out <output-folder>]");
            return;
        }

        Files.createDirectories(out.resolve("java-models"));
        List<Path> allFiles;
        try (var stream = Files.walk(project)) {
            allFiles = stream.filter(Files::isRegularFile).sorted().toList();
        }

        List<String> report = new ArrayList<>();
        report.add("# BW Project Migration Analysis");
        report.add("");
        report.add("Files discovered: **" + allFiles.size() + "**");
        report.add("");
        report.add("## Project inventory");
        for (Path file : allFiles) {
            report.add("- `" + project.relativize(file).toString().replace('\\\\', '/') + "`");
        }

        report.add("");
        report.add("## Process activities and endpoint clues");
        for (Path file : allFiles) {
            if (isXmlLike(file) && (file.toString().toLowerCase().endsWith(".process")
                    || file.getParent().toString().toLowerCase().contains("process"))) {
                inspectProcess(file, report);
            }
        }

        report.add("");
        report.add("## Connection and protocol clues");
        for (Path file : allFiles) {
            if (isXmlLike(file) && (file.toString().toLowerCase().contains("shared")
                    || file.toString().toLowerCase().contains("connection"))) {
                inspectConnection(file, report);
            }
        }

        report.add("");
        report.add("## Mapper field inventory");
        for (Path file : allFiles) {
            if (isXmlLike(file) && (file.toString().toLowerCase().contains("mapping")
                    || file.toString().toLowerCase().contains("mapper"))) {
                inspectMappings(file, report);
            }
        }

        report.add("");
        report.add("## XSD model generation");
        int generated = 0;
        for (Path file : allFiles) {
            if (file.toString().toLowerCase().endsWith(".xsd")) {
                generated += generateModels(file, out.resolve("java-models"), report);
            }
        }
        report.add("");
        report.add("## Scope and review warnings");
        report.add("- This scanner uses generic XML heuristics against a synthetic sample; validate against the exact BW export version.");
        report.add("- Native BW Mapper expressions, imported schemas, namespaces, shared resources and process references may require deeper resolution.");
        report.add("- Endpoint URLs are configuration clues; credentials and environment-specific values must be supplied securely.");
        report.add("- No production route is generated in this version. Route and transformation generation are planned next steps.");

        Files.write(out.resolve("project-analysis.md"), report);
        System.out.println("Analysis written to: " + out.toAbsolutePath());
        System.out.println("Java model classes generated: " + generated);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) map.put(args[i], args[i + 1]);
        return map;
    }

    private static void extractZipSafely(Path zip, Path destination) throws IOException {
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = normalizedDestination.resolve(entry.getName()).normalize();
                if (!target.startsWith(normalizedDestination)) {
                    throw new IOException("Unsafe ZIP entry: " + entry.getName());
                }
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static boolean isXmlLike(Path p) {
        String n = p.toString().toLowerCase();
        return XML_EXTENSIONS.stream().anyMatch(n::endsWith);
    }

    private static Document parseXml(Path file) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        return f.newDocumentBuilder().parse(file.toFile());
    }

    private static void inspectProcess(Path file, List<String> report) {
        try {
            Document d = parseXml(file);
            report.add("");
            report.add("### `" + file.getFileName() + "`");
            Element root = d.getDocumentElement();
            report.add("- Process/root element: `" + root.getNodeName() + "`");
            NodeList nodes = d.getElementsByTagName("*");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                String name = localName(e);
                if (name.toLowerCase().contains("http") || name.equalsIgnoreCase("Mapper")
                        || name.toLowerCase().contains("jms") || name.toLowerCase().contains("soap")
                        || name.toLowerCase().contains("sftp")) {
                    report.add("- Activity clue: `" + name + "`" + attr(e, "name"));
                    String method = childText(e, "Method");
                    String url = childText(e, "Url");
                    String conn = childText(e, "ConnectionRef");
                    if (!method.isBlank()) report.add("  - Method: `" + method + "`");
                    if (!url.isBlank()) report.add("  - URL: `" + url + "`");
                    if (!conn.isBlank()) report.add("  - Connection reference: `" + conn + "`");
                }
            }
        } catch (Exception ex) {
            report.add("- Could not parse `" + file.getFileName() + "`: " + ex.getMessage());
        }
    }

    private static void inspectConnection(Path file, List<String> report) {
        try {
            Document d = parseXml(file);
            Element root = d.getDocumentElement();
            report.add("- `" + file.getFileName() + "`: `" + localName(root) + "`" + attr(root, "name"));
            for (String tag : List.of("Protocol", "BaseUrl", "Authentication", "TlsEnabled")) {
                String value = childText(root, tag);
                if (!value.isBlank()) report.add("  - " + tag + ": `" + value + "`");
            }
        } catch (Exception ex) {
            report.add("- Could not inspect `" + file.getFileName() + "`: " + ex.getMessage());
        }
    }

    private static void inspectMappings(Path file, List<String> report) {
        try {
            Document d = parseXml(file);
            NodeList fields = d.getElementsByTagNameNS("*", "Field");
            if (fields.getLength() == 0) fields = d.getElementsByTagName("Field");
            report.add("- `" + file.getFileName() + "`");
            for (int i = 0; i < fields.getLength(); i++) {
                Element e = (Element) fields.item(i);
                report.add("  - `" + e.getAttribute("source") + "` → `"
                        + e.getAttribute("target") + "` (expression: `"
                        + e.getAttribute("expression") + "`)");
            }
        } catch (Exception ex) {
            report.add("- Could not inspect mapping `" + file.getFileName() + "`: " + ex.getMessage());
        }
    }

    private static int generateModels(Path xsd, Path output, List<String> report) {
        int count = 0;
        try {
            Document d = parseXml(xsd);
            NodeList elements = d.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "element");
            for (int i = 0; i < elements.getLength(); i++) {
                Element e = (Element) elements.item(i);
                String name = e.getAttribute("name");
                if (name.isBlank() || !hasComplexType(e)) continue;
                String className = toClassName(name);
                StringBuilder source = new StringBuilder();
                source.append("package com.example.generated.model;\\n\\n");
                source.append("import java.math.BigDecimal;\\n\\n");
                source.append("public class ").append(className).append(" {\\n");
                NodeList childElements = e.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "element");
                List<String> getters = new ArrayList<>();
                for (int j = 0; j < childElements.getLength(); j++) {
                    Element child = (Element) childElements.item(j);
                    if (child == e) continue;
                    String field = child.getAttribute("name");
                    String type = javaType(child.getAttribute("type"));
                    if (field.isBlank()) continue;
                    source.append("    private ").append(type).append(" ").append(field).append(";\\n");
                    String cap = Character.toUpperCase(field.charAt(0)) + field.substring(1);
                    source.append("    public ").append(type).append(" get").append(cap)
                            .append("() { return ").append(field).append("; }\\n");
                    source.append("    public void set").append(cap).append("(").append(type).append(" ")
                            .append(field).append(") { this.").append(field).append(" = ").append(field).append("; }\\n");
                    getters.add(field);
                }
                source.append("}\\n");
                Files.writeString(output.resolve(className + ".java"), source);
                report.add("- Generated `" + className + ".java` from `" + xsd.getFileName() + "` with fields: " + String.join(", ", getters));
                count++;
            }
        } catch (Exception ex) {
            report.add("- Could not generate models from `" + xsd.getFileName() + "`: " + ex.getMessage());
        }
        return count;
    }

    private static boolean hasComplexType(Element e) {
        return e.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "complexType").getLength() > 0;
    }
    private static String javaType(String xsd) {
        String t = xsd.contains(":") ? xsd.substring(xsd.indexOf(':') + 1) : xsd;
        return switch (t) {
            case "decimal", "double", "float" -> "BigDecimal";
            case "int", "integer", "long", "short" -> "Long";
            case "boolean" -> "Boolean";
            default -> "String";
        };
    }
    private static String childText(Element e, String name) {
        NodeList n = e.getElementsByTagName(name);
        return n.getLength() == 0 ? "" : n.item(0).getTextContent().trim();
    }
    private static String attr(Element e, String name) {
        String value = e.getAttribute(name);
        return value.isBlank() ? "" : " (name: `" + value + "`)";
    }
    private static String localName(Element e) {
        return e.getLocalName() == null ? e.getNodeName() : e.getLocalName();
    }
    private static String toClassName(String value) {
        String[] parts = value.replaceAll("[^A-Za-z0-9]", " ").trim().split("\\\\s+");
        StringBuilder b = new StringBuilder();
        for (String p : parts) if (!p.isBlank()) b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        return b.toString().isBlank() ? "GeneratedModel" : b.toString();
    }
}
