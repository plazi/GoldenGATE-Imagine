<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output omit-xml-declaration="yes"/>
	<xsl:template match="countries">
		<xsl:apply-templates select="./country"/>
	</xsl:template>
	<xsl:template match="country">
		<xsl:value-of select="./@name"/>
		<xsl:text>&#x0A;</xsl:text>
		<xsl:for-each select="./name">
			<xsl:choose>
				<xsl:when test="contains(./@languages, 'German')">
					<xsl:value-of select="./@normalized"/>
					<xsl:text>&#x0A;</xsl:text>
				</xsl:when>
				<xsl:when test="contains(./@languages, 'French')">
					<xsl:value-of select="./@normalized"/>
					<xsl:text>&#x0A;</xsl:text>
				</xsl:when>
				<xsl:when test="contains(./@languages, 'Italian')">
					<xsl:value-of select="./@normalized"/>
					<xsl:text>&#x0A;</xsl:text>
				</xsl:when>
				<xsl:when test="contains(./@languages, 'Spanish')">
					<xsl:value-of select="./@normalized"/>
					<xsl:text>&#x0A;</xsl:text>
				</xsl:when>
				<xsl:when test="contains(./@languages, 'Portuguese')">
					<xsl:value-of select="./@normalized"/>
					<xsl:text>&#x0A;</xsl:text>
				</xsl:when>
				<xsl:otherwise/>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
