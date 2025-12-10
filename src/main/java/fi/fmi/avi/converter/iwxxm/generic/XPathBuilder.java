package fi.fmi.avi.converter.iwxxm.generic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for converting human-readable XPath expressions with namespace prefixes
 * into version-agnostic XPath expressions using local-name() and contains(namespace-uri(), ...).
 * <p>
 * This allows writing readable XPaths like:
 * <pre>
 *   /iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/gml:timePosition
 * </pre>
 * Which get converted to:
 * <pre>
 *   /*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='TAF']
 *   /*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='issueTime']
 *   /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='TimeInstant']
 *   /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='timePosition']
 * </pre>
 */
public final class XPathBuilder {

    public static final String IWXXM = "://icao.int/iwxxm/";
    public static final String GML = "://www.opengis.net/gml/";
    public static final String AIXM = "://www.aixm.aero/schema/";
    public static final String OM = "://www.opengis.net/om/";
    public static final String SAMS = "://www.opengis.net/samplingSpatial/";
    public static final String SAM = "://www.opengis.net/sampling/";

    private static final Map<String, String> PREFIX_TO_NS_PATTERN;
    // Matches path elements like /prefix:localName or /prefix:localName[predicate]
    private static final Pattern PREFIXED_ELEMENT = Pattern.compile("/([a-z]+):([A-Za-z_][A-Za-z0-9_]*)");

    static {
        final Map<String, String> map = new HashMap<>();
        map.put("iwxxm", IWXXM);
        map.put("gml", GML);
        map.put("aixm", AIXM);
        map.put("om", OM);
        map.put("sams", SAMS);
        map.put("sam", SAM);
        PREFIX_TO_NS_PATTERN = Collections.unmodifiableMap(map);
    }

    private XPathBuilder() {
        // Utility class
    }

    /**
     * Converts a human-readable XPath with namespace prefixes into a version-agnostic XPath.
     * <p>
     * Example input:
     * <pre>/iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/gml:timePosition</pre>
     * <p>
     * Example output:
     * <pre>
     *   /*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='TAF']
     *   /*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='issueTime']
     *   /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='TimeInstant']
     *   /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='timePosition']
     * </pre>
     *
     * @param readableXPath XPath with namespace prefixes (e.g. iwxxm:, gml:, aixm:)
     * @return version-agnostic XPath using local-name() and contains(namespace-uri(), ...)
     */
    public static String toVersionAgnostic(final String readableXPath) {
        final Matcher matcher = PREFIXED_ELEMENT.matcher(readableXPath);
        final StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            final String prefix = matcher.group(1);
            final String localName = matcher.group(2);
            final String nsPattern = PREFIX_TO_NS_PATTERN.get(prefix);

            final String replacement;
            if (nsPattern != null) {
                replacement = String.format("/*[contains(namespace-uri(),'%s') and local-name()='%s']",
                        nsPattern, localName);
            } else {
                // Unknown prefix, just match by local name
                replacement = "/*[local-name()='" + localName + "']";
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Converts a human-readable XPath and wraps it for text extraction.
     * The result selects the first matching node and extracts its normalized text content.
     *
     * @param readableXPath XPath with namespace prefixes
     * @return version-agnostic XPath wrapped in normalize-space()
     */
    public static String text(final String readableXPath) {
        return "normalize-space((" + toVersionAgnostic(readableXPath) + ")[1])";
    }

    /**
     * Converts a human-readable XPath and wraps it for node selection.
     * The result selects the first matching node.
     *
     * @param readableXPath XPath with namespace prefixes
     * @return version-agnostic XPath selecting the first matching node
     */
    public static String node(final String readableXPath) {
        return "(" + toVersionAgnostic(readableXPath) + ")[1]";
    }

    /**
     * Creates a relative XPath for selecting child elements (starting with ./ instead of /).
     * Useful for XPaths evaluated relative to an already-selected node.
     *
     * @param readableXPath XPath with namespace prefixes, starting with ./
     * @return version-agnostic relative XPath
     */
    public static String relative(final String readableXPath) {
        if (!readableXPath.startsWith("./")) {
            throw new IllegalArgumentException("Relative XPath must start with './': " + readableXPath);
        }
        // Convert ./prefix:element to ./*[...]
        return "." + toVersionAgnostic(readableXPath.substring(1));
    }

    /**
     * Creates an element selector that matches any of the given local names within a namespace.
     * Useful for METAR/SPECI where either root element is valid.
     * <p>
     * Example:
     * <pre>
     *   anyOf("iwxxm", "METAR", "SPECI")
     * </pre>
     * Produces:
     * <pre>
     *   *[contains(namespace-uri(),'://icao.int/iwxxm/') and (local-name()='METAR' or local-name()='SPECI')]
     * </pre>
     *
     * @param prefix     namespace prefix (e.g., "iwxxm")
     * @param localNames possible local names
     * @return XPath element selector matching any of the local names
     */
    public static String anyOf(final String prefix, final String... localNames) {
        if (localNames.length == 0) {
            throw new IllegalArgumentException("At least one local name required");
        }
        final String nsPattern = PREFIX_TO_NS_PATTERN.get(prefix);
        if (nsPattern == null) {
            throw new IllegalArgumentException("Unknown prefix: " + prefix);
        }

        if (localNames.length == 1) {
            return String.format("*[contains(namespace-uri(),'%s') and local-name()='%s']", nsPattern, localNames[0]);
        }

        final StringBuilder localNameCondition = new StringBuilder("(");
        for (int i = 0; i < localNames.length; i++) {
            if (i > 0) {
                localNameCondition.append(" or ");
            }
            localNameCondition.append("local-name()='").append(localNames[i]).append("'");
        }
        localNameCondition.append(")");

        return String.format("*[contains(namespace-uri(),'%s') and %s]", nsPattern, localNameCondition);
    }
}

