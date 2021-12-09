package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
            final Document document = copyAsDocument(featureElement);
            retval = this.contentMessageConverter.convertMessage(document, hints);
        } catch (final ParserConfigurationException e) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER,
                    "Error in creating DOM document for the contained" + " message", e));
        }
        return retval;
    }

}
