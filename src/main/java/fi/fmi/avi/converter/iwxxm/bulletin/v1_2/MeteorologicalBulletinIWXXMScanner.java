package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;
import fi.fmi.avi.util.GTSExchangeFileInfo;

public class MeteorologicalBulletinIWXXMScanner<S extends AviationWeatherMessage, T extends MeteorologicalBulletin<S>> extends AbstractIWXXMScanner {

    private AviMessageSpecificConverter<Document, S> contentMessageConverter;

    private static IssueList collectHeading(final String bulletinIdentifier, final BulletinProperties properties) {
        final IssueList retval = new IssueList();
        try {
            final GTSExchangeFileInfo info = GTSExchangeFileInfo.Builder.from(bulletinIdentifier).build();
            properties.set(BulletinProperties.Name.HEADING, info.getHeading());
            info.getTimeStampYear().ifPresent(value -> properties.set(BulletinProperties.Name.TIMESTAMP_YEAR, value));
            info.getTimeStampMonth().ifPresent(value -> properties.set(BulletinProperties.Name.TIMESTAMP_MONTH, value));
            info.getTimeStampDay().ifPresent(value -> properties.set(BulletinProperties.Name.TIMESTAMP_DAY, value));
            info.getTimeStampHour().ifPresent(value -> properties.set(BulletinProperties.Name.TIMESTAMP_HOUR, value));
            info.getTimeStampMinute().ifPresent(value -> properties.set(BulletinProperties.Name.TIMESTAMP_MINUTE, value));
            info.getTimeStampSecond().ifPresent(value -> properties.set(BulletinProperties.Name.TIMESTAMP_SECOND, value));
        } catch (final Exception e) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Could not parse bulletin heading info from the bulletinIdentifier", e);
        }
        return retval;
    }

    public void setMessageConverter(final AviMessageSpecificConverter<Document, S> converter) {
        this.contentMessageConverter = converter;
    }

    public List<ConversionIssue> collectBulletinProperties(final Document input, final BulletinProperties properties, final ConversionHints hints) {
        final IssueList retval = new IssueList();
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        try {
            XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
            final String bulletinIdentifier = expr.evaluate(input.getDocumentElement());
            if (bulletinIdentifier.isEmpty()) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No or empty bulletinIdentifier in MeteorologicalBulletin");
                return retval;
            }

            retval.addAll(collectHeading(bulletinIdentifier, properties));

            expr = xpath.compile("/collect:MeteorologicalBulletin/collect:meteorologicalInformation/*");
            final NodeList features = (NodeList) expr.evaluate(input.getDocumentElement(), XPathConstants.NODESET);
            ConversionResult<S> messageResult;
            for (int i = 0; i < features.getLength(); i++) {
                messageResult = createAviationWeatherMessage((Element) features.item(i), hints);
                if (ConversionResult.Status.SUCCESS != messageResult.getStatus()) {
                    retval.addAll(messageResult.getConversionIssues());
                }
                if (messageResult.getConvertedMessage().isPresent()) {
                    properties.addToList(BulletinProperties.Name.MESSAGE, messageResult.getConvertedMessage().get());
                }
            }
        } catch (final XPathExpressionException xee) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Unexpected error in parsing MeteorologicalBulletin", xee);
        }
        return retval;
    }

    protected ConversionResult<S> createAviationWeatherMessage(final Element featureElement, final ConversionHints hints) {
        if (this.contentMessageConverter == null) {
            throw new IllegalStateException("messageConverter is not set");
        }
        ConversionResult<S> retval = new ConversionResult<>();
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document dom = db.newDocument();
            final Element root = dom.createElementNS(featureElement.getNamespaceURI(), featureElement.getNodeName());
            dom.appendChild(root);
            final NamedNodeMap attrs = featureElement.getAttributes();
            Node toSet;
            int size = attrs.getLength();
            for (int i = 0; i < size; i++) {
                toSet = attrs.item(i);
                final Attr a = (Attr) dom.importNode(toSet, true);
                root.setAttributeNodeNS(a);
            }
            final NodeList children = featureElement.getChildNodes();
            size = children.getLength();
            Node toAppend;
            for (int i = 0; i < size; i++) {
                toAppend = children.item(i).cloneNode(true);
                toAppend = dom.importNode(toAppend, true);
                root.appendChild(toAppend);
            }
            retval = this.contentMessageConverter.convertMessage(dom, hints);
        } catch (final ParserConfigurationException e) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER,
                    "Error in creating DOM document for the contained" + " message", e));
        }
        return retval;
    }

}
