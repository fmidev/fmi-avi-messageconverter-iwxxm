package fi.fmi.avi.converter.iwxxm.generic;

/**
 * Fields that can be extracted from IWXXM messages via XPath.
 * Each field has a {@link Type} that determines how the XPath result should be processed.
 */
public enum IWXXMField {
    ISSUE_TIME(Type.TEXT),
    OBSERVATION_TIME(Type.TEXT),
    VALID_TIME(Type.NODE),
    AERODROME(Type.NODE),
    ORIGINATING_MWO(Type.NODE),
    ISSUING_ATS_UNIT(Type.NODE),
    ISSUING_ATS_REGION(Type.NODE),
    ISSUING_CENTRE(Type.NODE);

    private final Type type;

    IWXXMField(final Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    /**
     * Determines how XPath results for a field should be processed.
     */
    public enum Type {
        /**
         * XPath returns text content (use {@link XPathBuilder#text})
         */
        TEXT,
        /**
         * XPath returns element node for further queries (use {@link XPathBuilder#node})
         */
        NODE
    }
}
