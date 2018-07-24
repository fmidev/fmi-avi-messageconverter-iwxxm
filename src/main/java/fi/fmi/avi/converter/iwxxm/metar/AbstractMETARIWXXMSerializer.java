package fi.fmi.avi.converter.iwxxm.metar;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.model.metar.METAR;
import icao.iwxxm21.METARType;

/**
 * Common functionality for conversions related to producing IWXXM TAF messages.
 *
 * @param <T>
 *         the type of the
 */
public abstract class AbstractMETARIWXXMSerializer<T> extends AbstractIWXXMSerializer implements AviMessageSpecificConverter<METAR, T> {

    protected abstract T render(final METARType taf, final ConversionHints hints) throws ConversionException;

    /**
     * Converts a TAF object into another format.
     *
     * @param input
     *         input message
     * @param hints
     *         parsing hints
     *
     * @return the conversion result.
     */
    @Override
    public ConversionResult<T> convertMessage(METAR input, ConversionHints hints) {
        ConversionResult<T> result = new ConversionResult<>();
        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            return result;
        }

        if (!input.allAerodromeReferencesContainPosition()) {
            result.addIssue(
                    new ConversionIssue(Type.MISSING_DATA, "All aerodrome references must contain a reference point location before converting to IWXXM"));
            return result;
        }
        //TODO: everything
        return result;
    }

    @Override
    protected Source getCleanupTransformationStylesheet(ConversionHints hints) throws ConversionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(this.getClass().getResourceAsStream("METARCleanup.xsl"));
            return new DOMSource(doc);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ConversionException("Unexpected problem in reading the cleanup XSL sheet", e);
        }

    }

}
