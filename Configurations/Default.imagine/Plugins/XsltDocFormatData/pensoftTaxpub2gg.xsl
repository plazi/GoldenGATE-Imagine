<?xml version="1.0" encoding="UTF-8"?>
<!-- gg2taxonx.xsl
    2007-10-30
    Terry Catapano
-->
<xsl:stylesheet 
xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" 
xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:tp="http://www.plazi.org/taxpub" 
xmlns:tax="http://www.taxonx.org/schema/v1" 
xmlns:dwc="http://digir.net/schema/conceptual/darwin/2003/1.0" 
xmlns:mods="http://www.loc.gov/mods/v3">
	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>
	
	<xsl:template match="/">
		<document>
			<xsl:apply-templates/>
		</document>
	</xsl:template>
	
	<xsl:template match="front">
		<mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
			<mods:titleInfo>
				<mods:title>
					<xsl:value-of select="normalize-space(./article-meta/title-group/article-title)"/>
				</mods:title>
			</mods:titleInfo>
			<xsl:for-each select="./article-meta/contrib-group/contrib/name">
				<mods:name type="personal">
					<mods:role>
						<mods:roleTerm>Author</mods:roleTerm>
					</mods:role>
					<mods:namePart>
						<xsl:value-of select="./surname"/>
						<xsl:text disable-output-escaping="yes">, </xsl:text>
						<xsl:value-of select="./given-names"/>
					</mods:namePart>
				</mods:name>
			</xsl:for-each>
			<mods:typeOfResource>text</mods:typeOfResource>
			<mods:relatedItem type="host">
				<mods:titleInfo>
					<mods:title>ZooKeys</mods:title>
				</mods:titleInfo>
				<mods:part>
					<mods:detail type="volume">
						<mods:number>
							<xsl:value-of select="./article-meta/issue"/>
						</mods:number>
					</mods:detail>
					<mods:date>
						<xsl:value-of select="./article-meta/pub-date/year"/>
					</mods:date>
					<mods:extent unit="page">
						<mods:start>
							<xsl:value-of select="./article-meta/fpage"/>
						</mods:start>
						<mods:end>
							<xsl:value-of select="./article-meta/lpage"/>
						</mods:end>
					</mods:extent>
				</mods:part>
			</mods:relatedItem>
			<mods:location>
				<mods:url>http://dx.doi.org/<xsl:value-of select="./article-meta/article-id[./@pub-id-type = 'doi']"/>
				</mods:url>
			</mods:location>
			<mods:identifier type="ZooKeys-Pub">
				<xsl:value-of select="./journal-meta/issn[./@pub-type = 'epub']"/>-<xsl:value-of select="./article-meta/issue"/>
			</mods:identifier>
		</mods:mods>
	</xsl:template>
	
	<xsl:template match="body">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="back">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="sec[./@sec-type = 'introduction']">
		<subSection type="introduction">
			<xsl:apply-templates/>
		</subSection>
	</xsl:template>
	
	<xsl:template match="sec[./@sec-type = 'materials-methods']">
		<subSection type="materials_methods">
			<xsl:apply-templates/>
		</subSection>
	</xsl:template>
	
	<xsl:template match="sec[./@sec-type = 'appendix']"/>
	
	<xsl:template match="sec[./parent::sec]" priority="2"><!-- this one needs to overrule any of the type specific templates below -->
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="sec[./@sec-type]">
		<subSection>
			<xsl:attribute name="type"><xsl:value-of select="./@sec-type"/></xsl:attribute>
			<xsl:apply-templates/>
		</subSection>
	</xsl:template>
	
	<xsl:template match="sec[not(./@sec-type) and ./title/text()]">
		<subSection>
			<xsl:attribute name="type"><xsl:value-of select="./title/text()"/></xsl:attribute>
			<xsl:apply-templates/>
		</subSection>
	</xsl:template>
	
	<xsl:template match="sec[.//fn-group]"/>
	
	<xsl:template match="sec">
		<subSection type="multiple">
			<xsl:apply-templates/>
		</subSection>
	</xsl:template>
	
	<xsl:template match="floats-group">
		<subSection type="multiple">
			<xsl:apply-templates/>
		</subSection>
	</xsl:template>
	
	<xsl:template match="tp:taxon-treatment[./parent::sec]">
		<xsl:text disable-output-escaping="yes">&lt;/subSection&gt;</xsl:text>
		<treatment>
			<xsl:apply-templates/>
		</treatment>
		<xsl:choose>
			<xsl:when test="./parent::sec/@sec-type">
				<xsl:text disable-output-escaping="yes">&lt;subSection type=&quot;</xsl:text><xsl:value-of select="./parent::sec/@sec-type"/><xsl:text disable-output-escaping="yes">&quot;&gt;</xsl:text>
			</xsl:when>
			<xsl:when test="./parent::sec/title/text()">
				<xsl:text disable-output-escaping="yes">&lt;subSection type=&quot;</xsl:text><xsl:value-of select="./parent::sec/title/text()"/><xsl:text disable-output-escaping="yes">&quot;&gt;</xsl:text>
			</xsl:when>
			<xsl:otherwise><xsl:text disable-output-escaping="yes">&lt;subSection type=&quot;multiple&quot;&gt;</xsl:text></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="tp:taxon-treatment[./parent::tp:taxon-treatment]">
		<xsl:text disable-output-escaping="yes">&lt;/treatment&gt;</xsl:text>
		<treatment>
			<xsl:apply-templates/>
		</treatment>
		<xsl:text disable-output-escaping="yes">&lt;treatment&gt;</xsl:text>
	</xsl:template>
	
	<xsl:template match="tp:taxon-treatment">
		<treatment>
			<xsl:apply-templates/>
		</treatment>
	</xsl:template>
	
	<xsl:template match="tp:nomenclature">
		<subSubSection type="nomenclature">
			<paragraph>
				<xsl:apply-templates select="./*[name(.) != 'tp:nomenclature-citation-list']"/>
			</paragraph>
		</subSubSection>
		<xsl:if test="./tp:nomenclature-citation-list">
		<subSubSection type="reference_group">
			<xsl:apply-templates select=".//tp:nomenclature-citation"/>
		</subSubSection></xsl:if>
	</xsl:template>
	
	<xsl:template match="tp:nomenclature-citation">
		<paragraph>
			<xsl:apply-templates/>
		</paragraph>
	</xsl:template>
	
	<xsl:template match="tp:treatment-sec">
		<subSubSection>
			<xsl:attribute name="type"><xsl:value-of select="./@sec-type"/></xsl:attribute>
			<xsl:apply-templates/>
		</subSubSection>
	</xsl:template>
	
	<xsl:template match="p[not(./parent::caption) and not(./ancestor::p)]">
		<paragraph>
			<xsl:apply-templates/>
		</paragraph>
	</xsl:template>
	
	<xsl:template match="table-wrap">
		<xsl:choose>
			<xsl:when test="./label and ./caption">
				<paragraph>
					<caption>
						<xsl:value-of select="./label/text()"/><xsl:text disable-output-escaping="yes">. &#x20;</xsl:text><xsl:apply-templates select="./caption"/>
					</caption>
				</paragraph>
			</xsl:when>
			<xsl:when test="./label">
				<paragraph>
					<caption>
						<xsl:value-of select="./label/text()"/>
					</caption>
				</paragraph>
			</xsl:when>
			<xsl:when test="./caption">
				<paragraph>
					<caption>
						<xsl:apply-templates select="./caption"/>
					</caption>
				</paragraph>
			</xsl:when>
		</xsl:choose>
		<paragraph>
			<xsl:copy-of select="./table"/>
		</paragraph>
	</xsl:template>
	
	<!--xsl:template match="table">
		<xsl:copy-of select="."/>
	</xsl:template-->
	
	
	<xsl:template match="fig[./parent::p]">
		<xsl:text disable-output-escaping="yes">&lt;/paragraph&gt;</xsl:text>
		<caption>
			<paragraph>
				<xsl:value-of select="./label"/><xsl:text disable-output-escaping="yes"> </xsl:text><xsl:apply-templates select="./caption/p"/>
			</paragraph>
		</caption>
		<xsl:text disable-output-escaping="yes">&lt;paragraph continue="true"&gt;</xsl:text>
	</xsl:template>
	
	<xsl:template match="fig">
		<caption>
			<paragraph>
				<xsl:value-of select="./label"/><xsl:text disable-output-escaping="yes"> </xsl:text><xsl:apply-templates select="./caption/p"/>
			</paragraph>
		</caption>
	</xsl:template>
	
	<xsl:template match="fig-group">
		<caption>
			<paragraph>
				<xsl:apply-templates select="./caption/p"/>
				<xsl:for-each select="./fig/label"><xsl:text disable-output-escaping="yes"> </xsl:text><xsl:value-of select="."/></xsl:for-each>
			</paragraph>
		</caption>
	</xsl:template>
	
	<xsl:template match="pageBreak">
		<xsl:copy-of select="."/>
	</xsl:template>
	
	
	<xsl:template match="tp:taxon-name">
		<taxonomicName>
			<xsl:if test="./@rank"><xsl:attribute name="rank"><xsl:value-of select="./@rank"/></xsl:attribute></xsl:if>
			<xsl:if test="./object-id/@xlink:href"><xsl:attribute name="LSID"><xsl:value-of select="./object-id/@xlink:href"/></xsl:attribute></xsl:if>
			<xsl:if test="./object-id/text()"><xsl:attribute name="LSID"><xsl:value-of select="./object-id/text()"/></xsl:attribute></xsl:if>
			<xsl:apply-templates/>
		</taxonomicName>
	</xsl:template>
	
	<xsl:template match="tp:taxon-name/object-id"/>
	
	<xsl:template match="tp:taxon-name-part">
		<taxonomicNameEpithet>
			<xsl:if test="./@taxon-name-part-type"><xsl:attribute name="rank"><xsl:value-of select="./@taxon-name-part-type"/></xsl:attribute></xsl:if>
			<xsl:apply-templates/>
		</taxonomicNameEpithet>
	</xsl:template>
	
	<xsl:template match="tp:taxon-authority">
		<taxonomicNameAuthor>
			<xsl:apply-templates/>
		</taxonomicNameAuthor>
	</xsl:template>
	
	<xsl:template match="tp:taxon-status">
		<taxonomicNameLabel>
			<xsl:apply-templates/>
		</taxonomicNameLabel>
	</xsl:template>
	
	
	<xsl:template match="tp:material-citation">
		<materialsCitation>
			<xsl:apply-templates/>
		</materialsCitation>
	</xsl:template>
	
	<xsl:template match="tp:type-status">
		<typeStatus><xsl:value-of select="."/></typeStatus>
	</xsl:template>
	
	<xsl:template match="list-item[(normalize-space(substring-before(./label, ':')) = 'Type status') and (./p/bold = 'Occurrence:')]">
		<paragraph><materialsCitation><xsl:apply-templates/></materialsCitation></paragraph>
	</xsl:template>
	
	<xsl:template match="label[(normalize-space(substring-before(., ':')) = 'Type status') and (./parent::list-item/p/bold = 'Occurrence:')]">Type status: <typeStatus><xsl:value-of select="normalize-space(substring-after(., ':'))"/></typeStatus></xsl:template>
	
	<xsl:template match="p[(./bold = 'Occurrence:') and (normalize-space(substring-before(./parent::list-item/label, ':')) = 'Type status')]">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="named-content">
		<xsl:choose>
			<xsl:when test="./@content-type = 'dwc:recordedBy'"><xsl:value-of select="' '"/><collectorName><xsl:value-of select="."/></collectorName></xsl:when>
			<xsl:when test="./@content-type = 'dwc:samplingProtocol'"><xsl:value-of select="' '"/><collectingMethod><xsl:value-of select="."/></collectingMethod></xsl:when>
			<xsl:when test="./@content-type = 'dwc:individualCount'"><xsl:value-of select="' '"/><specimenCount><xsl:value-of select="."/></specimenCount></xsl:when>
			<xsl:when test="./@content-type = 'dwc:country'"><xsl:value-of select="' '"/><collectingCountry><xsl:value-of select="."/></collectingCountry></xsl:when>
			<xsl:when test="./@content-type = 'dwc:locality'"><xsl:value-of select="' '"/><location><xsl:value-of select="."/></location></xsl:when>
			<xsl:when test="./@content-type = 'dwc:decimalLongitude'"><xsl:value-of select="' '"/><xsl:element name="geoCoordinate">
				<xsl:attribute name="orientation">longitude</xsl:attribute>
				<xsl:attribute name="value"><xsl:choose>
					<xsl:when test="contains(., '°')"><xsl:value-of select="substring-before(., '°')"/></xsl:when>
					<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
				</xsl:choose></xsl:attribute>
				<xsl:value-of select="."/>
			</xsl:element></xsl:when>
			<xsl:when test="./@content-type = 'dwc:decimalLatitude'"><xsl:value-of select="' '"/><xsl:element name="geoCoordinate">
				<xsl:attribute name="orientation">latitude</xsl:attribute>
				<xsl:attribute name="value"><xsl:choose>
					<xsl:when test="contains(., '°')"><xsl:value-of select="substring-before(., '°')"/></xsl:when>
					<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
				</xsl:choose></xsl:attribute>
				<xsl:value-of select="."/>
			</xsl:element></xsl:when>
			<xsl:when test="./@content-type = 'dwc:verbatimEventDate'"><xsl:value-of select="' '"/><xsl:element name="collectingDate">
				<xsl:attribute name="value"><xsl:choose>
					<xsl:when test="./parent::*/named-content[./@content-type = 'dwc:year'] and ./parent::*/named-content[./@content-type = 'dwc:month'] and ./parent::*/named-content[./@content-type = 'dwc:day']"><xsl:value-of select="./parent::*/named-content[./@content-type = 'dwc:year']"/>-<xsl:value-of select="./parent::*/named-content[./@content-type = 'dwc:month']"/>-<xsl:value-of select="./parent::*/named-content[./@content-type = 'dwc:day']"/></xsl:when>
					<xsl:when test="./parent::*/named-content[./@content-type = 'dwc:eventDate']"><xsl:value-of select="./parent::*/named-content[./@content-type = 'dwc:eventDate']"/></xsl:when>
					<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
				</xsl:choose></xsl:attribute>
				<xsl:value-of select="."/>
			</xsl:element></xsl:when>
			<xsl:when test="./@content-type = 'dwc:eventDate'"><xsl:choose>
				<xsl:when test="./parent::*/named-content[./@content-type = 'dwc:verbatimEventDate']"><xsl:value-of select="' '"/><xsl:value-of select="."/></xsl:when>
				<xsl:otherwise><xsl:value-of select="' '"/><xsl:element name="collectingDate">
					<xsl:attribute name="value"><xsl:choose>
						<xsl:when test="./parent::*/named-content[./@content-type = 'dwc:year'] and ./parent::*/named-content[./@content-type = 'dwc:month'] and ./parent::*/named-content[./@content-type = 'dwc:day']"><xsl:value-of select="./parent::*/named-content[./@content-type = 'dwc:year']"/>-<xsl:value-of select="./parent::*/named-content[./@content-type = 'dwc:month']"/>-<xsl:value-of select="./parent::*/named-content[./@content-type = 'dwc:day']"/></xsl:when>
						<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
					</xsl:choose></xsl:attribute>
				<xsl:value-of select="."/>
			</xsl:element></xsl:otherwise>
			</xsl:choose></xsl:when>
			<xsl:when test="./@content-type = 'dwc:catalogNumber'"><xsl:value-of select="' '"/><specimenCode><xsl:value-of select="."/></specimenCode></xsl:when>
			<xsl:when test="./@content-type = 'dwc:institutionCode'"><xsl:value-of select="' '"/><collectionCode><xsl:value-of select="."/></collectionCode></xsl:when>
			<!--xsl:when test="./@content-type = 'dwc:something'"><xsl:value-of select="' '"/><ggTag><xsl:value-of select="."/></ggTag></xsl:when-->
			<!--xsl:when test="./@content-type = 'dwc:something'"><xsl:value-of select="' '"/><ggTag><xsl:value-of select="."/></ggTag></xsl:when-->
			<!--xsl:when test="./@content-type = 'dwc:something'"><xsl:value-of select="' '"/><ggTag><xsl:value-of select="."/></ggTag></xsl:when-->
			<!--xsl:when test="./@content-type = 'dwc:something'"><xsl:value-of select="' '"/><ggTag><xsl:value-of select="."/></ggTag></xsl:when-->
			<!--xsl:when test="./@content-type = 'dwc:something'"><xsl:value-of select="' '"/><ggTag><xsl:value-of select="."/></ggTag></xsl:when-->
			<!--xsl:when test="./@content-type = 'dwc:something'"><xsl:value-of select="' '"/><ggTag><xsl:value-of select="."/></ggTag></xsl:when-->
			<xsl:otherwise><xsl:value-of select="' '"/><xsl:value-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="object-id">
		<xsl:choose>
			<xsl:when test="./@content-type = 'dwc:catalogNumber'"><xsl:value-of select="' '"/><specimenCode><xsl:value-of select="."/></specimenCode></xsl:when>
			<xsl:when test="./@content-type = 'dwc:institutionCode'"><xsl:value-of select="' '"/><collectionCode><xsl:value-of select="."/></collectionCode></xsl:when>
			<!--xsl:when test="./@content-type = 'dwc:something'"><xsl:value-of select="' '"/><ggTag><xsl:value-of select="."/></ggTag></xsl:when-->
			<xsl:otherwise><xsl:value-of select="' '"/><xsl:value-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="tp:collecting-event">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="date[./parent::tp:collecting-event]">
		<collectingDate><xsl:value-of select="./string-date"/></collectingDate>
	</xsl:template>
	
	<xsl:template match="tp:collecting-location">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="tp:location">
		<xsl:choose>
			<xsl:when test="./@location-type = 'dwc:continent' and not(./following-sibling::tp:location[./@location-type = 'dwc:country'])"><xsl:value-of select="' '"/><collectingCountry><xsl:value-of select="."/></collectingCountry></xsl:when>
			<xsl:when test="./@location-type = 'dwc:country'"><xsl:value-of select="' '"/><collectingCountry><xsl:value-of select="."/></collectingCountry></xsl:when>
			<xsl:when test="./@location-type = 'dwc:province'"><xsl:value-of select="' '"/><collectingRegion><xsl:value-of select="."/></collectingRegion></xsl:when>
			<xsl:when test="./@location-type = 'dwc:stateProvince'"><xsl:value-of select="' '"/><collectingRegion><xsl:value-of select="."/></collectingRegion></xsl:when>
			<xsl:when test="./@location-type = 'dwc:locality'"><xsl:value-of select="' '"/><location><xsl:value-of select="."/></location></xsl:when>
			
			<xsl:when test="./@location-type = 'dwc:longitude'"><xsl:value-of select="' '"/><geoCoordinate orientation="longitude"><xsl:value-of select="."/></geoCoordinate></xsl:when>
			<xsl:when test="./@location-type = 'dwc:latitude'"><xsl:value-of select="' '"/><geoCoordinate orientation="latitude"><xsl:value-of select="."/></geoCoordinate></xsl:when>
			
			<xsl:when test="./@location-type = 'dwc:verbatimLongitude'"><xsl:value-of select="' '"/><geoCoordinate orientation="longitude"><xsl:value-of select="."/></geoCoordinate></xsl:when>
			<xsl:when test="./@location-type = 'dwc:verbatimLatitude'"><xsl:value-of select="' '"/><geoCoordinate orientation="latitude"><xsl:value-of select="."/></geoCoordinate></xsl:when>
			<xsl:when test="./@location-type = 'dwc:verbatimCoordinates'"><xsl:value-of select="' '"/><geoCoordinatePair><xsl:value-of select="."/></geoCoordinatePair></xsl:when>
			
			<xsl:when test="./@location-type = 'dwc:verbatimElevation'"><xsl:value-of select="' '"/><elevation><xsl:value-of select="."/></elevation></xsl:when>
			
			<xsl:when test="./@location-type = 'dwc:verbatimDate'"><xsl:value-of select="' '"/><collectingDate><xsl:value-of select="."/></collectingDate></xsl:when>
			<xsl:otherwise><xsl:value-of select="' '"/><xsl:value-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
						<!--tp:material-citation>
						<tp:collecting-event>
							<tp:collecting-location>
								<tp:location location-type="dwc:continent">SOUTH
                                    AFRICA</tp:location>
								<tp:location location-type="dwc:province">Western Cape</tp:location>
								<tp:location location-type="dwc:locality">Kogelberg Nature
                                    Reserve</tp:location>
								<tp:location location-type="dwc:latitude">34°16.481&#8217;S</tp:location>
								<tp:location location-type="dwc:longitude">19°01.033&#8217;E</tp:location>
							</tp:collecting-location>, <date>
								<string-date>16 Jan&#8211;16 Feb 2000</string-date>
							</date>
						</tp:collecting-event>, , , <named-content content-type="dwc:locationRemarks">Mesic Mountain Fynbos, last burnt c.
                            1978</named-content>, <object-id content-type="dwc:collectionCode">SAM-HYM-P025052</object-id>, <object-id content-type="dwc:catalogNumber">OSUC 256956</object-id> (<object-id content-type="dwc:institutionCode">SAMC</object-id>
					</tp:material-citation-->
	
	
	<xsl:template match="title[./parent::treatment-sec]">
		<paragraph>
			<xsl:apply-templates/>
		</paragraph>
	</xsl:template>
	
	<xsl:template match="title">
		<paragraph>
			<xsl:apply-templates/>
		</paragraph>
	</xsl:template>
	
	<xsl:template match="label[./parent::treatment-sec]">
		<paragraph>
			<xsl:apply-templates/>
		</paragraph>
	</xsl:template>
	
	<xsl:template match="xref[./@ref-type = 'bibr']">
		<bibRefCitation>
			<xsl:attribute name="citationId"><xsl:value-of select="./@rid"/></xsl:attribute>
			<xsl:apply-templates/>
		</bibRefCitation>
	</xsl:template>
	
	
	<xsl:template match="ack">
		<subSection type="acknowledgements">
			<xsl:apply-templates/>
		</subSection>
	</xsl:template>
	
	<xsl:template match="label[./parent::ack]">
		<paragraph>
			<xsl:apply-templates/>
		</paragraph>
	</xsl:template>
	
	
	<xsl:template match="ref-list">
		<subSection type="reference_group">
			<xsl:apply-templates/>
		</subSection>
	</xsl:template>
	
	<xsl:template match="ref">
		<paragraph>
			<xsl:apply-templates/>
		</paragraph>
	</xsl:template>
	
	<xsl:template match="mixed-citation">
		<bibRef>
			<xsl:attribute name="citationId"><xsl:value-of select="./parent::ref/@id"/></xsl:attribute>
			<xsl:choose>
				<!--mixed-citation> BOOK CHAPTER
					<person-group>
						<name>
							<surname>Rutherford</surname>
							<given-names>MC</given-names>
						</name>
						<name>
							<surname>Mucina</surname>
							<given-names>L</given-names>
						</name>
						<name>
							<surname>Powrie</surname>
							<given-names>LW</given-names>
						</name>
					</person-group> (<year>2006</year>) <article-title>Biomes and bioregions of
                        Southern Africa</article-title>. In: <name>
						<surname>Mucina</surname>
						<given-names>L</given-names>
					</name>, <name>
						<surname>Rutherford</surname>
						<given-names>MC</given-names>
					</name> (<role>Eds</role>) <source>The vegetation of South Africa, Lesotho and
                        Swaziland</source>. <publisher-name>SANBI</publisher-name>
					<publisher-loc>Pretoria</publisher-loc>,
                        <fpage>30</fpage>&#8211;<lpage>51</lpage>.</mixed-citation-->
				<xsl:when test="(./publisher-name or ./publisher-loc) and ./article-title"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group[1]"/>, <xsl:apply-templates select="./year"/>. <xsl:apply-templates select="./article-title"/>. In: <xsl:if test="./person-group[2]"><xsl:apply-templates select="./person-group[2]"/><xsl:choose>
						<xsl:when test="count(./person-group[2]/name) > 1">Eds.,</xsl:when>
						<xsl:otherwise>Ed.,</xsl:otherwise>
					</xsl:choose></xsl:if> <volumeTitle><xsl:value-of select="./source"/></volumeTitle>. <journalOrPublisher><xsl:choose>
						<xsl:when test="./publisher-name and ./publisher-loc"><xsl:value-of select="./publisher-name"/>, <xsl:value-of select="./publisher-loc"/></xsl:when>
						<xsl:when test="./publisher-name"><xsl:value-of select="./publisher-name"/></xsl:when>
						<xsl:when test="./publisher-loc"><xsl:value-of select="./publisher-loc"/></xsl:when>
					</xsl:choose></journalOrPublisher><xsl:choose>
						<xsl:when test="./fpage and ./lpage">: <pageData><xsl:value-of select="./fpage"/> - <xsl:value-of select="./lpage"/></pageData></xsl:when>
						<xsl:when test="./fpage"><pageData>: <xsl:value-of select="./fpage"/></pageData></xsl:when>
						<xsl:when test="./lpage">, <xsl:value-of select="./lpage"/> pp.</xsl:when>
					</xsl:choose>
				</xsl:when>
					<!--mixed-citation> BOOK
					<person-group>
						<name>
							<surname>Mucina</surname>
							<given-names>L</given-names>
						</name>
						<name>
							<surname>Rutherford</surname>
							<given-names>MC</given-names>
						</name>
					</person-group> (<role>Eds</role>) <year>2006</year>. <source>The vegetation of
                        South Africa, Lesotho and Swaziland</source>
					<publisher-name>SANBI</publisher-name>, <publisher-loc>Pretoria</publisher-loc>,
                        <lpage>807</lpage> pp. </mixed-citation-->
				<xsl:when test="./publisher-name or ./publisher-loc"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group"/> <xsl:apply-templates select="./year"/>. <title><xsl:value-of select="./source"/></title>. <journalOrPublisher><xsl:choose>
						<xsl:when test="./publisher-name and ./publisher-loc"><xsl:value-of select="./publisher-name"/>, <xsl:value-of select="./publisher-loc"/></xsl:when>
						<xsl:when test="./publisher-name"><xsl:value-of select="./publisher-name"/></xsl:when>
						<xsl:when test="./publisher-loc"><xsl:value-of select="./publisher-loc"/></xsl:when>
					</xsl:choose></journalOrPublisher><xsl:choose>
						<xsl:when test="./fpage and ./lpage">: <pageData><xsl:value-of select="./fpage"/> - <xsl:value-of select="./lpage"/></pageData></xsl:when>
						<xsl:when test="./fpage"><pageData>: <xsl:value-of select="./fpage"/></pageData></xsl:when>
						<xsl:when test="./lpage">, <xsl:value-of select="./lpage"/> pp.</xsl:when>
					</xsl:choose>
				</xsl:when>
				<!--mixed-citation xlink:type="simple"> JOURNAL ARTICLE IN SPECIAL ISSUE
					<person-group>
						<name name-style="western">
							<surname>Smith</surname>
							<given-names>VS</given-names>
						</name>
						<name name-style="western">
							<surname>Rycroft</surname>
							<given-names>SD</given-names>
						</name>
						<name name-style="western">
							<surname>Brake</surname>
							<given-names>I</given-names>
						</name>
						<name name-style="western">
							<surname>Scott</surname>
							<given-names>B</given-names>
						</name>
						<name name-style="western">
							<surname>Baker</surname>
							<given-names>E</given-names>
						</name>
						<name name-style="western">
							<surname>Livermore</surname>
							<given-names>L</given-names>
						</name>
						<name name-style="western">
							<surname>Blagoderov</surname>
							<given-names>V</given-names>
						</name>
						<name name-style="western">
							<surname>Roberts</surname>
							<given-names>D</given-names>
						</name>
					</person-group> (<year>2011</year>)<article-title> Scratchpads 2.0: a Virtual Research Environment supporting scholarly collaboration, communication and data publication in biodiversity science.</article-title> In: <person-group>
						<name name-style="western">
							<surname>Smith</surname>
							<given-names>V</given-names>
						</name>
						<name name-style="western">
							<surname>Penev</surname>
							<given-names>L</given-names>
						</name>
					</person-group> (<role>Eds</role>). <issue-title> e-Infrastructures for data publishing in biodiversity science.</issue-title>
					<source>ZooKeys </source>
					<volume>150</volume>: <fpage>53</fpage>–<lpage>70</lpage>. <ext-link ext-link-type="uri" xlink:href="http://dx.doi.org/10.3897/zookeys.150.2193" xlink:type="simple">doi: 10.3897/zookeys.150.2193</ext-link>
				</mixed-citation-->
				<xsl:when test="./source and ./volume and ./issue-title"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group[1]"/> <xsl:apply-templates select="./year"/>. <xsl:apply-templates select="./article-title"/>. In: <xsl:if test="./person-group[2]"><xsl:apply-templates select="./person-group[2]"/><xsl:choose>
						<xsl:when test="count(./person-group[2]/name) > 1">Eds.,</xsl:when>
						<xsl:otherwise>Ed.,</xsl:otherwise>
					</xsl:choose></xsl:if><volumeTitle><xsl:value-of select="./issue-title"/></volumeTitle>. <journalOrPublisher><xsl:choose>
						<xsl:when test="./series"><xsl:value-of select="./source"/> (<xsl:value-of select="./series"/>)</xsl:when>
						<xsl:otherwise><xsl:value-of select="./source"/></xsl:otherwise>
					</xsl:choose></journalOrPublisher> <volume><xsl:value-of select="./volume"/></volume><xsl:choose>
						<xsl:when test="./fpage and ./lpage">: <pageData><xsl:value-of select="./fpage"/> - <xsl:value-of select="./lpage"/></pageData></xsl:when>
						<xsl:when test="./fpage"><pageData>: <xsl:value-of select="./fpage"/></pageData></xsl:when>
						<xsl:when test="./lpage">, <xsl:value-of select="./lpage"/> pp.</xsl:when>
					</xsl:choose>
				</xsl:when>
				<!--mixed-citation> JOURNAL ARTICLE
					<person-group>
						<name>
							<surname>Murphy</surname>
							<given-names>NP</given-names>
						</name>
						<name>
							<surname>Carey</surname>
							<given-names>D</given-names>
						</name>
						<name>
							<surname>Castro</surname>
							<given-names>LR</given-names>
						</name>
						<name>
							<surname>Dowton</surname>
							<given-names>M</given-names>
						</name>
						<name>
							<surname>Austin</surname>
							<given-names>AD</given-names>
						</name>
					</person-group> (<year>2007</year>) <article-title>Phylogeny of the
                        platygastroid wasps (<tp:taxon-name>Hymenoptera</tp:taxon-name>) based on
                        sequences from the 18S rRNA, 28S rRNA and cytochrome oxidase genes:
                        implications for the evolution of the ovipositor system and host
                        relationships</article-title>. <source>Biological Journal of the Linnean
                        Society</source>
					<volume>91</volume>:
                    <fpage>653</fpage>&#8211;<lpage>669</lpage>.</mixed-citation-->
				<xsl:when test="./source and ./volume"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group"/> <xsl:apply-templates select="./year"/>. <xsl:apply-templates select="./article-title"/>. <journalOrPublisher><xsl:choose>
						<xsl:when test="./series"><xsl:value-of select="./source"/> (<xsl:value-of select="./series"/>)</xsl:when>
						<xsl:otherwise><xsl:value-of select="./source"/></xsl:otherwise>
					</xsl:choose></journalOrPublisher> <volume><xsl:value-of select="./volume"/></volume><xsl:choose>
						<xsl:when test="./fpage and ./lpage">: <pageData><xsl:value-of select="./fpage"/> - <xsl:value-of select="./lpage"/></pageData></xsl:when>
						<xsl:when test="./fpage"><pageData>: <xsl:value-of select="./fpage"/></pageData></xsl:when>
						<xsl:when test="./lpage">, <xsl:value-of select="./lpage"/> pp.</xsl:when>
					</xsl:choose>
				</xsl:when>
				<!--mixed-citation> URL
					<person-group>
						<name>
							<surname>Otte</surname>
							<given-names>D</given-names>
						</name>
						<name>
							<surname>Eades</surname>
							<given-names>CC</given-names>
						</name>
						<name>
							<surname>Braun</surname>
							<given-names>H</given-names>
						</name>
						<name>
							<surname>Cigliano</surname>
							<given-names>MM</given-names>
						</name>
						<name>
							<surname>Naskrecki</surname>
							<given-names>P</given-names>
						</name>
					</person-group>
					<source>
						<tp:taxon-name>Orthoptera</tp:taxon-name> Species File Online</source>.
                    Version 2.2. <ext-link>http://osf2.orthoptera.org</ext-link>. [accessed: 23
                    February, <year>2009</year>]</mixed-citation-->
				<xsl:when test="./ext-link"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group"/> <xsl:apply-templates select="./year"/>. <title><xsl:value-of select="./source"/></title>. <xsl:value-of select="./ext-link"/>
				</xsl:when>
			</xsl:choose>
		</bibRef>
	</xsl:template>
	
	<xsl:template match="element-citation">
		<bibRef>
			<xsl:attribute name="citationId"><xsl:value-of select="./parent::ref/@id"/></xsl:attribute>
			<xsl:choose>
				<!--mixed-citation> BOOK CHAPTER
					<person-group>
						<name>
							<surname>Rutherford</surname>
							<given-names>MC</given-names>
						</name>
						<name>
							<surname>Mucina</surname>
							<given-names>L</given-names>
						</name>
						<name>
							<surname>Powrie</surname>
							<given-names>LW</given-names>
						</name>
					</person-group> (<year>2006</year>) <article-title>Biomes and bioregions of
                        Southern Africa</article-title>. In: <name>
						<surname>Mucina</surname>
						<given-names>L</given-names>
					</name>, <name>
						<surname>Rutherford</surname>
						<given-names>MC</given-names>
					</name> (<role>Eds</role>) <source>The vegetation of South Africa, Lesotho and
                        Swaziland</source>. <publisher-name>SANBI</publisher-name>
					<publisher-loc>Pretoria</publisher-loc>,
                        <fpage>30</fpage>&#8211;<lpage>51</lpage>.</mixed-citation-->
				<xsl:when test="(./publisher-name or ./publisher-loc) and ./article-title"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group[1]"/>, <xsl:apply-templates select="./year"/>. <xsl:apply-templates select="./article-title"/>. In: <xsl:if test="./person-group[2]"><xsl:apply-templates select="./person-group[2]"/><xsl:choose>
						<xsl:when test="count(./person-group[2]/name) > 1">Eds.,</xsl:when>
						<xsl:otherwise>Ed.,</xsl:otherwise>
					</xsl:choose></xsl:if> <volumeTitle><xsl:value-of select="./source"/></volumeTitle>. <journalOrPublisher><xsl:choose>
						<xsl:when test="./publisher-name and ./publisher-loc"><xsl:value-of select="./publisher-name"/>, <xsl:value-of select="./publisher-loc"/></xsl:when>
						<xsl:when test="./publisher-name"><xsl:value-of select="./publisher-name"/></xsl:when>
						<xsl:when test="./publisher-loc"><xsl:value-of select="./publisher-loc"/></xsl:when>
					</xsl:choose></journalOrPublisher><xsl:choose>
						<xsl:when test="./fpage and ./lpage">: <pageData><xsl:value-of select="./fpage"/> - <xsl:value-of select="./lpage"/></pageData></xsl:when>
						<xsl:when test="./fpage"><pageData>: <xsl:value-of select="./fpage"/></pageData></xsl:when>
						<xsl:when test="./lpage">, <xsl:value-of select="./lpage"/> pp.</xsl:when>
					</xsl:choose>
				</xsl:when>
					<!--mixed-citation> BOOK
					<person-group>
						<name>
							<surname>Mucina</surname>
							<given-names>L</given-names>
						</name>
						<name>
							<surname>Rutherford</surname>
							<given-names>MC</given-names>
						</name>
					</person-group> (<role>Eds</role>) <year>2006</year>. <source>The vegetation of
                        South Africa, Lesotho and Swaziland</source>
					<publisher-name>SANBI</publisher-name>, <publisher-loc>Pretoria</publisher-loc>,
                        <lpage>807</lpage> pp. </mixed-citation-->
				<xsl:when test="./publisher-name or ./publisher-loc"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group"/> <xsl:apply-templates select="./year"/>. <title><xsl:value-of select="./source"/></title>. <journalOrPublisher><xsl:choose>
						<xsl:when test="./publisher-name and ./publisher-loc"><xsl:value-of select="./publisher-name"/>, <xsl:value-of select="./publisher-loc"/></xsl:when>
						<xsl:when test="./publisher-name"><xsl:value-of select="./publisher-name"/></xsl:when>
						<xsl:when test="./publisher-loc"><xsl:value-of select="./publisher-loc"/></xsl:when>
					</xsl:choose></journalOrPublisher><xsl:choose>
						<xsl:when test="./fpage and ./lpage">: <pageData><xsl:value-of select="./fpage"/> - <xsl:value-of select="./lpage"/></pageData></xsl:when>
						<xsl:when test="./fpage"><pageData>: <xsl:value-of select="./fpage"/></pageData></xsl:when>
						<xsl:when test="./lpage">, <xsl:value-of select="./lpage"/> pp.</xsl:when>
					</xsl:choose>
				</xsl:when>
				<!--mixed-citation xlink:type="simple"> JOURNAL ARTICLE IN SPECIAL ISSUE
					<person-group>
						<name name-style="western">
							<surname>Smith</surname>
							<given-names>VS</given-names>
						</name>
						<name name-style="western">
							<surname>Rycroft</surname>
							<given-names>SD</given-names>
						</name>
						<name name-style="western">
							<surname>Brake</surname>
							<given-names>I</given-names>
						</name>
						<name name-style="western">
							<surname>Scott</surname>
							<given-names>B</given-names>
						</name>
						<name name-style="western">
							<surname>Baker</surname>
							<given-names>E</given-names>
						</name>
						<name name-style="western">
							<surname>Livermore</surname>
							<given-names>L</given-names>
						</name>
						<name name-style="western">
							<surname>Blagoderov</surname>
							<given-names>V</given-names>
						</name>
						<name name-style="western">
							<surname>Roberts</surname>
							<given-names>D</given-names>
						</name>
					</person-group> (<year>2011</year>)<article-title> Scratchpads 2.0: a Virtual Research Environment supporting scholarly collaboration, communication and data publication in biodiversity science.</article-title> In: <person-group>
						<name name-style="western">
							<surname>Smith</surname>
							<given-names>V</given-names>
						</name>
						<name name-style="western">
							<surname>Penev</surname>
							<given-names>L</given-names>
						</name>
					</person-group> (<role>Eds</role>). <issue-title> e-Infrastructures for data publishing in biodiversity science.</issue-title>
					<source>ZooKeys </source>
					<volume>150</volume>: <fpage>53</fpage>–<lpage>70</lpage>. <ext-link ext-link-type="uri" xlink:href="http://dx.doi.org/10.3897/zookeys.150.2193" xlink:type="simple">doi: 10.3897/zookeys.150.2193</ext-link>
				</mixed-citation-->
				<xsl:when test="./source and ./volume and ./issue-title"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group[1]"/> <xsl:apply-templates select="./year"/>. <xsl:apply-templates select="./article-title"/>. In: <xsl:if test="./person-group[2]"><xsl:apply-templates select="./person-group[2]"/><xsl:choose>
						<xsl:when test="count(./person-group[2]/name) > 1">Eds.,</xsl:when>
						<xsl:otherwise>Ed.,</xsl:otherwise>
					</xsl:choose></xsl:if><volumeTitle><xsl:value-of select="./issue-title"/></volumeTitle>. <journalOrPublisher><xsl:choose>
						<xsl:when test="./series"><xsl:value-of select="./source"/> (<xsl:value-of select="./series"/>)</xsl:when>
						<xsl:otherwise><xsl:value-of select="./source"/></xsl:otherwise>
					</xsl:choose></journalOrPublisher> <volume><xsl:value-of select="./volume"/></volume><xsl:choose>
						<xsl:when test="./fpage and ./lpage">: <pageData><xsl:value-of select="./fpage"/> - <xsl:value-of select="./lpage"/></pageData></xsl:when>
						<xsl:when test="./fpage"><pageData>: <xsl:value-of select="./fpage"/></pageData></xsl:when>
						<xsl:when test="./lpage">, <xsl:value-of select="./lpage"/> pp.</xsl:when>
					</xsl:choose>
				</xsl:when>
				<!--mixed-citation> JOURNAL ARTICLE
					<person-group>
						<name>
							<surname>Murphy</surname>
							<given-names>NP</given-names>
						</name>
						<name>
							<surname>Carey</surname>
							<given-names>D</given-names>
						</name>
						<name>
							<surname>Castro</surname>
							<given-names>LR</given-names>
						</name>
						<name>
							<surname>Dowton</surname>
							<given-names>M</given-names>
						</name>
						<name>
							<surname>Austin</surname>
							<given-names>AD</given-names>
						</name>
					</person-group> (<year>2007</year>) <article-title>Phylogeny of the
                        platygastroid wasps (<tp:taxon-name>Hymenoptera</tp:taxon-name>) based on
                        sequences from the 18S rRNA, 28S rRNA and cytochrome oxidase genes:
                        implications for the evolution of the ovipositor system and host
                        relationships</article-title>. <source>Biological Journal of the Linnean
                        Society</source>
					<volume>91</volume>:
                    <fpage>653</fpage>&#8211;<lpage>669</lpage>.</mixed-citation-->
				<xsl:when test="./source and ./volume"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group"/> <xsl:apply-templates select="./year"/>. <xsl:apply-templates select="./article-title"/>. <journalOrPublisher><xsl:choose>
						<xsl:when test="./series"><xsl:value-of select="./source"/> (<xsl:value-of select="./series"/>)</xsl:when>
						<xsl:otherwise><xsl:value-of select="./source"/></xsl:otherwise>
					</xsl:choose></journalOrPublisher> <volume><xsl:value-of select="./volume"/></volume><xsl:choose>
						<xsl:when test="./fpage and ./lpage">: <pageData><xsl:value-of select="./fpage"/> - <xsl:value-of select="./lpage"/></pageData></xsl:when>
						<xsl:when test="./fpage"><pageData>: <xsl:value-of select="./fpage"/></pageData></xsl:when>
						<xsl:when test="./lpage">, <xsl:value-of select="./lpage"/> pp.</xsl:when>
					</xsl:choose>
				</xsl:when>
				<!--mixed-citation> URL
					<person-group>
						<name>
							<surname>Otte</surname>
							<given-names>D</given-names>
						</name>
						<name>
							<surname>Eades</surname>
							<given-names>CC</given-names>
						</name>
						<name>
							<surname>Braun</surname>
							<given-names>H</given-names>
						</name>
						<name>
							<surname>Cigliano</surname>
							<given-names>MM</given-names>
						</name>
						<name>
							<surname>Naskrecki</surname>
							<given-names>P</given-names>
						</name>
					</person-group>
					<source>
						<tp:taxon-name>Orthoptera</tp:taxon-name> Species File Online</source>.
                    Version 2.2. <ext-link>http://osf2.orthoptera.org</ext-link>. [accessed: 23
                    February, <year>2009</year>]</mixed-citation-->
				<xsl:when test="./ext-link"><xsl:if test="./ext-link"><xsl:attribute name="url"><xsl:value-of select="./ext-link"/></xsl:attribute></xsl:if>
					<xsl:apply-templates select="./person-group"/> <xsl:apply-templates select="./year"/>. <title><xsl:value-of select="./source"/></title>. <xsl:value-of select="./ext-link"/>
				</xsl:when>
			</xsl:choose>
		</bibRef>
	</xsl:template>
	
	<xsl:template match="person-group">
		<xsl:choose>
			<xsl:when test="./preceding-sibling::person-group">
				<xsl:for-each select="./name"><editor>
					<xsl:value-of select="./surname"/><xsl:text disable-output-escaping="yes">, </xsl:text><xsl:value-of select="./given-names"/>
				</editor>,
			</xsl:for-each></xsl:when>
			<xsl:otherwise><xsl:for-each select="./name">
				<author>
					<xsl:value-of select="./surname"/><xsl:text disable-output-escaping="yes">, </xsl:text><xsl:value-of select="./given-names"/>
				</author>,
			</xsl:for-each></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="name[./parent::mixed-citation]">
		<editor>
			<xsl:value-of select="./surname"/><xsl:text disable-output-escaping="yes">, </xsl:text><xsl:value-of select="./given-names"/>
		</editor>,
	</xsl:template>
	
	<xsl:template match="year">
		<year><xsl:value-of select="."/></year>
	</xsl:template>
	
	<xsl:template match="article-title">
		<title><xsl:value-of select="."/></title>
	</xsl:template>
	
	<xsl:template match="article-title/tp:taxon-name"> <xsl:value-of select="."/> </xsl:template>
	
	<xsl:template match="bold">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="kwd-group">
		<xsl:choose>
			<xsl:when test="./ancestor::tp:taxon-treatment">
				<subSubSection type="multiple">
					<paragraph>
						<xsl:apply-templates/>
					</paragraph>
				</subSubSection>
			</xsl:when>
			<xsl:otherwise>
				<subSection type="multiple">
					<paragraph>
						<xsl:apply-templates/>
					</paragraph>
				</subSection>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
