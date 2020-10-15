package fi.fmi.avi.converter.iwxxm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

public class XMLSchemaInfo {
    private static final Logger LOG = LoggerFactory.getLogger(XMLSchemaInfo.class);

    private final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    private final List<Source> schemaSources = new ArrayList<>();
    private final Map<String, String> schemaLocations = new HashMap<>();
    private final List<URL> schematronRules = new ArrayList<>();

    public XMLSchemaInfo()  {
        this(false);
    }

    public XMLSchemaInfo(final boolean secureProcessing) {
        final IWXXMSchemaResourceResolver resolver = IWXXMSchemaResourceResolver.getInstance();
        schemaFactory.setResourceResolver(resolver);
        try {
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, secureProcessing);
        } catch (SAXNotSupportedException | SAXNotRecognizedException e) {
            LOG.warn("Failed to set the value of " + XMLConstants.FEATURE_SECURE_PROCESSING +" to XML SchemaFactory",e);
        }
    }

    public void addSchemaSource(final Source source) {
        this.schemaSources.add(source);
    }

    public void addSchemaSource(final InputStream source) {
        this.schemaSources.add(new StreamSource(source));
    }

    public void addSchemaLocation(final String namespace, final String location) {
        this.schemaLocations.put(namespace, location);
    }

    public void addSchematronRule(final URL sourceURL) {
        this.schematronRules.add(sourceURL);
    }

    public Schema getSchema() throws SAXException {
        return schemaFactory.newSchema(schemaSources.toArray(new Source[0]));
    }

    public Map<String, String> getSchemaLocations() {
        return Collections.unmodifiableMap(this.schemaLocations);
    }

    public List<Source> getSchemaSources() {
        return Collections.unmodifiableList(this.schemaSources);
    }

    public String getCombinedSchemaLocations() {
        return schemaLocations.entrySet().stream().map((entry) -> entry.getKey() + " " + entry.getValue() + " ").reduce("", String::concat).trim();
    }

    public List<URL> getSchematronRules() {
        return this.schematronRules;
    }

    public List<StreamSource> getSchematronRuleSources() {
        return this.schematronRules.stream().map(url -> {
            try {
                return new StreamSource(url.openStream(), url.toString());
            } catch (final IOException e) {
                LOG.warn("Unable to create StreamSource for the schematron rule from '" + url.toExternalForm() + "'", e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
