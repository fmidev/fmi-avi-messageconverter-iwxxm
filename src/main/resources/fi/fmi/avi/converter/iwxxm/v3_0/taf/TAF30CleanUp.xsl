<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:gml="http://www.opengis.net/gml/3.2"
  xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
  version="2.0">
  <xsl:template match="@*">
    <xsl:copy>
      <xsl:apply-templates select="../@*" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>