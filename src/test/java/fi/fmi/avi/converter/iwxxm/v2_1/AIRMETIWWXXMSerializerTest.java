package fi.fmi.avi.converter.iwxxm.v2_1;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class AIRMETIWWXXMSerializerTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private String doTestAIRMETStringSerialization(final String fn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING));
        final AIRMET s = readFromJSON(fn, AIRMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING);
        // Allow WITH_WARNINGS here due to issue:
        // "When AIRMETEvolvingConditionCollection timeIndicator is an observation, the phenomenonTime must be earlier than or equal to the beginning of the validPeriod of the report."
        // This seems to be a shortcoming of the rule (xlinked validTime is not considered)
        assertFalse(ConversionResult.Status.isMoreCritical(result.getStatus(), ConversionResult.Status.WITH_WARNINGS));
        assertTrue(result.getConvertedMessage().isPresent());
        return result.getConvertedMessage().get();
    }

    private void doTestAIRMETDOMSerialization(final String fn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM));
        final AIRMET s = readFromJSON(fn, AIRMETImpl.class);
        final ConversionResult<Document> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM);
        assertConversionResult(result).isSuccessful();
    }

    @Test
    public void dotestAIRMETStringSerialization1() throws Exception {
        doTestAIRMETStringSerialization("airmet_iwxxm1.json");
    }

    @Test
    public void dotestAIRMETMOVING() throws Exception {
        doTestAIRMETStringSerialization("airmetMOVING.json");
    }

    @Test
    public void testAIRMETCleanup() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING));
        final AIRMET s = readFromJSON("airmetMOVING.json", AIRMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString("airmetMOVING.IWXXM21"));
    }

    @Test
    public void dotestAIRMETSTNR() throws Exception {
        doTestAIRMETStringSerialization("airmetSTNR.json");
    }

    @Test
    public void dotestAIRMETStringSerialization3() throws Exception {
        doTestAIRMETStringSerialization("airmet2.json");
    }

    @Test
    public void dotestAIRMETStringSerialization4() throws Exception {
        doTestAIRMETStringSerialization("airmet_bkncld.json");
    }

    @Test
    public void dotestAIRMETStringSerialization5() throws Exception {
        doTestAIRMETStringSerialization("airmet_ovccld_abv.json");
    }

    @Test
    public void dotestAIRMETStringSerialization_wind() throws Exception {
        doTestAIRMETStringSerialization("airmet_wind.json");
    }

    @Test
    public void dotestAIRMETStringSerialization_vis() throws Exception {
        doTestAIRMETStringSerialization("airmet_vis.json");
    }

    @Test
    public void dotestAIRMETDOMSerialization1() throws Exception {
        doTestAIRMETDOMSerialization("airmet_iwxxm1.json");
    }

    @Test
    public void dotestAIRMETDOMSerialization2() throws Exception {
        doTestAIRMETDOMSerialization("airmetMOVING.json");
    }

    @Test
    @Ignore("Schematron issue, see comments in doTestAIRMETStringSerialization")
    public void dotestAIRMETDOMSerialization3() throws Exception {
        doTestAIRMETDOMSerialization("airmet2.json");
    }

}
