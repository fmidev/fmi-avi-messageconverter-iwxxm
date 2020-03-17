package fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.LinearRingType;
import net.opengis.gml32.PolygonPatchType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePositionType;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractJAXBIWXXMParser;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.PartialOrCompleteTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PhenomenonGeometryWithHeight;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AdvisoryNumber;
import fi.fmi.avi.model.SpaceWeatherAdvisory.NextAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherRegion;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.AdvisoryNumberImpl;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.NextAdvisoryImpl;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.SpaceWeatherAdvisoryAnalysisImpl;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.SpaceWeatherAdvisoryImpl;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.SpaceWeatherRegionImpl;
import fi.fmi.avi.model.TacOrGeoGeometry;
import fi.fmi.avi.model.immutable.PhenomenonGeometryWithHeightImpl;
import fi.fmi.avi.model.immutable.PolygonsGeometryImpl;
import fi.fmi.avi.model.immutable.TacOrGeoGeometryImpl;
import icao.iwxxm30.SpaceWeatherAdvisoryType;
import icao.iwxxm30.SpaceWeatherAnalysisPropertyType;
import icao.iwxxm30.SpaceWeatherAnalysisType;
import icao.iwxxm30.SpaceWeatherLocationType;
import icao.iwxxm30.SpaceWeatherPhenomenaType;
import icao.iwxxm30.SpaceWeatherRegionPropertyType;
import icao.iwxxm30.SpaceWeatherRegionType;

public abstract class AbstractSpaceWeatherIWXXMParser<T> extends AbstractJAXBIWXXMParser<T, SpaceWeatherAdvisory> {
    @Override
    protected SpaceWeatherAdvisory createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<SpaceWeatherAdvisory> result,
            final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        SpaceWeatherAdvisoryType input;

