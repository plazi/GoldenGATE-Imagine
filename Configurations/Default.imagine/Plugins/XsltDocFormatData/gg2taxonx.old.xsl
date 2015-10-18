<?xml version="1.0" encoding="UTF-8"?>
<!-- gg2taxonx.xsl
    2007-11-07
    Terry Catapano
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:tax="http://www.taxonx.org/schema/v1"
    xmlns:dc="http://digir.net/schema/conceptual/darwin/2003/1.0"
    xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    <xsl:strip-space elements="*"/>
    <xsl:template match="/">
        <tax:taxonx>
            <xsl:apply-templates select=".//mods:mods"/>
            <tax:taxonxBody>
                <xsl:apply-templates select="//*[parent::document][name() != 'mods:mods']"/>
            </tax:taxonxBody>
        </tax:taxonx>
    </xsl:template>
    <xsl:template match="mods:mods">
        <tax:taxonxHeader>
            <xsl:copy-of select="."/>
        </tax:taxonxHeader>
    </xsl:template>
    <xsl:template match="treatment">
        <tax:treatment>
            <xsl:apply-templates/>
        </tax:treatment>
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
                    <xsl:attribute name="type">
                        <xsl:value-of select="@type"/>
                    </xsl:attribute>
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
                            <xsl:attribute name="identifier">
                                <xsl:value-of select="@LSID-HNS"/>
                            </xsl:attribute>
                            <xsl:attribute name="source">HNS</xsl:attribute>
                        </xsl:element>
                    </xsl:if>
                    <xsl:if test="@lsidName">
                        <xsl:element name="tax:xmldata">
                            <xsl:choose>
                                <xsl:when test="@genus">
                                    <dc:Genus>
                                        <xsl:value-of select="@genus"/>
                                    </dc:Genus>
                                </xsl:when>
                                <xsl:when test="@species">
                                    <dc:Species>
                                        <xsl:value-of select="@species"/>
                                    </dc:Species>
                                </xsl:when>
                            </xsl:choose>

                        </xsl:element>
                    </xsl:if>
                </xsl:when>
            </xsl:choose>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="author">
        <tax:author>
            <xsl:apply-templates/>
        </tax:author>
    </xsl:template>
    <xsl:template match="title">
        <tax:title>
            <xsl:apply-templates/>
        </tax:title>
    </xsl:template>
    <xsl:template match="citation">
        <tax:bibref>
            <xsl:apply-templates/>
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
            <tax:p>
                <xsl:apply-templates/>
            </tax:p>
        </tax:figure>
    </xsl:template>
</xsl:stylesheet>
