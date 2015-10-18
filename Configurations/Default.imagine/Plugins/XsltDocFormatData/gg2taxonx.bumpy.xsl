<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:tax="http://www.taxonx.org/schema/v1" xmlns:dc="http://digir.net/schema/conceptual/darwin/2003/1.0" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>
	<xsl:strip-space elements="*"/>
	<!--xsl:template match="/">
		<tax:taxonx xsi:schemaLocation="http://www.taxonx.org/schema/v1 http://www.taxonx.org/schema/v1/taxonx1.xsd http://www.loc.gov/mods/v3 http://www.loc.gov/mods/v3/mods-3-1.xsd http://digir.net/schema/conceptual/darwin/2003/1.0 http://digir.net/schema/conceptual/darwin/2003/1.0/darwin2.xsd">
			<xsl:apply-templates select=".//mods:mods"/>
			<tax:taxonxBody>
				<!- -xsl:apply-templates select="//*[parent::document][name() != 'mods:mods']"/- ->
				<xsl:apply-templates select="*[name() != 'mods:mods']"/>
			</tax:taxonxBody>
		</tax:taxonx>
	</xsl:template>
	<xsl:template match="mods:mods">
		<tax:taxonxHeader>
			<xsl:copy-of select="."/>
		</tax:taxonxHeader>
	</xsl:template-->
	<xsl:template match="/">
		<xsl:apply-templates select="descendant-or-self::document"/>
	</xsl:template>
	<xsl:template match="document">
		<tax:taxonx xsi:schemaLocation="http://www.taxonx.org/schema/v1 http://www.taxonx.org/schema/v1/taxonx1.xsd http://www.loc.gov/mods/v3 http://www.loc.gov/mods/v3/mods-3-1.xsd http://digir.net/schema/conceptual/darwin/2003/1.0 http://digir.net/schema/conceptual/darwin/2003/1.0/darwin2.xsd">
			<tax:taxonxHeader>
				<xsl:apply-templates select="./mods:mods"/>
			</tax:taxonxHeader>
			<tax:taxonxBody>
				<xsl:apply-templates select="./*[name() != 'mods:mods']"/>
			</tax:taxonxBody>
		</tax:taxonx>
	</xsl:template>
	<xsl:template match="mods:mods">
		<xsl:copy-of select="."/>
	</xsl:template>
	<xsl:template match="treatment">
		<tax:treatment>
			<xsl:apply-templates/>
		</tax:treatment>
	</xsl:template>
	<xsl:template match="subSection">
		<xsl:choose>
			<xsl:when test="@type = 'document_head'">
				<tax:div>
					<xsl:attribute name="type">multiple</xsl:attribute>
					<xsl:apply-templates/>
				</tax:div>
			</xsl:when>
			<xsl:when test="@type = 'reference_group'">
				<tax:ref_group>
					<xsl:apply-templates/>
				</tax:ref_group>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="tax:div">
					<xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="subSubSection">
		<xsl:choose>
			<xsl:when test="@type = 'nomenclature'">
				<tax:nomenclature>
					<xsl:apply-templates/>
				</tax:nomenclature>
			</xsl:when>
			<xsl:when test="@type = 'document_head'">
				<tax:head>
					<xsl:apply-templates/>
				</tax:head>
			</xsl:when>
			<xsl:when test="@type = 'reference_group'">
				<tax:ref_group>
					<xsl:apply-templates/>
				</tax:ref_group>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="tax:div">
					<xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="paragraph">
		<xsl:choose>
			<xsl:when test="parent::subSubSection[@type = 'document_head']">
				<xsl:apply-templates/>
			</xsl:when>
			<xsl:when test="parent::subSubSection[@type = 'nomenclature']">
				<xsl:apply-templates/>
			</xsl:when>
			<xsl:when test="name(child::*) = 'footnote'">
				<xsl:apply-templates/>
			</xsl:when>
			<xsl:when test="name(child::*) = 'caption'">
				<xsl:apply-templates/>
			</xsl:when>
			<xsl:otherwise>
				<tax:p>
					<xsl:apply-templates/>
				</tax:p>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="taxonomicName">
		<xsl:element name="tax:name">
			<xsl:choose>
				<xsl:when test="@*">
					<xsl:if test="@LSID-HNS">
						<xsl:element name="tax:xid">
							<xsl:attribute name="identifier"><xsl:value-of select="@LSID-HNS"/></xsl:attribute>
							<xsl:attribute name="source">HNS</xsl:attribute>
						</xsl:element>
					</xsl:if>
					<xsl:if test="@lsidName-HNS and (@genus or @rank='family')">
						<xsl:element name="tax:xmldata">
							<xsl:choose>
								<xsl:when test="@rank = 'family'">
									<dc:Family>
										<xsl:value-of select="text()"/>
									</dc:Family>
								</xsl:when>
								<!--xsl:when test="@rank = 'subFamily'">
                                    <dc:Subfamily>
                                        <xsl:value-of select="text()"/>
                                    </dc:Subfamily>
                                </xsl:when-->
								<!--xsl:when test="@rank = 'tribe'">
                                    <dc:Tribe>
                                        <xsl:value-of select="text()"/>
                                    </dc:Tribe>
                                </xsl:when-->
								<xsl:when test="@genus">
									<dc:Genus>
										<xsl:value-of select="@genus"/>
									</dc:Genus>
									<xsl:if test="@species">
										<dc:Species>
											<xsl:value-of select="@species"/>
										</dc:Species>
									</xsl:if>
									<xsl:if test="@subSpecies">
										<dc:Subspecies>
											<xsl:value-of select="@subSpecies"/>
										</dc:Subspecies>
									</xsl:if>
								</xsl:when>
								<xsl:when test="@species">
									<dc:Species>
										<xsl:value-of select="@species"/>
									</dc:Species>
									<xsl:if test="@subSpecies">
										<dc:Subspecies>
											<xsl:value-of select="@subSpecies"/>
										</dc:Subspecies>
									</xsl:if>
								</xsl:when>
							</xsl:choose>
						</xsl:element>
					</xsl:if>
				</xsl:when>
			</xsl:choose>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="materialsCitation">
		<xsl:if test=".//pageStartToken">
			<xsl:element name="tax:pb">
				<xsl:attribute name="n"><xsl:value-of select=".//pageStartToken/@pageNumber"/></xsl:attribute>
			</xsl:element>
		</xsl:if>
		<xsl:element name="tax:collection_event">
			<xsl:if test="./@longitude or ./@latitude or ./@country or ./@location or ./@collectingDate or ./@collectorName or ./@stateProvince or ./@typeStatus">
				<xsl:element name="tax:xmldata">
					
					<xsl:if test="./@longitude">
						<xsl:element name="dc:DecimalLongitude">
							<xsl:value-of select="./@longitude"/>
						</xsl:element>
					</xsl:if>
					<xsl:if test="./@latitude">
						<xsl:element name="dc:DecimalLatitude">
							<xsl:value-of select="./@latitude"/>
						</xsl:element>
					</xsl:if>
					
					<xsl:if test="./@country">
						<xsl:element name="dc:Country">
							<xsl:value-of select="./@country"/>
						</xsl:element>
					</xsl:if>
					<xsl:if test="./@stateProvince">
						<xsl:element name="dc:StateProvince">
							<xsl:value-of select="./@stateProvince"/>
						</xsl:element>
					</xsl:if>
					<xsl:if test="./@location">
						<xsl:element name="dc:Locality">
							<xsl:value-of select="./@location"/>
						</xsl:element>
					</xsl:if>
					
					<xsl:if test="./@collectingDate">
						<xsl:element name="dc:YearCollected">
							<xsl:value-of select="substring(./@collectingDate, 1, 4)"/>
						</xsl:element>
						<xsl:element name="dc:MonthCollected">
							<xsl:value-of select="substring(./@collectingDate, 6, 2)"/>
						</xsl:element>
						<xsl:element name="dc:DayCollected">
							<xsl:value-of select="substring(./@collectingDate, 9, 2)"/>
						</xsl:element>
					</xsl:if>
					
					<xsl:if test="./@collectorName">
						<xsl:element name="dc:Collector">
							<xsl:value-of select="./@collectorName"/>
						</xsl:element>
					</xsl:if>
					<xsl:if test="./@typeStatus">
						<xsl:element name="dc:TypeStatus">
							<xsl:value-of select="./@typeStatus"/>
						</xsl:element>
					</xsl:if>
				</xsl:element>
			</xsl:if>
			<xsl:value-of select="normalize-space()"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="//*[ancestor::materialsCitation]">
		<xsl:choose>
			<xsl:when test="name() = 'pageStartToken'">
				<xsl:apply-templates select="."/>
			</xsl:when>
			<xsl:when test="./pageStartToken">
				<xsl:apply-templates select="./pageStartToken"/>
				<xsl:value-of select="text()"/>
			</xsl:when>
			<xsl:when test="./child::*">
				<xsl:apply-templates select="./child::*"/>
				<xsl:value-of select="text()"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="text()"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="pageStartToken">
		<xsl:element name="tax:pb"><xsl:attribute name="n"><xsl:value-of select="./@pageNumber"/></xsl:attribute></xsl:element>
		<xsl:value-of select="text()"/>
	</xsl:template>
	<xsl:template match="normalizedToken">
		<xsl:value-of select="./@originalValue"/>
	</xsl:template>
	<!--xsl:template match="author">
		<tax:author>
			<xsl:apply-templates/>
		</tax:author>
	</xsl:template>
	<xsl:template match="title">
		<tax:title>
			<xsl:apply-templates/>
		</tax:title>
	</xsl:template-->
	
	<!--xsl:template match="citation">
		<tax:bibref>
			<xsl:apply-templates/>
		</tax:bibref>
	</xsl:template>
	<xsl:template match="//*[parent::citation]">
		<xsl:choose>
			<xsl:when test="name() = 'pageStartToken'">
				<xsl:apply-templates select="."/>
			</xsl:when>
			<xsl:when test="./pageStartToken">
				<xsl:apply-templates select="./pageStartToken"/>
				<xsl:value-of select="text()"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="text()"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template-->
	
	<xsl:template match="citation">
		<tax:bibref>
			<xsl:if test=".//pageStartToken">
				<xsl:element name="tax:pb">
					<xsl:attribute name="n"><xsl:value-of select=".//pageStartToken/@pageNumber"/></xsl:attribute>
				</xsl:element>
			</xsl:if>
			<xsl:value-of select="normalize-space()"/>
		</tax:bibref>
	</xsl:template>
	
	<xsl:template match="footnote">
		<xsl:element name="tax:note">
			<xsl:attribute name="place">foot</xsl:attribute>
			<xsl:choose>
				<xsl:when test="./text()">
					<tax:p>
						<xsl:apply-templates/>
					</tax:p>
				</xsl:when>
				<xsl:otherwise>
					<xsl:apply-templates/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:element>
	</xsl:template>
	<xsl:template match="caption">
		<tax:figure>
			<xsl:choose>
				<xsl:when test="./paragraph">
					<xsl:apply-templates/>
				</xsl:when>
				<xsl:otherwise>
					<tax:p>
						<xsl:apply-templates/>
					</tax:p>
				</xsl:otherwise>
			</xsl:choose>
		</tax:figure>
	</xsl:template>
</xsl:stylesheet>
