package fi.fmi.avi.converter.iwxxm;

import java.time.ZonedDateTime;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.taf.AbstractTAFIWXXMParser;
import fi.fmi.avi.model.AviationCodeListUser;
import icao.iwxxm21.ReportType;

/**
 * Common functionality for parsing validation of IWXXM messages.
 */
public abstract class AbstractIWXXMScanner extends IWXXMConverterBase {

    private static Templates iwxxmTemplates;

    /**
     * Checks ther DOM Document against the official IWXXM 2.1.0 Schematron validation rules.
     * Uses a pre-generated XLS transformation file producing the Schematron SVRL report.
     *
     * @param input
     *         IWXXM message Document
     * @param hints
     *         conversion hints to guide the validaton
     *
     * @return the list of Schematron validation issues (failed asserts)
     */
    public static IssueList validateAgainstIWXXMSchematron(final Document input, final ConversionHints hints) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new IWXXMNamespaceContext());
        IssueList retval = new IssueList();
        try {
            DOMResult schematronOutput = new DOMResult();
            Transformer transformer = getIwxxmTemplates().newTransformer();
            DOMSource dSource = new DOMSource(input);
            transformer.transform(dSource, schematronOutput);
            NodeList failedAsserts = (NodeList) xPath.evaluate("//svrl:failed-assert/svrl:text", schematronOutput.getNode(), XPathConstants.NODESET);
            if (failedAsserts != null) {
                for (int i = 0; i < failedAsserts.getLength(); i++) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Failed Schematron assertation: " + failedAsserts.item(i).getNodeValue());
                }
            }
        } catch (TransformerException | XPathExpressionException e) {
            throw new RuntimeException("Unable to apply XSLT pre-compiled Schematron validation rules to the document to validate", e);
        }
        return retval;
    }

    protected static IssueList collectReportMetadata(final ReportType input, final GenericReportProperties properties, final ConversionHints hints) {
        IssueList retval = new IssueList();

        //Issues for these reported already by XML Schema or Schematron validation, so not checking them here:
        if (input.getPermissibleUsage() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.valueOf(input.getPermissibleUsage().name()));
        }
        if (input.getPermissibleUsageReason() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON, AviationCodeListUser.PermissibleUsageReason.valueOf(input.getPermissibleUsageReason().name()));
        }
        properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, input.getPermissibleUsageSupplementary());
        properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, input.getTranslatedBulletinID());

        XMLGregorianCalendar cal = input.getTranslatedBulletinReceptionTime();
        if (cal != null) {
            ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, time);
        }

        cal = input.getTranslationTime();
        if (cal != null) {
            ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATION_TIME, time);
        }

        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, input.getTranslationCentreDesignator());
        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, input.getTranslationCentreName());
        properties.set(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, input.getTranslationFailedTAC());
        return retval;
    }

    /*
       Performance optimization: use a pre-compiled the Templates object
       for running the XSL transformations required for IWXXM Schematron
       validation. This makes each validation 3-4 times faster.
   */
    private synchronized static Templates getIwxxmTemplates() {
        if (iwxxmTemplates == null) {
            TransformerFactory tFactory = TransformerFactory.newInstance();

            try {
                iwxxmTemplates = tFactory.newTemplates(new StreamSource(AbstractTAFIWXXMParser.class.getClassLoader().getResourceAsStream("schematron/xslt/iwxxm.xsl")));
            } catch (Exception e) {
                throw new RuntimeException("Unable to read XSL file for IWXXM 2.1 Schematron validation, make sure the the file exists in " + " classpath location 'schematron/xslt/iwxxm.xsl' ");
            }
        }
        return iwxxmTemplates;
    }

}
