<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:gml="http://www.opengis.net/gml/3.2"
  xmlns:om="http://www.opengis.net/om/2.0"
  xmlns:iwxxm="http://icao.int/iwxxm/2.1"
  version="1.0">
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="//gml:boundedBy[@xsi:nil='true']"/>
    <xsl:template match="//om:result[@xsi:type='iwxxm:MeteorologicalAerodromeForecastRecordPropertyType']/@xsi:type"/>
</xsl:stylesheet>