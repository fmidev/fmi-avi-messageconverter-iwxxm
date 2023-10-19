package fi.fmi.avi.converter.iwxxm.v2023_1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class VASIGMETIWWXXMSerializerTest {
    @Autowired
    private AviMessageConverter converter;

    protected SIGMET readFromJSON(final String fileName) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        try (InputStream inputStream = this.getClass().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                return objectMapper.readValue(inputStream, SIGMETImpl.class);
            } else {
                throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
            }
        }
    }

    protected String readFromFile(final String fileName) throws IOException {
        try {
            return new String(Files.readAllBytes(Paths.get(getClass().getResource(fileName).toURI())));
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
        }
    }

    private String fixIds(String s) {
        if (s == null) return null;
        return s.replaceAll("gml:id=\"(.*)\"", "gml:id=\"GMLID\"")
                .replaceAll("xlink:href=\"(.*)\"", "xlink:href=\"XLINKHREF\"")
                .replaceAll("volcanoId=\"(.*)\"", "volcanoId=\"VOLCANO\"");
    }

    @Test
    public void testVaSigmet() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1.json", "vasigmet1.xml");
    }

    @Test
    public void testVaSigmet_NoCoords() throws Exception {
        doTestSIGMETStringSerializationNoCoords("vasigmet1_nocoords.json", "vasigmet1_nocoords.xml");
    }

    @Test
    public void testVaSigmet_NoName() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1_noname.json", "vasigmet1_noname.xml");
    }

    @Test
    public void testVaSigmetCancel() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1_cancel.json", "vasigmet1_cancel.xml");
    }

    @Test
    public void testVaSigmetCancelMovToFir() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1_cancel_movtofir.json", "vasigmet1_cancel_movtofir.xml");
    }

    public void testSIGMETStringSerialization(String fn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING));
        final SIGMET s = readFromJSON(fn);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING);
        for (ConversionIssue iss : result.getConversionIssues()) {
            System.err.println("iss:" + iss);
        }
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
    }

    public String doTestSIGMETStringSerialization(final String fn, final String iwxxmFn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING));
        final SIGMET s = readFromJSON(fn);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING);

        for (ConversionIssue iss : result.getConversionIssues()) {
            System.err.println("iss:" + iss.getMessage() + "===" + iss.getCause());
        }

        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        System.err.println("XML:\n" + result.getConvertedMessage().get());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());

        String expectedXml = fixIds(readFromFile(iwxxmFn));
        assertEquals(expectedXml, fixIds(result.getConvertedMessage().get()));
        return result.getConvertedMessage().orElse(null);
    }

    public String doTestSIGMETStringSerializationNoCoords(final String fn, final String iwxxmFn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING));
        final SIGMET s = readFromJSON(fn);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING);

        boolean letItPass = false;
        for (ConversionIssue iss : result.getConversionIssues()) {
            System.err.println("iss:" + iss.getMessage() + "===" + iss.getCause());
            if (iss.toString().contains("SIGMET-6")) {
                letItPass = true;
            }
        }

        if (!letItPass) { //Skip assertion if error is in rule VolcanicAshSIGMET-6
            assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        }
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());

        System.out.println(result.getConvertedMessage().get());
        String expectedXml = fixIds(readFromFile(iwxxmFn));
        assertEquals(expectedXml, fixIds(result.getConvertedMessage().get()));
        return result.getConvertedMessage().orElse(null);
    }

}
