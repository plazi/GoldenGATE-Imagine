<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:tax="http://www.taxonx.org/schema/v1" xmlns:dwc="http://digir.net/schema/conceptual/darwin/2003/1.0" xmlns:dwcranks="http://rs.tdwg.org/UBIF/2006/Schema/1.1" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>
	<xsl:strip-space elements="*"/>
	
	<xsl:template match="/">
		<xsl:apply-templates select="descendant-or-self::document"/>
	</xsl:template>
	
	<xsl:template match="document">
		<tax:taxonx xsi:schemaLocation="http://www.taxonx.org/schema/v1 http://www.taxonx.org/schema/v1/taxonx1.xsd http://www.loc.gov/mods/v3 http://www.loc.gov/mods/v3/mods-3-1.xsd http://digir.net/schema/conceptual/darwin/2003/1.0 http://digir.net/schema/conceptual/darwin/2003/1.0/darwin2.xsd http://rs.tdwg.org/UBIF/2006/Schema/1.1 http://rs.tdwg.org/UBIF/2006/Schema/1.1/DarwinCoreAdditionalTaxonRanks.xsd">
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
					<xsl:attribute name="type"><xsl:call-template name="divType"/></xsl:attribute>
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
					<xsl:attribute name="type"><xsl:call-template name="divType"/></xsl:attribute>
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="divType"><xsl:choose>
		<xsl:when test="@type = 'abstract' or @type = 'acknowledgments' or @type = 'biology_ecology' or @type = 'description' or @type = 'diagnosis' or @type = 'discussion' or @type = 'distribution' or @type = 'etymology' or @type = 'key' or @type = 'introduction' or @type = 'materials_examined' or @type = 'materials_methods' or @type = 'multiple' or @type = 'synopsis'"><xsl:value-of select="@type"/></xsl:when>
		<xsl:otherwise>multiple</xsl:otherwise>
	</xsl:choose></xsl:template>
	
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
			<xsl:when test="./table">
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
					<xsl:element name="tax:xmldata">
						<xsl:if test="@LSID-HNS">
							<xsl:element name="tax:xid">
								<xsl:attribute name="identifier"><xsl:value-of select="@LSID-HNS"/></xsl:attribute>
								<xsl:attribute name="source">HNS</xsl:attribute>
							</xsl:element>
						</xsl:if>
						<xsl:if test="@LSID-ZBK">
							<xsl:element name="tax:xid">
								<xsl:attribute name="identifier"><xsl:value-of select="@LSID-ZBK"/></xsl:attribute>
								<xsl:attribute name="source">ZooBank</xsl:attribute>
							</xsl:element>
						</xsl:if>
						<xsl:if test="@rank">
								<xsl:if test="@domain"><dwcranks:domain><xsl:value-of select="@domain"/></dwcranks:domain>
								</xsl:if>
								
								<xsl:if test="@superKingdom"><dwcranks:superkingdom><xsl:value-of select="@superKingdom"/></dwcranks:superkingdom>
								</xsl:if>
							<xsl:if test="@kingdom"><dwc:Kingdom><xsl:value-of select="@kingdom"/></dwc:Kingdom>
							</xsl:if>
								<xsl:if test="@subKingdom"><dwcranks:subkingdom><xsl:value-of select="@subKingdom"/></dwcranks:subkingdom>
								</xsl:if>
								<xsl:if test="@infraKingdom"><dwcranks:infrakingdom><xsl:value-of select="@infraKingdom"/></dwcranks:infrakingdom>
								</xsl:if>
								
								<xsl:if test="@superPhylum"><dwcranks:superphylum><xsl:value-of select="@superPhylum"/></dwcranks:superphylum>
								</xsl:if>
							<xsl:if test="@pyhylum"><dwc:Phylum><xsl:value-of select="@phylum"/></dwc:Phylum>
							</xsl:if>
								<xsl:if test="@subPhylum"><dwcranks:subphylum><xsl:value-of select="@subPhylum"/></dwcranks:subphylum>
								</xsl:if>
								<xsl:if test="@infraPhylum"><dwcranks:infraphylum><xsl:value-of select="@infraPhylum"/></dwcranks:infraphylum>
								</xsl:if>
								
								<xsl:if test="@superClass"><dwcranks:superclass><xsl:value-of select="@superClass"/></dwcranks:superclass>
								</xsl:if>
							<xsl:if test="@class"><dwc:Class><xsl:value-of select="@class"/></dwc:Class>
							</xsl:if>
								<xsl:if test="@subClass"><dwcranks:subclass><xsl:value-of select="@subClass"/></dwcranks:subclass>
								</xsl:if>
								<xsl:if test="@infraClass"><dwcranks:infraclass><xsl:value-of select="@infraClass"/></dwcranks:infraclass>
								</xsl:if>
								
								<xsl:if test="@superOrder"><dwcranks:superorder><xsl:value-of select="@superOrder"/></dwcranks:superorder>
								</xsl:if>
							<xsl:if test="@order"><dwc:Order><xsl:value-of select="@order"/></dwc:Order>
							</xsl:if>
								<xsl:if test="@subOrder"><dwcranks:suborder><xsl:value-of select="@subOrder"/></dwcranks:suborder>
								</xsl:if>
								<xsl:if test="@infraOrder"><dwcranks:infraorder><xsl:value-of select="@infraOrder"/></dwcranks:infraorder>
								</xsl:if>
								
								<xsl:if test="@superFamily"><dwcranks:superfamily><xsl:value-of select="@superFamily"/></dwcranks:superfamily>
								</xsl:if>
							<xsl:if test="@family"><dwc:Family><xsl:value-of select="@family"/></dwc:Family>
							</xsl:if>
								<xsl:if test="@subFamily"><dwcranks:subfamily><xsl:value-of select="@subFamily"/></dwcranks:subfamily>
								</xsl:if>
								<xsl:if test="@infraFamily"><dwcranks:infrafamily><xsl:value-of select="@infraFamily"/></dwcranks:infrafamily>
								</xsl:if>
								<xsl:if test="@superTribe"><dwcranks:supertribe><xsl:value-of select="@superTribe"/></dwcranks:supertribe>
								</xsl:if>
								<xsl:if test="@tribe"><dwcranks:tribe><xsl:value-of select="@tribe"/></dwcranks:tribe>
								</xsl:if>
								<xsl:if test="@subTribe"><dwcranks:subtribe><xsl:value-of select="@subTribe"/></dwcranks:subtribe>
								</xsl:if>
								<xsl:if test="@infraTribe"><dwcranks:infratribe><xsl:value-of select="@infraTribe"/></dwcranks:infratribe>
								</xsl:if>
								
							<xsl:if test="@genus"><dwc:Genus><xsl:value-of select="@genus"/></dwc:Genus>
							</xsl:if>
								<xsl:if test="@subGenus"><dwcranks:subgenus><xsl:value-of select="@subGenus"/></dwcranks:subgenus>
								</xsl:if>
								<xsl:if test="@infraGenus"><dwcranks:infragenus><xsl:value-of select="@infraGenus"/></dwcranks:infragenus>
								</xsl:if>
								<xsl:if test="@section"><dwcranks:section><xsl:value-of select="@section"/></dwcranks:section>
								</xsl:if>
								<xsl:if test="@subSection"><dwcranks:subsection><xsl:value-of select="@subSection"/></dwcranks:subsection>
								</xsl:if>
								<xsl:if test="@series"><dwcranks:series><xsl:value-of select="@series"/></dwcranks:series>
								</xsl:if>
								<xsl:if test="@subSeries"><dwcranks:subseries><xsl:value-of select="@subSeries"/></dwcranks:subseries>
								</xsl:if>
								
								<xsl:if test="@speciesAggregate"><dwcranks:speciesAggregate><xsl:value-of select="@speciesAggregate"/></dwcranks:speciesAggregate>
								</xsl:if>
							<xsl:if test="@species"><dwc:Species><xsl:value-of select="@species"/></dwc:Species>
							</xsl:if>
							<xsl:if test="@subSpecies"><dwc:Subspecies><xsl:value-of select="@subSpecies"/></dwc:Subspecies>
							</xsl:if>
								<!--xsl:if test="@subspecies"><dwcranks:subspeciesEpithet><xsl:value-of select="@subspecies"/></dwcranks:subspeciesEpithet></xsl:if-->
								<xsl:if test="@infraSpecies"><dwcranks:infraspeciesEpithet><xsl:value-of select="@infraSpecies"/></dwcranks:infraspeciesEpithet>
								</xsl:if>
								<xsl:if test="@graftChimaera"><dwcranks:graftChimaeraEpithet><xsl:value-of select="@graftChimaera"/></dwcranks:graftChimaeraEpithet>
								</xsl:if>
								<xsl:if test="@cultivarGroup"><dwcranks:cultivarGroupEpithet><xsl:value-of select="@cultivarGroup"/></dwcranks:cultivarGroupEpithet>
								</xsl:if>
								<xsl:if test="@convar"><dwcranks:convarEpithet><xsl:value-of select="@convar"/></dwcranks:convarEpithet>
								</xsl:if>
								<xsl:if test="@cultivar"><dwcranks:cultivarEpithet><xsl:value-of select="@cultivar"/></dwcranks:cultivarEpithet>
								</xsl:if>
								<xsl:if test="@bioVariety"><dwcranks:bioVarietyEpithet><xsl:value-of select="@bioVariety"/></dwcranks:bioVarietyEpithet>
								</xsl:if>
								<xsl:if test="@pathoVariety"><dwcranks:pathoVarietyEpithet><xsl:value-of select="@pathoVariety"/></dwcranks:pathoVarietyEpithet>
								</xsl:if>
								<xsl:if test="@variety"><dwcranks:varietyEpithet><xsl:value-of select="@variety"/></dwcranks:varietyEpithet>
								</xsl:if>
								<xsl:if test="@subVariety"><dwcranks:subvarietyEpithet><xsl:value-of select="@subVariety"/></dwcranks:subvarietyEpithet>
								</xsl:if>
								<xsl:if test="@subSubVariety"><dwcranks:subsubvarietyEpithet><xsl:value-of select="@subSubVariety"/></dwcranks:subsubvarietyEpithet>
								</xsl:if>
								<xsl:if test="@form"><dwcranks:formEpithet><xsl:value-of select="@form"/></dwcranks:formEpithet>
								</xsl:if>
								<xsl:if test="@subForm"><dwcranks:subformEpithet><xsl:value-of select="@subForm"/></dwcranks:subformEpithet>
								</xsl:if>
								<xsl:if test="@subSubForm"><dwcranks:subsubformEpithet><xsl:value-of select="@subSubForm"/></dwcranks:subsubformEpithet>
								</xsl:if>
								<xsl:if test="@specialForm"><dwcranks:specialformEpithet><xsl:value-of select="@specialForm"/></dwcranks:specialformEpithet>
								</xsl:if>
								<xsl:if test="@candidate"><dwcranks:candidateEpithet><xsl:value-of select="@candidate"/></dwcranks:candidateEpithet>
								</xsl:if>
							<dwc:taxonRank><xsl:value-of select="translate(@rank, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/></dwc:taxonRank>
						</xsl:if>
						<xsl:choose>
							<xsl:when test="@authority">
								<dwc:scientificNameAuthorship>
									<xsl:value-of select="@authority"/>
								</dwc:scientificNameAuthorship>
							</xsl:when>
							<xsl:when test="/authority">
								<dwc:scientificNameAuthorship>
									<xsl:value-of select="/authority"/>
								</dwc:scientificNameAuthorship>
							</xsl:when>
						</xsl:choose>
					</xsl:element>
				</xsl:when>
			</xsl:choose>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="taxonomicNameLabel">
		<xsl:choose>
			<xsl:when test="./ancestor::subSubSection[./@type = 'nomenclature']">
				<xsl:text disable-output-escaping="yes">&#x20;</xsl:text>
				<xsl:element name="tax:status"><xsl:value-of select="text()"/></xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text disable-output-escaping="yes">&#x20;</xsl:text>
				<xsl:value-of select="normalize-space()"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="authority">
		<xsl:text disable-output-escaping="yes">&#x20;</xsl:text>
		<xsl:value-of select="normalize-space()"/>
	</xsl:template>
	<xsl:template match="taxonNameAuthority">
		<xsl:text disable-output-escaping="yes">&#x20;</xsl:text>
		<xsl:value-of select="normalize-space()"/>
	</xsl:template>
	
	<xsl:template match="materialsCitation">
		<xsl:if test=".//pageStartToken">
			<xsl:element name="tax:pb">
				<xsl:attribute name="n"><xsl:value-of select=".//pageStartToken/@pageNumber"/></xsl:attribute>
			</xsl:element>
		</xsl:if>
		<xsl:if test=".//pageBreakToken[./@start]">
			<xsl:if test="./preceding::pageBreakToken"><xsl:element name="tax:pb">
				<xsl:attribute name="n"><xsl:value-of select=".//pageBreakToken/@pageNumber"/></xsl:attribute>
			</xsl:element></xsl:if>
		</xsl:if>
		<xsl:element name="tax:collection_event">
			<xsl:if test="./@longitude or ./@latitude or ./@country or ./@location or ./@collectingDate or ./@collectorName or ./@stateProvince or ./@typeStatus">
				<xsl:element name="tax:xmldata">
					
					<xsl:if test="./@longitude">
						<xsl:element name="dwc:DecimalLongitude">
							<xsl:value-of select="./@longitude"/>
						</xsl:element>
					</xsl:if>
					<xsl:if test="./@latitude">
						<xsl:element name="dwc:DecimalLatitude">
							<xsl:value-of select="./@latitude"/>
						</xsl:element>
					</xsl:if>
					
					<xsl:if test="./@country">
						<xsl:element name="dwc:Country">
							<xsl:value-of select="./@country"/>
						</xsl:element>
					</xsl:if>
					<xsl:if test="./@stateProvince">
						<xsl:element name="dwc:StateProvince">
							<xsl:value-of select="./@stateProvince"/>
						</xsl:element>
					</xsl:if>
					<xsl:if test="./@location">
						<xsl:element name="dwc:Locality">
							<xsl:value-of select="./@location"/>
						</xsl:element>
					</xsl:if>
					
					<xsl:if test="./@collectingDate">
						<xsl:element name="dwc:YearCollected">
							<xsl:value-of select="substring(./@collectingDate, 1, 4)"/>
						</xsl:element>
						<xsl:element name="dwc:MonthCollected">
							<xsl:value-of select="substring(./@collectingDate, 6, 2)"/>
						</xsl:element>
						<xsl:element name="dwc:DayCollected">
							<xsl:value-of select="substring(./@collectingDate, 9, 2)"/>
						</xsl:element>
					</xsl:if>
					
					<xsl:if test="./@collectorName">
						<xsl:element name="dwc:Collector">
							<xsl:value-of select="./@collectorName"/>
						</xsl:element>
					</xsl:if>
					<xsl:if test="./@typeStatus">
						<xsl:element name="dwc:TypeStatus">
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
			<xsl:when test="name() = 'pageBreakToken' and ./@start">
				<xsl:apply-templates select="."/>
			</xsl:when>
			<xsl:when test="./pageStartToken">
				<xsl:apply-templates select="./pageStartToken"/>
				<xsl:value-of select="text()"/>
			</xsl:when>
			<xsl:when test="./pageBreakToken[./@start]">
				<xsl:apply-templates select="./pageBreakToken[./@start]"/>
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
		<xsl:if test=". != document//pageStartToken[1]"><xsl:element name="tax:pb"><xsl:attribute name="n"><xsl:value-of select="./@pageNumber"/></xsl:attribute></xsl:element></xsl:if>
		<xsl:value-of select="text()"/>
	</xsl:template>
	<xsl:template match="pageBreakToken[./@start]">
		<xsl:if test="./preceding::pageBreakToken"><xsl:element name="tax:pb"><xsl:attribute name="n">
			<xsl:value-of select="./@pageNumber"/></xsl:attribute>
		</xsl:element></xsl:if>
		<xsl:value-of select="text()"/>
	</xsl:template>
	
	<xsl:template match="normalizedToken">
		<xsl:text disable-output-escaping="yes">&#x20;</xsl:text>
		<xsl:value-of select="./@originalValue"/>
	</xsl:template>
	
	<xsl:template match="text()[normalize-space(.) = ',' and not(./ancestor::subSubSection[./@type = 'nomenclature']) and ./preceding-sibling::*[1][name() = 'taxonomicName'] and ./following-sibling::*[1][name() = 'taxonomicNameLabel']]">	</xsl:template>
	<xsl:template match="text()">
		<xsl:if test="./preceding-sibling::* and not(contains('.,:;!?)]}>', substring(normalize-space(string(.)), 1, 1)))"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text></xsl:if>
		<!--xsl:text disable-output-escaping="yes">&#x20;</xsl:text-->
		<xsl:value-of select="normalize-space()"/>
	</xsl:template>
	
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
	
	<xsl:template match="bibRef">
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
	
	<xsl:template match="table">
		<xsl:element name="tax:div">
			<xsl:attribute name="type">other</xsl:attribute>
			<xsl:attribute name="otherType">table</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="tr">
		<xsl:element name="tax:div">
			<xsl:attribute name="type">other</xsl:attribute>
			<xsl:attribute name="otherType">tr</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="thead">
		<xsl:element name="tax:div">
			<xsl:attribute name="type">other</xsl:attribute>
			<xsl:attribute name="otherType">thead</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="td">
		<tax:p>
			<xsl:apply-templates/>
		</tax:p>
	</xsl:template>
	<xsl:template match="th">
		<tax:p>
			<xsl:apply-templates/>
		</tax:p>
	</xsl:template>
</xsl:stylesheet>
