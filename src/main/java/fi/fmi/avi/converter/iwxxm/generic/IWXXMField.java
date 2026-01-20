package fi.fmi.avi.converter.iwxxm.generic;

/**
 * Fields that can be extracted from IWXXM messages via XPath.
 * Each field has a {@link DataType} that determines how the XPath result should be processed.
 */
public enum IWXXMField {
    ISSUE_TIME(DataType.STRING),
    OBSERVATION_TIME(DataType.STRING),
    VALID_TIME(DataType.NODE),
    AERODROME(DataType.NODE),
    ORIGINATING_MWO(DataType.NODE),
    ISSUING_ATS_UNIT(DataType.NODE),
    ISSUING_ATS_REGION(DataType.NODE),
    ISSUING_CENTRE(DataType.NODE);

    private final DataType xpathDataType;

    IWXXMField(final DataType xpathDataType) {
        this.xpathDataType = xpathDataType;
    }

    public DataType getXPathDataType() {
        return xpathDataType;
    }

    /**
     * Determines how XPath results for a field should be processed.
     */
    public enum DataType {
        /**
         * XPath returns text content (use {@link XPathBuilder#text})
         */
        STRING,
        /**
         * XPath returns element node for further queries (use {@link XPathBuilder#node})
         */
        NODE
    }
}
