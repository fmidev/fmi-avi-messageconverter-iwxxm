package fi.fmi.avi.converter.iwxxm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * Resolves the locations of the IWXXM XML Schema files within
 * the JAXB generated jar files using Java ClassLoader. Used a memory
 * cache internally to avoid repeated class path searches
 * (keeps the full XML Schema file contents in memory as Strings).
 */
public class IWXXMSchemaResourceResolver implements LSResourceResolver {

    public enum NamespaceLocation {
        XML("http://www.w3.org/XML/1998/namespace", "org/w3/2001/03/", null),
        XLINK11("http://www.w3.org/1999/xlink", "org/w3/xlink/1.1/", org.w3c.xlink11.ResourceType.class),
        GML32("http://www.opengis.net/gml/3.2", "net/opengis/gml/3.2.1/", net.opengis.gml32.AbstractGMLType.class),
        GTS("http://www.isotc211.org/2005/gts", "iso/19139/20070417/gts/", org.iso19139.ogc2007.gts.TMPrimitivePropertyType.class),
        GSR("http://www.isotc211.org/2005/gsr", "iso/19139/20070417/gsr/", org.iso19139.ogc2007.gsr.SCCRSPropertyType.class),
        GSS("http://www.isotc211.org/2005/gss", "iso/19139/20070417/gss/", org.iso19139.ogc2007.gss.GMObjectPropertyType.class),
        GCO("http://www.isotc211.org/2005/gco", "iso/19139/20070417/gco/", org.iso19139.ogc2007.gco.AbstractObjectType.class),
        GMD("http://www.isotc211.org/2005/gmd", "iso/19139/20070417/gmd/", org.iso19139.ogc2007.gmd.AbstractDQElementType.class),
        OM20("http://www.opengis.net/om/2.0", "net/opengis/om/2.0/", net.opengis.om20.OMObservationPropertyType.class),
        SAMPLING20("http://www.opengis.net/sampling/2.0", "net/opengis/sampling/2.0/", net.opengis.sampling.SamplingFeatureComplexType.class),
        SAMPLING_SPATIAL20("http://www.opengis.net/samplingSpatial/2.0", "net/opengis/samplingSpatial/2.0/",
                net.opengis.sampling.spatial.SFSpatialSamplingFeatureType.class),
        AIXM51("http://www.aixm.aero/schema/5.1.1", "aero/aixm/schema/5.1.1/", aero.aixm511.CodeICAOType.class),
        METCE12("http://def.wmo.int/metce/2013", "int/wmo/metce/1.2/", wmo.metce2013.ProcessType.class),
        OPM12("http://def.wmo.int/opm/2013", "int/wmo/opm/1.2/", wmo.opm2013.AbstractObservablePropertyPropertyType.class),
        IWXXM21("http://icao.int/iwxxm/2.1", "int/icao/iwxxm/2.1.1/", icao.iwxxm21.TAFType.class);

        private final String namespaceURI;
        private String pathPrefix;
        private Class<?> finderClass;

        NamespaceLocation(final String namespaceURI, final String pathPrefix, final Class<?> finderClass) {
            this.namespaceURI = namespaceURI;
            this.pathPrefix = pathPrefix;
            this.finderClass = finderClass;
        }

        public String getFullPathFor(final String systemId) {
            return this.pathPrefix + systemId.substring(systemId.lastIndexOf('/') + 1);
        }

        public String getNamespaceURI() {
            return this.namespaceURI;
        }

        public String getPathPrefix() {
            return this.pathPrefix;
        }

        public Class<?> getFinderClass() {
            return this.finderClass;
        }

        public static NamespaceLocation forURI(final String namespaceURI) {
            for (NamespaceLocation n : values()) {
                if (n.getNamespaceURI().equals(namespaceURI)) {
                    return n;
                }
            }
            return null;
        }
    }

    ;


    //Singleton
    private static IWXXMSchemaResourceResolver instance;

    public synchronized static IWXXMSchemaResourceResolver getInstance() {
        if (instance == null) {
            instance = new IWXXMSchemaResourceResolver();
        }
        return instance;
    }

    private static final Map<String, LSInput> cache = new HashMap<>();

    private IWXXMSchemaResourceResolver() {
    }

    @Override
    public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {
        NamespaceLocation ns = NamespaceLocation.forURI(namespaceURI);
        if (ns != null) {
            final String key = ns.getFullPathFor(systemId);
            synchronized (cache) {
                if (cache.containsKey(key)) {
                    return cache.get(key);
                } else {
                    cache.put(key, new ClassLoaderResourceInput(ns.getFinderClass(), ns.getFullPathFor(systemId), publicId, systemId, baseURI));
                    return cache.get(key);
                }
            }
        }
        return null;
    }

    private static class ClassLoaderResourceInput implements LSInput {
        private URL url;
        private String publicId;
        private String systemId;
        private String baseURI;
        private String cachedContent;

        public ClassLoaderResourceInput(final Class<?> cls, final String path, final String publicId, final String systemId, final String baseURI)
                throws IllegalArgumentException {
            if (cls != null) {
                this.url = cls.getClassLoader().getResource(path);
            } else {
                this.url = ClassLoaderResourceInput.class.getClassLoader().getResource(path);
            }
            if (this.url == null) {
                throw new IllegalArgumentException("Resource '" + path + "' not found in classpath");
            }
            this.publicId = publicId;
            this.systemId = systemId;
            this.baseURI = baseURI;
        }

        @Override
        public Reader getCharacterStream() {
            if (this.url == null) {
                return null;
            }
            try {
                return new BufferedReader(new InputStreamReader(this.url.openStream()));
            } catch (IOException e) {
                //Noop
            }
            return null;
        }

        @Override
        public void setCharacterStream(final Reader characterStream) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public InputStream getByteStream() {
            if (this.url == null) {
                return null;
            }
            try {
                return this.url.openStream();
            } catch (IOException e) {
                //NOOP
            }
            return null;
        }

        @Override
        public void setByteStream(final InputStream byteStream) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getStringData() {
            if (this.url == null) {
                return null;
            }
            if (this.cachedContent == null) {
                Reader input = this.getCharacterStream();
                if (input != null) {
                    try {
                        StringWriter sw = new StringWriter();
                        IOUtils.copy(input, sw);
                        this.cachedContent = sw.toString();
                    } catch (IOException e) {
                        //NOOP
                    }
                }
            }
            return this.cachedContent;
        }

        @Override
        public void setStringData(final String stringData) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getSystemId() {
            return this.systemId;
        }

        @Override
        public void setSystemId(final String systemId) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getPublicId() {
            return this.publicId;
        }

        @Override
        public void setPublicId(final String publicId) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getBaseURI() {
            return this.baseURI;
        }

        @Override
        public void setBaseURI(final String baseURI) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(final String encoding) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public boolean getCertifiedText() {
            return true;
        }

        @Override
        public void setCertifiedText(final boolean certifiedText) {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}
