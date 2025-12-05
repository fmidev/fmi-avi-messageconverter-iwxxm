package fi.fmi.avi.converter.iwxxm.v2025_2;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511FullSerializer;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolverAixm511Full;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import icao.iwxxm2025_2.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;

public abstract class AbstractIWXXM20252Serializer<T extends AviationWeatherMessageOrCollection, S> extends AbstractIWXXMAixm511FullSerializer<T, S> {

    private static final String IWXXM_2025_2_SCHEMA_PATH = "/int/icao/iwxxm/2025_2/iwxxm.xsd";
    private static final String IWXXM_2025_2_SCHEMATRON_RULE_PATH = "/schematron/xslt/int/icao/iwxxm/2025_2/rule/iwxxm.xsl";
    private static IWXXMNamespaceContext nsCtx;

    private static synchronized IWXXMNamespaceContext getNSContext() {
        if (nsCtx == null) {
            nsCtx = new IWXXMNamespaceContext();
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/2.1", "iwxxm21");
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/3.0", "iwxxm30");
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/2023-1", "iwxxm2023_1");
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/2025-2", "iwxxm");
        }
        return nsCtx;
    }

    protected static <E> E getFirstOrNull(final List<E> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    protected static void setReportCommonMetadata(
            final AviationWeatherMessage source,
            final ConversionResult<?> results,
            final ReportType target
    ) throws ConversionException {
        try {
            final DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            target.setReportStatus(mapReportStatus(source.getReportStatus()));
            setPermissibleUsageMetadata(source, target, results);
            setTranslationMetadata(source, target, datatypeFactory, results);
        } catch (final DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting message metadata", e);
        }
    }

    private static ReportStatusType mapReportStatus(final AviationWeatherMessage.ReportStatus reportStatus) {
        switch (reportStatus) {
            case AMENDMENT:
                return ReportStatusType.AMENDMENT;
            case CORRECTION:
                return ReportStatusType.CORRECTION;
            case NORMAL:
                return ReportStatusType.NORMAL;
            default:
                throw new IllegalArgumentException("Unknown report status: " + reportStatus);
        }
    }

    private static void setPermissibleUsageMetadata(
            final AviationWeatherMessage source,
            final ReportType target,
            final ConversionResult<?> results) {
        if (source.getPermissibleUsage().isPresent()) {
            final AviationCodeListUser.PermissibleUsage usage = source.getPermissibleUsage().get();
            if (usage == AviationCodeListUser.PermissibleUsage.OPERATIONAL) {
                target.setPermissibleUsage(PermissibleUsageType.OPERATIONAL);
            } else {
                target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
                source.getPermissibleUsageReason().ifPresent(reason ->
                        target.setPermissibleUsageReason(PermissibleUsageReasonType.valueOf(reason.name())));
                source.getPermissibleUsageSupplementary().ifPresent(target::setPermissibleUsageSupplementary);
            }
        } else {
            results.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "PermissibleUsage is required"));
        }
    }

    private static void setTranslationMetadata(
            final AviationWeatherMessage source,
            final ReportType target,
            final DatatypeFactory datatypeFactory,
            final ConversionResult<?> results) {
        if (source.isTranslated()) {
            source.getTranslatedBulletinID().ifPresent(target::setTranslatedBulletinID);
            source.getTranslatedBulletinReceptionTime()
                    .map(time -> datatypeFactory.newXMLGregorianCalendar(toIWXXMDateTime(time)))
                    .ifPresent(target::setTranslatedBulletinReceptionTime);
            source.getTranslationCentreDesignator().ifPresent(target::setTranslationCentreDesignator);
            source.getTranslationCentreName().ifPresent(target::setTranslationCentreName);
            source.getTranslationTime()
                    .map(time -> datatypeFactory.newXMLGregorianCalendar(toIWXXMDateTime(time)))
                    .ifPresent(target::setTranslationTime);
            if (results.getStatus() != ConversionResult.Status.SUCCESS) {
                source.getTranslatedTAC().ifPresent(target::setTranslationFailedTAC);
            }
        }
    }

    @Override
    public XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(IWXXMSchemaResourceResolverAixm511Full.getInstance(), F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(SpaceWeatherAdvisoryType.class.getResource(IWXXM_2025_2_SCHEMA_PATH));
        schemaInfo.addSchematronRule(SpaceWeatherAdvisoryType.class.getResource(IWXXM_2025_2_SCHEMATRON_RULE_PATH));
        schemaInfo.addSchemaLocation("http://icao.int/iwxxm/2025-2", "http://schemas.wmo.int/iwxxm/2025-2/iwxxm.xsd");
        return schemaInfo;
    }

    @Override
    protected IWXXMNamespaceContext getNamespaceContext() {
        return getNSContext();
    }

}
