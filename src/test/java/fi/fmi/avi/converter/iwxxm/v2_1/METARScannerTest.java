package fi.fmi.avi.converter.iwxxm.v2_1;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.iwxxm.*;
import fi.fmi.avi.converter.iwxxm.v2_1.metar.METARIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.metar.METARProperties;
import fi.fmi.avi.converter.iwxxm.v2_1.metar.ObservationRecordProperties;
import fi.fmi.avi.converter.iwxxm.v2_1.metar.TrendForecastRecordProperties;
import fi.fmi.avi.model.*;
import icao.iwxxm21.METARType;
import icao.iwxxm21.ReportType;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import wmo.metce2013.ProcessType;

import javax.xml.XMLConstants;
import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class METARScannerTest implements IWXXMConverterTests {

    private List<ConversionIssue> withCollectedPropertiesFrom(final String fileName, final Consumer<METARProperties> resultHandler) throws Exception {
        final Document doc = IWXXMConverterTests.readDocumentFromResource(fileName, METARScannerTest.class);
        final JAXBContext ctx = AbstractIWXXMAixm511WxSerializer.getAixm511WxJAXBContext();
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final IWXXMSchemaResourceResolver resolver = IWXXMSchemaResourceResolverAixm511Wx.getInstance();
        schemaFactory.setResourceResolver(resolver);
        //Secure processing does not allow "file" protocol loading for schemas:
        schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        final Schema iwxxmSchema = schemaFactory.newSchema(ReportType.class.getResource("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));

        final Binder<Node> binder = ctx.createBinder();

        //XML Schema validation upon JAXB unmarshal:
        binder.setSchema(iwxxmSchema);

        final METARType source = binder.unmarshal(doc, METARType.class).getValue();

        final ReferredObjectRetrievalContext refCtx = new ReferredObjectRetrievalContext(doc, binder);
        final METARProperties metarProperties = new METARProperties();
        final List<ConversionIssue> issues = METARIWXXMScanner.collectMETARProperties(source, refCtx, metarProperties, ConversionHints.EMPTY);
        resultHandler.accept(metarProperties);
        return issues;
    }

    @Test
    public void testPropertiesSetForMETAR_A3() throws Exception {
        final List<ConversionIssue> issues = withCollectedPropertiesFrom("metar-A3-1.xml", props -> {
            assertTrue(props.get(METARProperties.Name.STATUS, AviationCodeListUser.MetarStatus.class).isPresent());
            assertTrue(props.get(METARProperties.Name.SPECI, Boolean.class).isPresent());
            assertTrue(props.get(METARProperties.Name.AUTOMATED, Boolean.class).isPresent());
            assertFalse(props.get(METARProperties.Name.SNOW_CLOSURE, Boolean.class).isPresent());
            assertFalse(props.get(METARProperties.Name.TREND_NO_SIGNIFICANT_CHANGES, Boolean.class).isPresent());
            assertTrue(props.get(METARProperties.Name.OBSERVATION, OMObservationProperties.class).isPresent());
            assertFalse(props.getList(METARProperties.Name.TREND_FORECAST, OMObservationProperties.class).isEmpty());
            assertTrue(props.get(METARProperties.Name.REPORT_METADATA, GenericReportProperties.class).isPresent());

            final OMObservationProperties obs = props.get(METARProperties.Name.OBSERVATION, OMObservationProperties.class).get();
            assertTrue(obs.get(OMObservationProperties.Name.TYPE, String.class).isPresent());
            assertTrue(obs.get(OMObservationProperties.Name.PHENOMENON_TIME, PartialOrCompleteTimeInstant.class).isPresent());
            assertTrue(obs.get(OMObservationProperties.Name.RESULT_TIME, PartialOrCompleteTimeInstant.class).isPresent());
            assertFalse(obs.get(OMObservationProperties.Name.VALID_TIME, String.class).isPresent());
            assertTrue(obs.get(OMObservationProperties.Name.PROCEDURE, ProcessType.class).isPresent());
            assertTrue(obs.get(OMObservationProperties.Name.OBSERVED_PROPERTY, String.class).isPresent());
            assertTrue(obs.get(OMObservationProperties.Name.AERODROME, Aerodrome.class).isPresent());
            assertTrue(obs.get(OMObservationProperties.Name.SAMPLING_POINT, ElevatedPoint.class).isPresent());
            assertTrue(obs.get(OMObservationProperties.Name.RESULT, ObservationRecordProperties.class).isPresent());

            final List<OMObservationProperties> trends = props.getList(METARProperties.Name.TREND_FORECAST, OMObservationProperties.class);
            assertEquals(2, trends.size());
            for (final OMObservationProperties trend : trends) {
                assertTrue(trend.get(OMObservationProperties.Name.TYPE, String.class).isPresent());
                assertTrue(trend.get(OMObservationProperties.Name.PHENOMENON_TIME, PartialOrCompleteTime.class).isPresent());
                assertTrue(trend.get(OMObservationProperties.Name.RESULT_TIME, PartialOrCompleteTimeInstant.class).isPresent());
                assertFalse(trend.get(OMObservationProperties.Name.VALID_TIME, String.class).isPresent());
                assertTrue(trend.get(OMObservationProperties.Name.PROCEDURE, ProcessType.class).isPresent());
                assertTrue(trend.get(OMObservationProperties.Name.OBSERVED_PROPERTY, String.class).isPresent());
                assertTrue(trend.get(OMObservationProperties.Name.AERODROME, Aerodrome.class).isPresent());
                assertTrue(trend.get(OMObservationProperties.Name.SAMPLING_POINT, ElevatedPoint.class).isPresent());
                assertTrue(trend.get(OMObservationProperties.Name.RESULT, TrendForecastRecordProperties.class).isPresent());
            }

            final GenericReportProperties meta = props.get(METARProperties.Name.REPORT_METADATA, GenericReportProperties.class).get();
            assertTrue(meta.get(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.class).isPresent());
            assertFalse(meta.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, String.class).isPresent());
            assertFalse(meta.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, String.class).isPresent());
            assertFalse(meta.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, ZonedDateTime.class).isPresent());
            assertFalse(meta.get(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, String.class).isPresent());
            assertFalse(meta.get(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, String.class).isPresent());
            assertFalse(meta.get(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.class).isPresent());
            assertFalse(meta.get(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, String.class).isPresent());
        });
        assertTrue("No issues should have been found", issues.isEmpty());
    }

}
