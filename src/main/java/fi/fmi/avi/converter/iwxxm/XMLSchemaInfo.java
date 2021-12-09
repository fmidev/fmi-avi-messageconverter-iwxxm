package fi.fmi.avi.converter.iwxxm;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

public class XMLSchemaInfo {
    public static final String SCHEMA_LOCATION_ATTRIBUTE = "schemaLocation";

    private static final Logger LOG = LoggerFactory.getLogger(XMLSchemaInfo.class);
    private static final Pattern SCHEMA_LOCATION_SEPARATOR = Pattern.compile("\\s+");

    private final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    private final List<URL> schemaSources = new ArrayList<>();
    private final Map<String, String> schemaLocations = new LinkedHashMap<>();
    private final List<URL> schematronRules = new ArrayList<>();

    public XMLSchemaInfo() {
        this(false);
    }

    public XMLSchemaInfo(final boolean secureProcessing) {
        final IWXXMSchemaResourceResolver resolver = IWXXMSchemaResourceResolver.getInstance();
        schemaFactory.setResourceResolver(resolver);
        try {
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, secureProcessing);
        } catch (SAXNotSupportedException | SAXNotRecognizedException e) {
            LOG.warn("Failed to set the value of {} to XML SchemaFactory", XMLConstants.FEATURE_SECURE_PROCESSING, e);
        }
    }

    public static Map<String, String> decodeSchemaLocation(final String schemaLocation) {
        requireNonNull(schemaLocation, "schemaLocation");
        final String[] schemaLocationComponents = SCHEMA_LOCATION_SEPARATOR.split(schemaLocation);
        final Map<String, String> builder = new LinkedHashMap<>();
        for (int i = 0; i + 1 < schemaLocationComponents.length; i += 2) {
            final String namespace = schemaLocationComponents[i];
            final String location = schemaLocationComponents[i + 1];
            if (!namespace.isEmpty() && !location.isEmpty()) {
                builder.put(namespace, location);
            }
        }
        return builder;
    }

    public static String encodeSchemaLocation(final Map<String, String> schemaLocation) {
        requireNonNull(schemaLocation, "schemaLocation");
        return schemaLocation.entrySet().stream()//
                .map(entry -> entry.getKey() + " " + entry.getValue())//
                .collect(Collectors.joining(" "));
    }

    public void addAllFrom(final XMLSchemaInfo other) {
        requireNonNull(other, "other");
        schemaSources.addAll(other.schemaSources);
        schemaLocations.putAll(other.schemaLocations);
        schematronRules.addAll(other.schematronRules);
    }

    public void addSchemaSource(final URL source) {
        requireNonNull(source, "source");
        this.schemaSources.add(source);
    }

    public void addSchemaLocation(final String namespace, final String location) {
        requireNonNull(namespace, "namespace");
        requireNonNull(location, "location");
        this.schemaLocations.put(namespace, location);
    }

    public void addSchematronRule(final URL sourceURL) {
        requireNonNull(sourceURL, "sourceURL");
        this.schematronRules.add(sourceURL);
    }

    public Schema getSchema() throws SAXException {
        final List<Source> sources = new ArrayList<>(schemaSources.size());
        try {
            for (final URL sourceURL : schemaSources) {
                sources.add(new StreamSource(sourceURL.openStream()));
            }
            return schemaFactory.newSchema(sources.toArray(new Source[0]));
        } catch (final IOException e) {
            throw new SAXParseException(e.getMessage(), null, e);
        } finally {
            for (final IOException error : closeAll(sources)) {
                LOG.error("Error closing resource: {}", error.getMessage(), error);
            }
        }
    }

    public Map<String, String> getSchemaLocations() {
        return Collections.unmodifiableMap(this.schemaLocations);
    }

    public List<URL> getSchemaSources() {
        return Collections.unmodifiableList(this.schemaSources);
    }

    public String getCombinedSchemaLocations() {
        return encodeSchemaLocation(this.schemaLocations);
    }

    public List<URL> getSchematronRules() {
        return this.schematronRules;
    }

    public List<StreamSource> getSchematronRuleSources() {
        return this.schematronRules.stream().map(url -> {
            try {
                return new StreamSource(url.openStream(), url.toString());
            } catch (final IOException e) {
                LOG.warn("Unable to create StreamSource for the schematron rule from '{}'", url.toExternalForm(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<IOException> closeAll(final List<Source> sources) {
        final List<IOException> errors = new ArrayList<>(0);
        for (final Source source : sources) {
            if (source instanceof StreamSource) {
                final StreamSource streamSource = (StreamSource) source;
                close(streamSource.getInputStream(), errors);
                close(streamSource.getReader(), errors);
            }
        }
        return errors;
    }

    private void close(@Nullable final Closeable closeable, final List<IOException> errors) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
                errors.add(e);
            }
        }
    }
}
