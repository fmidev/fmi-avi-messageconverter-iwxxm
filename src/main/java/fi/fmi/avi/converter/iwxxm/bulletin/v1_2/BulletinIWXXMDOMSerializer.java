package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.bulletin.AbstractBulletinIWXXMAixm511WxSerializer;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;

public class BulletinIWXXMDOMSerializer<U extends AviationWeatherMessage, S extends MeteorologicalBulletin<U>>
        extends AbstractBulletinIWXXMAixm511WxSerializer<Document, U, S> {

    @Override
    protected Document aggregateAsBulletin(final Document collection, final List<Document> messages, final ConversionHints hints) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document retval = db.newDocument();
            final Node copiedRoot = retval.importNode(collection.getDocumentElement(), true);
            retval.appendChild(copiedRoot);

            for (final Document outputMessage : messages) {
                final Node toAdd = retval.importNode(outputMessage.getDocumentElement(), true);
                final Element metInfo = retval.createElementNS(IWXXMNamespaceContext.getDefaultURI("collect"), "meteorologicalInformation");
                metInfo.appendChild(toAdd);
                metInfo.setPrefix("collect");
                final Node identifier = retval.getElementsByTagNameNS(IWXXMNamespaceContext.getDefaultURI("collect"), "bulletinIdentifier").item(0);
                if (identifier != null) {
                    identifier.getParentNode().insertBefore(metInfo, identifier);
                } else {
                    throw new IllegalStateException("The collection does not contain the identifier element yet");
                }
            }

            return retval;
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("DocumentBuilderFactory does not support feature " + XMLConstants.FEATURE_SECURE_PROCESSING, e);
        }
    }

    @Override
    protected IssueList validate(final Document output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
        return BulletinIWXXMDOMSerializer.validateDOMAgainstSchemaAndSchematron(output, schemaInfo, hints);
    }
}