        if (SpaceWeatherAdvisoryType.class.isAssignableFrom(source.getClass())) {
            input = (SpaceWeatherAdvisoryType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            JAXBElement<?> je = (JAXBElement<?>) source;
            if (SpaceWeatherAdvisoryType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (SpaceWeatherAdvisoryType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a SWX JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a SWX JAXB element");
        }
        SpaceWeatherAdvisoryImpl.Builder retval = SpaceWeatherAdvisoryImpl.builder();

        //Set issue time
        String issueTimeS  = input.getIssueTime().getTimeInstant().getTimePosition().getValue().get(0);
        PartialOrCompleteTimeInstant.Builder issueTime = PartialOrCompleteTimeInstant.builder().setCompleteTime(ZonedDateTime.parse(issueTimeS));
        retval.setIssueTime(issueTime.build());


        //Set issuing center name
        String issuingCenter = input.getIssuingSpaceWeatherCentre().getUnit().getTimeSlice().get(0).getUnitTimeSlice().getUnitName().getValue();
        retval.setIssuingCenterName(issuingCenter);

        //Set advisory number
        AdvisoryNumber advisoryNumber = parseAdvisoryNumber(input.getAdvisoryNumber().getValue());
        retval.setAdvisoryNumber(advisoryNumber);

        //set advisory number to be replaced
        //TODO: check if exists
        AdvisoryNumber replaceAdvisoryNumber = parseAdvisoryNumber(input.getReplacedAdvisoryNumber());
        retval.setReplaceAdvisoryNumber(replaceAdvisoryNumber);

        //Set list of expected phenomena
        List<String> phenomena = parsePhenomenonList(input.getPhenomenon());
        retval.addAllPhenomena(phenomena);

        //Set analysis
        List<SpaceWeatherAdvisoryAnalysis> analyses = parseAnalysisList(input.getAnalysis(), refCtx);
        retval.addAllAnalyses(analyses);

        //Set remarks
        String remark = input.getRemarks().getValue();
        retval.setRemarks(Arrays.asList(remark));

        TimeInstantPropertyType nextAdvisory = input.getNextAdvisoryTime();
        NextAdvisoryImpl.Builder na = NextAdvisoryImpl.builder();
        if(nextAdvisory.getNilReason().get(0).equals("http://codes.wmo.int/common/nil/inapplicable")) {
            na.setTime(Optional.empty());
            na.setTimeSpecifier(NextAdvisory.Type.NO_FURTHER_ADVISORIES);
        }
        retval.setNextAdvisory(na.build());

        return retval.build();
    }

    private AdvisoryNumber parseAdvisoryNumber(String advisoryNumber) {
        List<Integer> i = new ArrayList<>();
        for (String element : advisoryNumber.split("/")) {
            i.add(Integer.parseInt(element));
        }

        return AdvisoryNumberImpl.builder().setYear(i.get(0)).setSerialNumber(i.get(1)).build();

    }

    private List<String> parsePhenomenonList(List<SpaceWeatherPhenomenaType> elements) {
        List<String> phenomena = new ArrayList<>();
        for (SpaceWeatherPhenomenaType element : elements) {
            phenomena.add(element.getHref());
        }

        return phenomena;
    }

    private List<SpaceWeatherAdvisoryAnalysis> parseAnalysisList(final List<SpaceWeatherAnalysisPropertyType> elements,
            final ReferredObjectRetrievalContext refCtx) {
        List<SpaceWeatherAdvisoryAnalysis> analyses = new ArrayList<>();
        for(SpaceWeatherAnalysisPropertyType spaceWeatherAnalysisElement : elements) {

            //Declare SWX Analysis
            SpaceWeatherAdvisoryAnalysisImpl.Builder analysis = SpaceWeatherAdvisoryAnalysisImpl.builder();

            //Declare phenomenon
            PhenomenonGeometryWithHeightImpl.Builder phenomenon = new PhenomenonGeometryWithHeightImpl.Builder();

            //Set phenomenon time
            SpaceWeatherAnalysisType spaceWeatherAnalysis = spaceWeatherAnalysisElement.getSpaceWeatherAnalysis();
            TimeInstantType timeInstantType = (TimeInstantType) spaceWeatherAnalysis.getPhenomenonTime().getAbstractTimeObject().getValue();
            PartialOrCompleteTimeInstant.Builder analysisTime = PartialOrCompleteTimeInstant.builder()
                    .setCompleteTime(ZonedDateTime.parse(timeInstantType.getTimePosition().getValue().get(0)));
            phenomenon.setTime(analysisTime.build());

            //TODO: Get Regions (List)
            List<SpaceWeatherRegion> regions = new ArrayList<>();
            String nilReason = new String();
            for (SpaceWeatherRegionPropertyType regionProperty : spaceWeatherAnalysis.getRegion()) {
                SpaceWeatherRegion region = null;
                if (regionProperty.getHref() != null) {
                    //TODO: Add refcontext to method and retrieve item with id
                    Optional<SpaceWeatherRegionType> optional = refCtx.getReferredObject(regionProperty.getHref(), SpaceWeatherRegionType.class);
                    if (optional.isPresent()) {
                        //region = optional.get();
                        parseSpaceWeatherRegion(optional.get());
                    } else {
                        //TODO: add error no content
                        continue;
                    }
                } else if(regionProperty.getNilReason().size() > 0){
                    QName regionQ = new QName("region");
                    nilReason = regionProperty.getNilReason().get(0);
                    continue;
                } else {
                    region = parseSpaceWeatherRegion(regionProperty.getSpaceWeatherRegion());
                }
                regions.add(region);
            }

            analysis.setRegion(regions);
            //Set time indicator value
            if (spaceWeatherAnalysis.getTimeIndicator().value().toUpperCase().equals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION.toString())) {
                analysis.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
            } else {
                analysis.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
            }

            if(nilReason != null && !nilReason.trim().equals("")) {
                if(nilReason.equals("http://codes.wmo.int/common/nil/nothingOfOperationalSignificance")) {
                    analysis.setNoPhenomenaExpected(true);
                    analysis.setNoInformationAvailable(false);
                } else if(nilReason.equals("a")) {
                    //TODO: check if there are more reasons
                    analysis.setNoPhenomenaExpected(false);
                    analysis.setNoInformationAvailable(true);
                }
                else {
                    //TODO: Add error
                }
            } else {
                analysis.setNoPhenomenaExpected(false);
                analysis.setNoInformationAvailable(false);
            }



            analyses.add(analysis.build());
        }


        return analyses;
    }

    private SpaceWeatherRegionImpl parseSpaceWeatherRegion(SpaceWeatherRegionType region) {
        SpaceWeatherRegionImpl.Builder regionItem = SpaceWeatherRegionImpl.builder();
        //TODO: geographic location
        PhenomenonGeometryWithHeightImpl.Builder geometryWithHeight = new PhenomenonGeometryWithHeightImpl.Builder();
        PolygonPatchType polygonPatchType = (PolygonPatchType) region.getGeographicLocation()
                .getAirspaceVolume()
                .getHorizontalProjection()
                .getSurface()
                .getValue()
                .getPatches()
                .getValue()
                .getAbstractSurfacePatch()
                .get(0)
                .getValue();
        LinearRingType linearRingType = (LinearRingType) polygonPatchType.getExterior().getAbstractRing().getValue();
        List<List<Double>> latlonPairs = processPosList(linearRingType.getPosList().getValue());

        geometryWithHeight.setGeometry(
                TacOrGeoGeometryImpl.builder().setGeoGeometry(PolygonsGeometryImpl.builder().setPolygons(latlonPairs).build()).build());
        //TODO: separate poslist into lat lon pairs
        //TODO: location indicators
        regionItem.setLocationIndicator(region.getLocationIndicator().getHref());

        regionItem.setGeographiclocation(geometryWithHeight.build());

        return regionItem.build();
    }


    private List<List<Double>> processPosList(List<Double> posList) {
        List<List<Double>> pairList = new ArrayList<>();

         for(int i = 0; i < posList.size(); i++) {
             //TODO: check list size is even
             List<Double> latlonPair = new ArrayList<>();
             latlonPair.add(posList.get(i));
             i = i + 1;
             latlonPair.add(posList.get(i));
             pairList.add(latlonPair);
         }


        return pairList;
    }
}
