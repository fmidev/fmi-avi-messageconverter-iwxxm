package fi.fmi.avi.converter.iwxxm.bulletin;

import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;
import fi.fmi.avi.util.GTSExchangeFileInfo;

public abstract class AbstractBulletinScanner<T extends MeteorologicalBulletin<? extends AviationWeatherMessage>> extends AbstractIWXXMScanner {

    public List<ConversionIssue> collectBulletinProperties(final Document input, final BulletinProperties properties, final ConversionHints hints) {
        final IssueList retval = new IssueList();

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        try {
            XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
            final String bulletinIdentifier = expr.evaluate(input.getDocumentElement());
            if ("" .equals(bulletinIdentifier)) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No or empty bulletinIdentifier in MeteorologicalBulletin");
                return retval;
            }

            retval.addAll(collectHeading(bulletinIdentifier, properties));

            expr = xpath.compile("/collect:MeteorologicalBulletin/collect:meteorologicalInformation/*");
            final NodeList features = (NodeList) expr.evaluate(input.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < features.getLength(); i++) {
                collectAviationWeatherMessage((Element) features.item(i), xpath, properties);
            }
        } catch (final XPathExpressionException xee) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Unexpected error in parsing MeteorologicalBulletin", xee);
        }
        return retval;
    }

    protected abstract IssueList collectAviationWeatherMessage(final Element featureElement, final XPath xpath, final BulletinProperties properties);

    private static IssueList collectHeading(final String bulletinIdentifier, final BulletinProperties properties) {
        final IssueList retval = new IssueList();
        try {
            final GTSExchangeFileInfo info = GTSExchangeFileInfo.Builder.from(bulletinIdentifier).build();
            properties.set(BulletinProperties.Name.HEADING, info.getHeading());
            info.getTimeStampYear().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_YEAR, value));
            info.getTimeStampMonth().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_MONTH, value));
            info.getTimeStampDay().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_DAY, value));
            info.getTimeStampHour().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_HOUR, value));
            info.getTimeStampMinute().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_MINUTE, value));
            info.getTimeStampSecond().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_SECOND, value));
        } catch (final Exception e) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Could not parse bulletin heading info from the bulletinIdentifier", e);
        }
        return retval;
    }

}
