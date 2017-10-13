package fi.fmi.avi.converter.iwxxm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;

/**
 * Created by rinne on 11/10/17.
 */
class IWXXMNamespaceMapper extends NamespacePrefixMapper implements NamespaceContext {
    private static Map<String, String> mapping = new HashMap<>();

    IWXXMNamespaceMapper() {
        mapping.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        mapping.put("http://www.w3.org/1999/xlink", "xlink");
        mapping.put("http://www.opengis.net/gml/3.2", "gml");
        mapping.put("http://www.isotc211.org/2005/gmd", "gmd");
        mapping.put("http://www.isotc211.org/2005/gco", "gco");
        mapping.put("http://www.isotc211.org/2005/gts", "gts");
        mapping.put("http://www.aixm.aero/schema/5.1.1", "aixm");
        mapping.put("http://icao.int/iwxxm/2.1", "iwxxm");
        mapping.put("http://def.wmo.int/opm/2013", "opm");
        mapping.put("http://def.wmo.int/metce/2013", "metce");
        mapping.put("http://www.opengis.net/om/2.0", "om");
        mapping.put("http://www.opengis.net/sampling/2.0", "sam");
        mapping.put("http://www.opengis.net/samplingSpatial/2.0", "sams");
    }

    @Override
    public String getPreferredPrefix(final String namespace, final String prefix, final boolean required) {
        return mapping.getOrDefault(namespace, prefix);
    }

    public String getNamespaceURI(final String prefix) {
        return mapping.entrySet().stream().filter(entry -> entry.getValue().equals(prefix)).map(Map.Entry::getKey).findAny().orElse(null);
    };

    public String getPrefix(final String uri) {
        return mapping.get(uri);
    }

    public Iterator<?> getPrefixes(final String uri) {
        ArrayList<String> retval = new ArrayList<>(1);
        String value = this.getPrefix(uri);
        if (value != null) {
            retval.add(value);
        }
        return retval.iterator();
    }
}
