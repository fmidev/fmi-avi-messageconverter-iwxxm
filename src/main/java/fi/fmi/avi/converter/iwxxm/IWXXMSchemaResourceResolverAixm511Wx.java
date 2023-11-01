package fi.fmi.avi.converter.iwxxm;

import org.w3c.dom.ls.LSInput;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Resolves the locations of the IWXXM XML Schema files within
 * the JAXB generated jar files using Java ClassLoader. Uses a memory
 * cache internally to avoid repeated class path searches
 * (keeps the full XML Schema file contents in memory as char arrays).
 */
public class IWXXMSchemaResourceResolverAixm511Wx extends IWXXMSchemaResourceResolver {

    private static final ConcurrentMap<String, LSInput> cache = new ConcurrentHashMap<>();
    private static IWXXMSchemaResourceResolverAixm511Wx instance;

    private IWXXMSchemaResourceResolverAixm511Wx() {
    }

    public synchronized static IWXXMSchemaResourceResolverAixm511Wx getInstance() {
        if (instance == null) {
            instance = new IWXXMSchemaResourceResolverAixm511Wx();
        }
        return instance;
    }

    @Override
    public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {
        final NamespaceLocation ns = forURI(namespaceURI);
        if (ns != null) {
            return cache.computeIfAbsent(ns.getFullPathFor(systemId),
                    key -> new ClassLoaderResourceInput(ns.getFinderClass(), ns.getFullPathFor(systemId), publicId, systemId, baseURI));
        }
        return null;
    }

    @Override
    public EnumSet<NamespaceLocation> ignoredNamespaceLocations() {
        return EnumSet.of(NamespaceLocation.AIXM511FULL);
    }
}
