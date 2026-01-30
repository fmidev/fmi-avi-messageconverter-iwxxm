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
 * <pre>{@code
 *   ./iwxxm:issueTime/gml:TimeInstant/gml:timePosition
 * }</pre>
 * Which get converted to:
 * <pre>{@code
 *   ./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='issueTime']
 *    /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='TimeInstant']
 *    /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='timePosition']
 * }</pre>
 * <p>
 * Also handles namespaced attributes like {@code @gml:id} which get converted to:
 * <pre>{@code
 *   @*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='id']
 * }</pre>
 */
public final class XPathBuilder {

    private static final Map<String, String> PREFIX_TO_NS_PATTERN = createPrefixToNsPattern();
    // Matches either /prefix:localName (elements) or @prefix:localName (attributes)
    // The type group captures "/" or "@" to determine if it's an element or attribute
    private static final Pattern PREFIXED_NAME = Pattern.compile(
            "(?<type>[/@])(?<prefix>[a-zA-Z][a-zA-Z0-9]*):(?<localPart>[A-Za-z_][A-Za-z0-9_.-]*)");

    private XPathBuilder() {
    }

    private static Map<String, String> createPrefixToNsPattern() {
        final Map<String, String> map = new HashMap<>();
        map.put("iwxxm", "://icao.int/iwxxm/");
        map.put("gml", "://www.opengis.net/gml/");
        map.put("aixm", "://www.aixm.aero/schema/");
        map.put("om", "://www.opengis.net/om/");
        map.put("sams", "://www.opengis.net/samplingSpatial/");
        map.put("sam", "://www.opengis.net/sampling/");
        map.put("collect", "://def.wmo.int/collect/");
        return Collections.unmodifiableMap(map);
    }

    /**
     * Converts a human-readable XPath with namespace prefixes into a version-agnostic XPath.
     * <p>
     * Replaces {@code /prefix:localName} with {@code /*[contains(namespace-uri(),'nsPattern') and local-name()='localName']},
     * and {@code @prefix:localName} with {@code @*[contains(namespace-uri(),'nsPattern') and local-name()='localName']},
     * where {@code nsPattern} is a version-independent namespace URI fragment for the given prefix.
     * </p>
     *
     * @param readableXPath XPath with namespace prefixes (e.g. iwxxm:, gml:, aixm:)
     * @return version-agnostic XPath using local-name() and contains(namespace-uri(), nsPattern)
     * @throws IllegalArgumentException if the XPath contains an unknown namespace prefix
     */
    public static String toVersionAgnostic(final String readableXPath) {
        final Matcher matcher = PREFIXED_NAME.matcher(readableXPath);
        // TODO: Replace StringBuffer with StringBuilder when moving to Java 9+
        final StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            final String type = matcher.group("type");
            final String prefix = matcher.group("prefix");
            final String nsPattern = PREFIX_TO_NS_PATTERN.get(prefix);
            if (nsPattern == null) {
                throw new IllegalArgumentException("Unknown namespace prefix: " + prefix
                        + ". Known prefixes: " + PREFIX_TO_NS_PATTERN.keySet());
            }
            final String localName = matcher.group("localPart");
            final String replacement = type + "*[contains(namespace-uri(),'" + nsPattern + "') and local-name()='" + localName + "']";
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
     * Converts a human-readable XPath and wraps it according to the field's XPath data type.
     *
     * @param readableXPath XPath with namespace prefixes
     * @param field         the field being queried, determines wrapping style
     * @return version-agnostic XPath with appropriate wrapper
     */
    public static String wrap(final String readableXPath, final IWXXMField field) {
        switch (field.getXPathDataType()) {
            case STRING:
                return text(readableXPath);
            case NODE:
                return node(readableXPath);
            default:
                throw new IllegalArgumentException("Unknown XPath data type: " + field.getXPathDataType());
        }
    }

}

