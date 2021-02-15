package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherBulletin;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import icao.iwxxm21.TAFType;

/**
 * Created by rinne on 10/02/17.
 */
@Configuration
@Import({ IWXXMTAFConverter.class, IWXXMMETARSPECIConverter.class, IWXXMSIGMETAIRMETConverter.class, IWXXMSpaceWeatherConverter.class,
        IWXXMGenericBulletinConverter.class })
public class IWXXMConverter {

    // *******************
    //    TAF
    // *******************

    /**
     * Pre-configured spec for {@link TAF} to IWXXM 2.1 XML format TAF document String.
     */
    public static final ConversionSpecification<TAF, String> TAF_POJO_TO_IWXXM21_STRING = new ConversionSpecification<>(TAF.class, String.class, null,
            "TAF, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for {@link TAF} to IWXXM 2.1 XML format TAF document String.
     */
    public static final ConversionSpecification<TAF, String> TAF_POJO_TO_IWXXM30_STRING = new ConversionSpecification<>(TAF.class, String.class, null,
            "TAF, XML/IWXXM 3.0");

    /**
     * Pre-configured spec for {@link TAF} to IWXXM 2.1 XML format TAF document DOM Node.
     */
    public static final ConversionSpecification<TAF, Document> TAF_POJO_TO_IWXXM21_DOM = new ConversionSpecification<>(TAF.class, Document.class,
            null, "TAF, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for {@link TAF} to IWXXM 3.0.0 XML format TAF document DOM Node.
     */
    public static final ConversionSpecification<TAF, Document> TAF_POJO_TO_IWXXM30_DOM = new ConversionSpecification<>(TAF.class, Document.class,
            null, "TAF, XML/IWXXM 3.0.0");


    /**
     * Pre-configured spec for {@link TAF} to IWXXM 2.1 XML format TAFType JAXB class.
     */
    public static final ConversionSpecification<TAF, TAFType> TAF_POJO_TO_IWXXM21_JAXB = new ConversionSpecification<>(TAF.class, TAFType.class, null, null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format TAF document String to {@link TAF}.
     */
    public static final ConversionSpecification<String, TAF> IWXXM21_STRING_TO_TAF_POJO = new ConversionSpecification<>(String.class, TAF.class,
            "TAF, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format TAF document DOM Node to {@link TAF}.
     */
    public static final ConversionSpecification<Document, TAF> IWXXM21_DOM_TO_TAF_POJO = new ConversionSpecification<>(Document.class, TAF.class,
            "TAF, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for {@link TAFBulletin} to WMO COLLECT 1.2 XML String containing IWXXM 2.1 TAFs.
     */
    public static final ConversionSpecification<TAFBulletin, String> TAF_BULLETIN_POJO_TO_WMO_COLLECT_STRING = new ConversionSpecification<>(TAFBulletin.class,
            String.class, null, "XML/WMO COLLECT 1.2 + IWXXM 2.1 TAF");

    /**
     * Pre-configured spec for {@link TAFBulletin} to WMO COLLECT 1.2 XML DOM document containing IWXXM 2.1 TAFs.
     */
    public static final ConversionSpecification<TAFBulletin, Document> TAF_BULLETIN_POJO_TO_WMO_COLLECT_DOM = new ConversionSpecification<>(TAFBulletin.class,
            Document.class, null, "XML/WMO COLLECT 1.2 + IWXXM 2.1 TAF");

    /**
     * Pre-configured spec for WMO COLLECT 1.2 XML String containing IWXXM 2.1 TAFs to {@link TAFBulletin}.
     */
    public static final ConversionSpecification<String, TAFBulletin> WMO_COLLECT_STRING_TO_TAF_BULLETIN_POJO = new ConversionSpecification<>(String.class,
            TAFBulletin.class, "XML/WMO COLLECT 1.2 + IWXXM 2.1 TAF", null);

    /**
     * Pre-configured spec for WMO COLLECT 1.2 XML DOM document containing IWXXM 2.1 TAFs to {@link TAFBulletin}.
     */
    public static final ConversionSpecification<Document, TAFBulletin> WMO_COLLECT_DOM_TO_TAF_BULLETIN_POJO = new ConversionSpecification<>(Document.class,
            TAFBulletin.class, "XML/WMO COLLECT 1.2 + IWXXM 2.1 TAF", null);

    /**
     * Pre-configured spec for IWXXM 3.0.0 XML format TAF document String to {@link TAF}.
     */
    public static final ConversionSpecification<String, TAF> IWXXM30_STRING_TO_TAF_POJO = new ConversionSpecification<>(String.class, TAF.class,
            "TAF, XML/IWXXM 3.0.0", null);

    /**
     * Pre-configured spec for IWXXM 3.0.0 XML format TAF document DOM Node to {@link TAF}.
     */
    public static final ConversionSpecification<Document, TAF> IWXXM30_DOM_TO_TAF_POJO = new ConversionSpecification<>(Document.class, TAF.class,
            "TAF, XML/IWXXM 3.0.0", null);

    // *******************
    //  METAR & SPECI
    // *******************

    /**
     * Pre-configured spec for IWXXM 2.1 XML format METAR document String to {@link METAR}.
     */
    public static final ConversionSpecification<String, METAR> IWXXM21_STRING_TO_METAR_POJO = new ConversionSpecification<>(String.class, METAR.class,
            "METAR, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format METAR document DOM Node to {@link METAR}.
     */
    public static final ConversionSpecification<Document, METAR> IWXXM21_DOM_TO_METAR_POJO = new ConversionSpecification<>(Document.class, METAR.class,
            "METAR, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format SPECI document String to {@link SPECI}.
     */
    public static final ConversionSpecification<String, SPECI> IWXXM21_STRING_TO_SPECI_POJO = new ConversionSpecification<>(String.class, SPECI.class,
            "SPECI, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format SPECI document DOM Node to {@link SPECI}.
     */
    public static final ConversionSpecification<Document, SPECI> IWXXM21_DOM_TO_SPECI_POJO = new ConversionSpecification<>(Document.class, SPECI.class,
            "SPECI, XML/IWXXM 2.1", null);

    // *******************
    //  SIGMET & AIRMET
    // *******************

    /**
     * Pre-configured spec for IWXXM 2.1 XML format SIGMET document String to {@link SIGMET}.
     */
    public static final ConversionSpecification<String, SIGMET> IWXXM21_STRING_TO_SIGMET_POJO = new ConversionSpecification<>(String.class, SIGMET.class,
            "SIGMET, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format SIGMET document DOM Node to {@link SIGMET}.
     */
    public static final ConversionSpecification<Document, SIGMET> IWXXM21_DOM_TO_SIGMET_POJO = new ConversionSpecification<>(Document.class, SIGMET.class,
            "SIGMET, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for {@link SIGMET} to IWXXM 2.1 XML format SIGMET document String.
     */
    public static final ConversionSpecification<SIGMET, String> SIGMET_POJO_TO_IWXXM21_STRING = new ConversionSpecification<>(SIGMET.class, String.class,
            null, "SIGMET, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for {@link SIGMET} to IWXXM 2.1 XML format SIGMET document DOM Node.
     */
    public static final ConversionSpecification<SIGMET, Document> SIGMET_POJO_TO_IWXXM21_DOM = new ConversionSpecification<>(SIGMET.class, Document.class,
            null, "SIGMET, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for {@link AIRMET} to IWXXM 2.1 XML format AIRMET document String.
     */
    public static final ConversionSpecification<AIRMET, String> AIRMET_POJO_TO_IWXXM21_STRING = new ConversionSpecification<>(AIRMET.class, String.class, null,
            "AIRMET, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for {@link AIRMET} to IWXXM 2.1 XML format AIRMET document DOM Node.
     */
    public static final ConversionSpecification<AIRMET, Document> AIRMET_POJO_TO_IWXXM21_DOM = new ConversionSpecification<>(AIRMET.class, Document.class, null,
            "AIRMET, XML/IWXXM 2.1");

    // *******************
    //  Space weather
    // *******************
    /**
     * Pre-configured spec for IWXXM 3.0 XML format Space Weather Advisory document String to {@link SpaceWeatherAdvisory}.
     */
    public static final ConversionSpecification<String, SpaceWeatherAdvisory> IWXXM30_STRING_TO_SPACE_WEATHER_POJO = new ConversionSpecification<>(String.class,
            SpaceWeatherAdvisory.class, "SWX, XML/IWXXM 3.0", null);

    /**
     * Pre-configured spec for IWXXM 3.0 XML format Space Weather Advisory document DOM node to {@link SpaceWeatherAdvisory}.
     */
    public static final ConversionSpecification<Document, SpaceWeatherAdvisory> IWXXM30_DOM_TO_SPACE_WEATHER_POJO = new ConversionSpecification<>(
            Document.class, SpaceWeatherAdvisory.class, "SWX, XML/IWXXM 3.0", null);

    /**
     * Pre-configured spec for {@link SpaceWeatherAdvisory} to IWXXM 3.0 XML format SWX document String.
     */
    public static final ConversionSpecification<SpaceWeatherAdvisory, String> SPACE_WEATHER_POJO_TO_IWXXM30_STRING = new ConversionSpecification<>(
            SpaceWeatherAdvisory.class, String.class, null, "SWX, XML/IWXXM 3.0");

    /**
     * Pre-configured spec for {@link SpaceWeatherAdvisory} to IWXXM 3.0 XML format SWX document DOM node.
     */
    public static final ConversionSpecification<SpaceWeatherAdvisory, Document> SPACE_WEATHER_POJO_TO_IWXXM30_DOM = new ConversionSpecification<>(
            SpaceWeatherAdvisory.class, Document.class, null, "SWX, XML/IWXXM 3.0");

    /**
     * Pre-configured spec for {@link SpaceWeatherBulletin} to WMO COLLECT 1.2 XML String containing IWXXM 3.0 SpaceWeatherAdvisories.
     */
    public static final ConversionSpecification<SpaceWeatherBulletin, String> SWX_BULLETIN_POJO_TO_WMO_COLLECT_STRING = new ConversionSpecification<>(
            SpaceWeatherBulletin.class, String.class, null, "XML/WMO COLLECT 1.2 + IWXXM 3.0 SWX");

    /**
     * Pre-configured spec for {@link SpaceWeatherBulletin} to WMO COLLECT 1.2 XML DOM document containing IWXXM 3.0 SpaceWeatherAdvisories.
     */
    public static final ConversionSpecification<SpaceWeatherBulletin, Document> SWX_BULLETIN_POJO_TO_WMO_COLLECT_DOM = new ConversionSpecification<>(
            SpaceWeatherBulletin.class, Document.class, null, "XML/WMO COLLECT 1.2 + IWXXM 3.0 SWX");

    /**
     * Pre-configured spec for WMO COLLECT 1.2 XML String containing IWXXM 3.0 SpaceWeatherAdvisories to {@link SpaceWeatherBulletin}.
     */
    public static final ConversionSpecification<String, SpaceWeatherBulletin> WMO_COLLECT_STRING_TO_SWX_BULLETIN_POJO = new ConversionSpecification<>(
            String.class, SpaceWeatherBulletin.class, "XML/WMO COLLECT 1.2 + IWXXM 3.0 SWX", null);

    /**
     * Pre-configured spec for WMO COLLECT 1.2 XML DOM document containing IWXXM 3.0 SpaceWeatherAdvisories to {@link SpaceWeatherBulletin}.
     */
    public static final ConversionSpecification<Document, SpaceWeatherBulletin> WMO_COLLECT_DOM_TO_SWX_BULLETIN_POJO = new ConversionSpecification<>(
            Document.class, SpaceWeatherBulletin.class, "XML/WMO COLLECT 1.2 + IWXXM 3.0 SWX", null);

    // *******************
    //  Generic bulletins
    // *******************

    /**
     * Pre-configured spec for WMO COLLECT 1.2 XML DOM document to {@link GenericMeteorologicalBulletin}
     */
    public static final ConversionSpecification<Document, GenericMeteorologicalBulletin> IWXXM21_DOM_TO_GENERIC_BULLETIN_POJO = new ConversionSpecification<>(
            Document.class, GenericMeteorologicalBulletin.class, "XML/WMO COLLECT 1.2", null);

    /**
     * Pre-configured spec for WMO COLLECT 1.2 XML document String to {@link GenericMeteorologicalBulletin}
     */
    public static final ConversionSpecification<String, GenericMeteorologicalBulletin> IWXXM21_STRING_TO_GENERIC_BULLETIN_POJO = new ConversionSpecification<>(
            String.class, GenericMeteorologicalBulletin.class, "XML/WMO COLLECT 1.2", null);

}
