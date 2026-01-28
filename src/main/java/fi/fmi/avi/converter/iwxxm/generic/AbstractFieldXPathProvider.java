package fi.fmi.avi.converter.iwxxm.generic;

import java.util.*;

public abstract class AbstractFieldXPathProvider implements FieldXPathProvider {

    private final Map<IWXXMField, List<String>> expressions;

    protected AbstractFieldXPathProvider(final Map<IWXXMField, List<String>> expressions) {
        this.expressions = withCommonFields(expressions);
    }

    private static Map<IWXXMField, List<String>> withCommonFields(final Map<IWXXMField, List<String>> expressions) {
        final Map<IWXXMField, List<String>> merged = new EnumMap<>(expressions);
        put(merged, IWXXMField.GML_ID, "@gml:id");
        return Collections.unmodifiableMap(merged);
    }

    /**
     * Helper method for building field XPath maps in FieldXPathProvider implementations.
     * Wraps each path according to the field's type and adds them to the map.
     *
     * @param map   the map to add the field expressions to
     * @param field the field being mapped
     * @param paths one or more XPath expressions for this field
     */
    protected static void put(final Map<IWXXMField, List<String>> map, final IWXXMField field, final String... paths) {
        final List<String> wrapped = new ArrayList<>(paths.length);
        for (final String path : paths) {
            wrapped.add(XPathBuilder.wrap(path, field));
        }
        map.put(field, Collections.unmodifiableList(wrapped));
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
