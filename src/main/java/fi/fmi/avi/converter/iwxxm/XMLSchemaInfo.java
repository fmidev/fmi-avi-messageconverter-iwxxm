package fi.fmi.avi.converter.iwxxm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

public class XMLSchemaInfo {
    private final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    private final List<Source> schemaSources = new ArrayList<>();
    private final Map<String, String> schemaLocations = new HashMap<>();
    private URL schematronRules;

    public XMLSchemaInfo() throws SAXException {
        this(false);
    }

    public XMLSchemaInfo(final boolean secureProcessing) throws SAXException {
        final IWXXMSchemaResourceResolver resolver = IWXXMSchemaResourceResolver.getInstance();
        schemaFactory.setResourceResolver(resolver);
        schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, secureProcessing);
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

    public void setSchematronRules(final URL sourceURL) {
        this.schematronRules = sourceURL;
    }

    public Schema getSchema() throws SAXException {
        return schemaFactory.newSchema(schemaSources.toArray(new Source[0]));
    }

    public String getSchemaLocations() {
        return schemaLocations.entrySet().stream().map((entry) -> entry.getKey() + " " + entry.getValue() + " ").reduce("", String::concat).trim();
    }

    public URL getSchematronRules() {
        return this.schematronRules;
    }

    public StreamSource getSchematronRulesSource() throws IOException {
        return new StreamSource(this.schematronRules.openStream(), this.schematronRules.toString());
    }
}
