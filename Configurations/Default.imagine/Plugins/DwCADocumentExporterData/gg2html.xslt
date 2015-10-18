<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:strip-space elements="*"/>
	<xsl:template match="treatment-test">
		<xsl:copy-of select="."/>
	</xsl:template>
	<xsl:template match="treatment">
		<html><body><div><xsl:apply-templates select=".//paragraph"/></div></body></html>
	</xsl:template>
	
	<xsl:template match="paragraph[./parent::caption]"><!--
		--><p><xsl:apply-templates/></p><!--
	--></xsl:template>
	
	<xsl:template match="paragraph[./caption]"><!--
		--><p><xsl:apply-templates/></p><!--
	--></xsl:template>
	
	<xsl:template match="caption/externalLink"/>
	
	<xsl:template match="paragraph"><!--
		--><p><xsl:apply-templates/></p><!--
	--></xsl:template>
	
	<xsl:template match="paragraph/table">
		<table>
			<xsl:for-each select=".//tr"><tr>
					<xsl:for-each select=".//td"><td><xsl:if test="./@colspan">
						<xsl:attribute name="colspan"><xsl:value-of select="./@colspan"/></xsl:attribute>
					</xsl:if>
					<xsl:if test="./@rowspan">
						<xsl:attribute name="rowspan"><xsl:value-of select="./@rowspan"/></xsl:attribute>
					</xsl:if><xsl:apply-templates/></td></xsl:for-each>
				</tr></xsl:for-each>
		</table>
	</xsl:template>
	
	<xsl:template match="taxonomicName">
		<xsl:choose>
			<xsl:when test="./@_query_"><a>
				<xsl:attribute name="title">Search Plazi for '<xsl:value-of select="."/>'</xsl:attribute>
				<xsl:attribute name="href">http://plazi.cs.umb.edu/GgServer/search?taxonomicName.isNomenclature=true<xsl:text disable-output-escaping="yes">&amp;</xsl:text>taxonomicName.exactMatch=true<xsl:text disable-output-escaping="yes">&amp;</xsl:text>taxonomicName.taxonomicName=<xsl:value-of select="translate(., ' ', '+')"/></xsl:attribute>
				<xsl:value-of select="."/>
			</a></xsl:when>
			<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
		</xsl:choose>
		<xsl:if test="./@LSID-HNS"><span><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><sup><a><xsl:attribute name="title">Lookup '<xsl:value-of select="."/>' at Hymenoptera Name Server</xsl:attribute><xsl:attribute name="href">http://osuc.biosci.ohio-state.edu/hymenoptera/nomenclator.lsid_entry?lsid=<xsl:choose>
				<xsl:when test="starts-with(./@LSID-HNS, 'urn:lsid:')"><xsl:value-of select="./@LSID-HNS"/></xsl:when>
				<xsl:otherwise>urn:lsid:biosci.ohio-state.edu:osuc_concepts:<xsl:value-of select="./@LSID-HNS"/></xsl:otherwise>
			</xsl:choose></xsl:attribute><xsl:text disable-output-escaping="yes">HNS</xsl:text></a></sup></span></xsl:if>
		<xsl:if test="./@LSID-ZBK"><span><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><sup><a><xsl:attribute name="title">Lookup '<xsl:value-of select="."/>' at ZooBank</xsl:attribute><xsl:attribute name="href">http://zoobank.org/?lsid=<xsl:choose>
				<xsl:when test="starts-with(./@LSID-ZBK, 'urn:lsid:')"><xsl:value-of select="./@LSID-ZBK"/></xsl:when>
				<xsl:otherwise>urn:lsid:zoobank.org:act:<xsl:value-of select="./@LSID-ZBK"/></xsl:otherwise>
			</xsl:choose></xsl:attribute><xsl:text disable-output-escaping="yes">ZBK</xsl:text></a></sup></span></xsl:if>
	</xsl:template>
	
	<xsl:template match="taxonomicNameLabel"><xsl:value-of select="."/></xsl:template>
	
	<xsl:template match="location">
		<xsl:choose>
			<xsl:when test="./@longitude and ./@latitude"><a>
				<xsl:attribute name="title">Search Plazi for locations around (long <xsl:value-of select="./@longitude"/>/lat <xsl:value-of select="./@latitude"/>)</xsl:attribute>
				<xsl:attribute name="href">http://plazi.cs.umb.edu/GgServer/search?materialsCitation.longitude=<xsl:value-of select="./@longitude"/><xsl:text disable-output-escaping="yes">&amp;</xsl:text>materialsCitation.latitude=<xsl:value-of select="./@latitude"/></xsl:attribute>
				<xsl:value-of select="."/>
			</a></xsl:when>
			<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="materialsCitation"><xsl:apply-templates/><!-- xsl:for-each select="./externalLink">
		<span><a>
				<xsl:attribute name="title"><xsl:value-of select="./@title"/></xsl:attribute>
				<xsl:attribute name="target">_blank</xsl:attribute>
				<xsl:choose>
					<xsl:when test="./@href">
						<xsl:attribute name="href"><xsl:value-of select="./@href"/></xsl:attribute>
						<xsl:attribute name="target">_blank</xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="onclick"><xsl:value-of select="./@onclick"/></xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:choose>
					<xsl:when test="./@iconImage"><img>
							<xsl:attribute name="alt"><xsl:value-of select="./@lable"/></xsl:attribute>
							<xsl:attribute name="src"><xsl:value-of select="./@iconImage"/></xsl:attribute>
					</img></xsl:when>
					<xsl:otherwise><xsl:value-of select="./@label"/></xsl:otherwise>
				</xsl:choose>
		</a></span>
		</xsl:for-each --></xsl:template>
	
	<xsl:template match="normalizedToken"><xsl:value-of select="./@originalValue"/></xsl:template>
	
	<xsl:template match="pageBreakToken"><xsl:apply-templates/></xsl:template>
	
	<xsl:template match="text()"><xsl:if test="string-length(substring-before(., normalize-space(.))) > 0"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text></xsl:if><xsl:value-of select="normalize-space(.)"/><xsl:if test="string-length(substring-after(., normalize-space(.))) > 0"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text></xsl:if></xsl:template>
	
	<xsl:template match="materialsCitation/externalLink"/>
	
	<xsl:template match="*"><xsl:apply-templates/></xsl:template>
	
	<!--xsl:template match="externalLink"><a>
		<xsl:attribute name="title"><xsl:value-of select="./@title"/></xsl:attribute>
		<xsl:attribute name="target">_blank</xsl:attribute>
		<xsl:choose>
			<xsl:when test="./@href">
				<xsl:attribute name="href"><xsl:value-of select="./@href"/></xsl:attribute>
				<xsl:attribute name="target">_blank</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="onclick"><xsl:value-of select="./@onclick"/></xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:choose>
			<xsl:when test="./@iconImage">
				<img>
					<xsl:attribute name="alt"><xsl:value-of select="./@lable"/></xsl:attribute>
					<xsl:attribute name="src"><xsl:value-of select="./@iconImage"/></xsl:attribute>
				</img>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="./@label"/>
			</xsl:otherwise>
		</xsl:choose>
	</a></xsl:template-->
	
</xsl:stylesheet>
