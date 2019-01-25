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
import fi.fmi.avi.util.GTSExchangeFileInfo;

public class IWXXMGenericBulletinScanner extends AbstractIWXXMScanner {

    public static List<ConversionIssue> collectBulletinProperties(final Document input,
            final BulletinProperties properties, final ConversionHints hints) {
        final IssueList retval = new IssueList();

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        try {
            XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
            final String bulletinIdentifier = expr.evaluate(input.getDocumentElement());
            if ("".equals(bulletinIdentifier)) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No or empty bulletinIdentifier in MeteorologicalBulletin");
                return retval;
            }

            retval.addAll(collectHeading(bulletinIdentifier, properties));

            expr = xpath.compile("/collect:MeteorologicalBulletin/collect:meteorologicalInformation/*");
            final NodeList features = (NodeList) expr.evaluate(input.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < features.getLength(); i++) {
                collectGenericAviationWeatherMessage((Element) features.item(i), properties);
            }
        } catch (final XPathExpressionException xee) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Unexpected error in parsing MeteorologicalBulletin", xee);
        }
        return retval;
    }

    private static IssueList collectHeading(final String bulletinIdentifier, final BulletinProperties properties) {
        final IssueList retval = new IssueList();
        try {
            GTSExchangeFileInfo info = GTSExchangeFileInfo.Builder.from(bulletinIdentifier).build();
            properties.set(BulletinProperties.Name.HEADING, info.getHeading());
        } catch (Exception e) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Could not parse bulletin heading info from the bulletinIdentifier", e);
        }
        return retval;
    }

    private static IssueList collectGenericAviationWeatherMessage(final Element featureElement, final BulletinProperties properties) {
        final IssueList retval = new IssueList();
        //TODO: parse the DOM element into a GenericAviationWeatherMessage using XPath queries (not JAXB)

        return retval;
    }
}
