package fi.fmi.avi.converter.iwxxm.generic;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for converting human-readable XPath expressions with namespace prefixes
 * into version-agnostic XPath expressions using local-name() and contains(namespace-uri(), ...).
 * <p>
 * This allows writing readable XPaths like:
 * <pre>
 *   ./iwxxm:issueTime/gml:TimeInstant/gml:timePosition
 * </pre>
 * Which get converted to:
 * <pre>
 *   ./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='issueTime']
 *    /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='TimeInstant']
 *    /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='timePosition']
 * </pre>
 * Both relative paths (starting with ./) and absolute paths (starting with /) are supported.
 */
public final class XPathBuilder {

    private static final Map<String, String> PREFIX_TO_NS_PATTERN;
    // Matches /prefix:localName or ./prefix:localName in XPath expressions. Predicates like [1] are not matched and remain untouched.
    private static final Pattern PREFIXED_ELEMENT = Pattern.compile("(\\.)?/([a-zA-Z]+):([A-Za-z_][A-Za-z0-9_]*)");

    static {
        final Map<String, String> map = new HashMap<>();
        map.put("iwxxm", "://icao.int/iwxxm/");
        map.put("gml", "://www.opengis.net/gml/");
        map.put("aixm", "://www.aixm.aero/schema/");
        map.put("om", "://www.opengis.net/om/");
        map.put("sams", "://www.opengis.net/samplingSpatial/");
        map.put("sam", "://www.opengis.net/sampling/");
        map.put("collect", "://def.wmo.int/collect/");
        PREFIX_TO_NS_PATTERN = Collections.unmodifiableMap(map);
    }

    private XPathBuilder() {
    }

    /**
     * Converts a human-readable XPath with namespace prefixes into a version-agnostic XPath.
     * <p>
     * Example input:
     * <pre>./iwxxm:issueTime/gml:TimeInstant/gml:timePosition</pre>
     * <p>
     * Example output:
     * <pre>
     *   ./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='issueTime']
     *    /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='TimeInstant']
     *    /*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='timePosition']
     * </pre>
     *
     * @param readableXPath XPath with namespace prefixes (e.g. iwxxm:, gml:, aixm:), can be relative (./) or absolute (/)
     * @return version-agnostic XPath using local-name() and contains(namespace-uri(), ...)
     */
    public static String toVersionAgnostic(final String readableXPath) {
        final Matcher matcher = PREFIXED_ELEMENT.matcher(readableXPath);
        final StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            final String dot = matcher.group(1);  // Optional leading "." for relative paths
            final String prefix = matcher.group(2);
            final String localName = matcher.group(3);
            final String nsPattern = PREFIX_TO_NS_PATTERN.get(prefix);

            final String replacement;
            final String pathStart = dot != null ? "./*" : "/*";
            if (nsPattern != null) {
                replacement = String.format("%s[contains(namespace-uri(),'%s') and local-name()='%s']",
                        pathStart, nsPattern, localName);
            } else {
                // Unknown prefix, just match by local name
                replacement = pathStart + "[local-name()='" + localName + "']";
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
     * Converts a human-readable XPath and wraps it according to the field type.
     *
     * @param readableXPath XPath with namespace prefixes
     * @param field the field being queried, determines wrapping style
     * @return version-agnostic XPath with appropriate wrapper
     */
    public static String wrap(final String readableXPath, final IWXXMField field) {
        switch (field.getType()) {
            case TEXT:
                return text(readableXPath);
            case NODE:
                return node(readableXPath);
            default:
                throw new IllegalArgumentException("Unknown field type: " + field.getType());
        }
    }

    /**
     * Helper method for building field XPath maps in FieldXPathProvider implementations.
     * Wraps each path according to the field's type and adds them to the map.
     *
     * @param map   the map to add the field expressions to
     * @param field the field being mapped
     * @param paths one or more XPath expressions for this field
     */
    public static void put(final Map<IWXXMField, List<String>> map, final IWXXMField field, final String... paths) {
        final List<String> wrapped = new ArrayList<>(paths.length);
        for (final String path : paths) {
            wrapped.add(wrap(path, field));
        }
        map.put(field, wrapped);
    }
}

