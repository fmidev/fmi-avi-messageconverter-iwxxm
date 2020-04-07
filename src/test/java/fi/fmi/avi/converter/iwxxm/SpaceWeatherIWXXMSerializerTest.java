package fi.fmi.avi.converter.iwxxm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory.SpaceWeatherIWXXMStringSerializer;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.SpaceWeatherAdvisoryImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherIWXXMSerializerTest {

    @Autowired
    private AviMessageConverter converter;

    private ObjectMapper OBJECT_MAPPER;

    @Before
    public void setup() {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    }

    private String getInput(String fileName) throws IOException {
        InputStream is = null;
        try {
            String path = System.getProperty("user.dir") + "/src/test/resources/fi/fmi/avi/converter/iwxxm/" + fileName;
            File file = new File(path);
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                throw new FileNotFoundException("could not locate file as resource or using path " + path);
            }
            Objects.requireNonNull(is);

            return IOUtils.toString(is, "UTF-8");
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Test
    public void serializerTest() throws Exception {
        String input = getInput("spacewx-A2-4.json");
        SpaceWeatherAdvisoryImpl swx = OBJECT_MAPPER.readValue(input, SpaceWeatherAdvisoryImpl.class);
        System.out.println("sdfgtrse");

        SpaceWeatherIWXXMStringSerializer s = new SpaceWeatherIWXXMStringSerializer();
        ConversionResult<String> result = s.convertMessage(swx, null);

        System.out.println(result.getConvertedMessage());

        //assertTrue(converter.isSpecificationSupported(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING));
        //ConversionResult<String> result = converter.convertMessage(swx, IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING);



        //final String serialized = OBJECT_MAPPER.writeValueAsString(SWXObject);
        //final SpaceWeatherAdvisoryImpl deserialized = OBJECT_MAPPER.readValue(serialized, SpaceWeatherAdvisoryImpl.class);
    }
}
