package fi.fmi.avi.converter.iwxxm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.NamespaceContext;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Helper for using per-defined XML Schema namespace prefixes in IWXXM documents.
 */
public class IWXXMNamespaceContext extends NamespacePrefixMapper implements NamespaceContext {
    private static final Map<String, String> DEFAULT_MAPPING;
    private final Map<String, String> mapping;

    static {
        final Map<String, String> m = new HashMap<>();
        m.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        m.put("http://www.w3.org/1999/xlink", "xlink");
        m.put("http://www.opengis.net/gml/3.2", "gml");
        m.put("http://www.isotc211.org/2005/gmd", "gmd");
        m.put("http://www.isotc211.org/2005/gco", "gco");
        m.put("http://www.isotc211.org/2005/gts", "gts");
        m.put("http://www.aixm.aero/schema/5.1.1", "aixm");
        m.put("http://icao.int/iwxxm/2.1", "iwxxm");
        m.put("http://icao.int/iwxxm/3.0", "iwxxm30");
        m.put("http://def.wmo.int/opm/2013", "opm");
        m.put("http://def.wmo.int/metce/2013", "metce");
        m.put("http://def.wmo.int/collect/2014", "collect");
        m.put("http://www.opengis.net/om/2.0", "om");
        m.put("http://www.opengis.net/sampling/2.0", "sam");
        m.put("http://www.opengis.net/samplingSpatial/2.0", "sams");
        m.put("http://purl.oclc.org/dsdl/svrl", "svrl");
        Set<String> duplicates = findDuplicatePrefixes(m);
        if (duplicates.size() > 0) {
            throw new RuntimeException("The default namespace-prefix mapping contains duplicate prefixes, this is not allowed: " + duplicates.toString());
        }
        DEFAULT_MAPPING = Collections.unmodifiableMap(m);
    }

    public static String getDefaultURI(final String prefix) {
        return DEFAULT_MAPPING.entrySet().stream().filter(entry -> entry.getValue().equals(prefix)).map(Map.Entry::getKey).findAny().orElse(null);
    }

    public static String getDefaultPrefix(final String uri) {
        return DEFAULT_MAPPING.get(uri);
    }

    private static Set<String> findDuplicatePrefixes(final Map<String, String> mapping) {
        final Set<String> prefixes = new HashSet<>();
        return mapping.values().stream().filter(s -> !prefixes.add(s)).collect(Collectors.toSet());
    }

    public IWXXMNamespaceContext() {
        this.mapping = new HashMap<>(DEFAULT_MAPPING);
    }

    public String getURI(final String prefix) {
        return this.mapping.entrySet().stream().filter(entry -> entry.getValue().equals(prefix)).map(Map.Entry::getKey).findAny().orElse(null);
    }

    @Override
    public String getPreferredPrefix(final String namespace, final String prefix, final boolean required) {
        return mapping.getOrDefault(namespace, prefix);
    }

    @Override
    public String getNamespaceURI(final String prefix) {
        return getURI(prefix);
    };

    @Override
    public String getPrefix(final String uri) {
        return mapping.get(uri);
    }

    @Override
    public Iterator<String> getPrefixes(final String uri) {
        final ArrayList<String> retval = new ArrayList<>(1);
        final String value = this.getPrefix(uri);
        if (value != null) {
            retval.add(value);
        }
        return retval.iterator();
    }

    public String overrideNamespacePrefix(final String uri, final String prefix) {
        if (mapping.containsKey(uri)) {
            if (mapping.get(uri).equals(prefix)) {
                return prefix;
            }
            Optional<Map.Entry<String, String>> duplicate = this.mapping.entrySet().stream().filter(e -> e.getValue().equals(prefix)).findFirst();
            if (duplicate.isPresent()) {
                throw new IllegalArgumentException("The prefix '" + prefix + "' is already mapped to '" + duplicate.get().getKey());
            }
            return mapping.put(uri, prefix);
        } else {
            throw new IllegalArgumentException(
                    "Namespace URI '" + uri + "' not defined in the context, cannot override. Add new namespace " + "mapping with addNamespacePrefix method");
        }
    }

    public void addNamespacePrefix(final String uri, final String prefix) {
        if (!mapping.containsKey(uri)) {
            Optional<Map.Entry<String, String>> duplicate = this.mapping.entrySet().stream().filter(e -> e.getValue().equals(prefix)).findFirst();
            if (duplicate.isPresent()) {
                throw new IllegalArgumentException("The prefix '" + prefix + "' is already mapped to '" + duplicate.get().getKey());
            }
            mapping.put(uri, prefix);
        } else {
            throw new IllegalArgumentException("Namespace URI '" + uri + "' already defined in the context, cannot add. Override namespace "
                    + "mapping with overrideNamespacePrefix method");
        }
    }
}
