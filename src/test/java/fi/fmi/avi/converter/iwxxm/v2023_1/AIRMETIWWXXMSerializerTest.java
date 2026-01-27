package fi.fmi.avi.converter.iwxxm.v2023_1;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class AIRMETIWWXXMSerializerTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private void doTestAIRMETDOMSerialization(final String fn) throws Exception {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM2023_1_DOM)).isTrue();
        final AIRMET s = readFromJSON(fn, AIRMETImpl.class);
        final ConversionResult<Document> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM2023_1_DOM);
        assertConversionResult(result).isSuccessful();
    }

    private void doTestAIRMETStringSerialization(final String fn, final String iwxxmFn) throws Exception {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM2023_1_STRING)).isTrue();
        final AIRMET s = readFromJSON(fn, AIRMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM2023_1_STRING);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString(iwxxmFn));
    }

    @Test
    public void dotestAIRMETStringSerialization1() throws Exception {
        doTestAIRMETStringSerialization("airmet_iwxxm1.json", "airmet_iwxxm1.xml");
    }

    @Test
    public void dotestAIRMETMOVING() throws Exception {
        doTestAIRMETStringSerialization("airmetMOVING.json", "airmetMOVING.xml");
    }

    @Test
    public void dotestAIRMETCleanup() throws Exception {
        doTestAIRMETStringSerialization("airmetMOVING.json", "airmetMOVING.xml");
    }

    @Test
    public void dotestAIRMETSTNR() throws Exception {
        doTestAIRMETStringSerialization("airmetSTNR.json", "airmetSTNR.xml");
    }

    @Test
    public void dotestAIRMETStringSerialization3() throws Exception {
        doTestAIRMETStringSerialization("airmet2.json", "airmet2.xml");
    }

    @Test
    public void dotestAIRMETStringSerialization4() throws Exception {
        doTestAIRMETStringSerialization("airmet_bkncld.json", "airmet_bkncld.xml");
    }

    @Test
    public void dotestAIRMETStringSerialization5() throws Exception {
        doTestAIRMETStringSerialization("airmet_ovccld.json", "airmet_ovccld.xml");
    }

    @Test
    public void dotestAIRMETStringSerialization6() throws Exception {
        //Because IWXXM 2023-1 can not represent ovccld with ABV the result should be the same as without ABV
        doTestAIRMETStringSerialization("airmet_ovccld_abv.json", "airmet_ovccld.xml");
    }

    @Test
    public void dotestAIRMETStringSerialization_wind() throws Exception {
        doTestAIRMETStringSerialization("airmet_wind.json", "airmet_wind.xml");
    }

    @Test
    public void dotestAIRMETStringSerialization_vis() throws Exception {
        doTestAIRMETStringSerialization("airmet_vis.json", "airmet_vis.xml");
    }

    @Test
    public void dotestAIRMET_OBS_BEFORE_StringSerialization3() throws Exception {
        doTestAIRMETStringSerialization("airmet2_obs_before.json", "airmet2_obs_before.xml");
    }

    @Test
    public void dotestAIRMET_OPER() throws Exception {
        doTestAIRMETStringSerialization("airmet_OPER.json", "airmet_OPER.xml");
    }

    @Test
    public void dotestAIRMET_TEST() throws Exception {
        doTestAIRMETStringSerialization("airmet_TEST.json", "airmet_TEST.xml");
    }

    @Test
    public void dotestAIRMET_EXER() throws Exception {
        doTestAIRMETStringSerialization("airmet_EXER.json", "airmet_EXER.xml");
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
    public void dotestAIRMETDOMSerialization3() throws Exception {
        doTestAIRMETDOMSerialization("airmet2.json");
    }

}
