CONFIGURATION INFO

Default.imagine configuration for GoldenGATE Document Markup System, created 2014.10.21.13.39

First standard configuration for GoldenGATE Imagine


REQUIRED JAVA VERSION

Sun (r) Java Runtime Environment (JRE) 1.4.2 or higher (recommended: JRE 1.5.0 or higher)


REQUIRED GOLDENGATE VERSION

2013.02.19.19.30


LICENSE

Copyright (c) 2006-2014, Guido Sautter, IPD Boehm, Universität Karlsruhe (TH)
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Universität Karlsruhe (TH) nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS 'AS IS' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


CHANGE LOG

Changes to this configuration in reverse chronological order


NEW IN VERSION 2015.10.05.04.25

- added char usage statistics to improve glyph decoding
- improved dealing with words and figures that reach of out of page bounds
- added capability to export treatment data to DwC-A
- added capability to export figures and tables to ZIP, together with captions


NEW IN VERSION 2015.09.18.19.42

- added document style templates for Zootaxa monographs
- adjusted document style templates for Zootaxa articles to account for layout variances
- salvaging taxon name authorities that are also genera
- improved handling of pages flipped from bottom-up or top-down text to left-right
- improved handling of figures embedded in PDFs upside down
- several minor bugfixes


NEW IN VERSION 2015.09.15.00.03

- improved handling of taxonomic names between the ranks of family and genus
- added explicit annotation duplicate cleanup
- added table cell annotation cleanup and safeguard
- added minimum column margin to table detection
- adjusted Zootaxa 2013 onward style template to split captions off tables more reliably


NEW IN VERSION 2015.09.06.18.05

- added document style template for Zootaxa 2013 onward (differs in metadata from 2007 onward)
- added further anchor to Zootaxa 2007 onward document style template to recognize difference
- relaxed criteria for caption detection to cope with font size inaccuracies
- paragraph break detection improvement and bugfix in document structure detector


NEW IN VERSION 2015.09.05.19.03

- added use of document style templates
- added designer for document style templates
- added heading based fully automated treatment tagger
- filtering off-page words


NEW IN VERSION 2015.08.10.14.40

- added decryption of encrypted PDFs
- massive reduction of memory requirements for PDF decoding
- massive reduction of memory requirements for document display
- added document navigation facilities (find next/previous, list occurrences)
- added memory cleanup infrastructure for plug-ins on closing documents


NEW IN VERSION 2015.07.11.01.08

- added text extraction from PDF forms
- added repair function for mass word fragmentation (can happen in obfuscated born-digital PDFs)
- bugfix in OCR result editing
- word sequence modification improvements
- added ontology based trait term tagging
- metric conversion bugfix in quantity tagging
- extended taxon name authority handling to botanical idiosyncracies
- added button based paragraph exclusion in materials citation tagging
- added 'collectedFrom' detail to materials citation attributes


NEW IN VERSION 2015.05.17.14.53

- small-caps problems in PDF font decoding alleviated
- implicit space text obfuscation problems fixed
- landscape page problems fixed
- added editable XML view of document and annotations
- added option to decode PDFs in slave JVM to alleviate memory problems (deactivated in config by default, though)


NEW IN VERSION 2015.05.09.01.21

- PDF facilities can decode TrueType fonts now
- further improvements to PDF font decodeing facilities
- extended font correction functionality for born-digital PDFs
- sorting issues in Java 1.7 and above sorted out
- cations, footnotes, and tables hidden now on treatment and materials citation markup
- many minor fixes and improvements


NEW IN VERSION 2015.03.25.15.07

- improved page image extraction for scanned PDFs (working much the same as for figures now)
- revised distinction of scanned from born-digital PDFs
- fixed decoding of sans-serif fonts
- added MacOS version of OCR engine
- fixed splitting of blocks into paragraphs
- failing gracefully now if Java refuses to render some character
- added manual assignment of captions to images and tables


NEW IN VERSION 2015.03.19.09.31

- added automated title and pagination recognition from document to metadata editor
- added document head annotation of document metadata attributes
- document metadata editor also shows in Tools menu
- extraction buttons for already extracted document metadata attributes reveal themselves with green borders
- restricted emphasis annotations to within paragraph boundaries
- added manual correction of assignment of caption targets (tables and figures) to captions
- updates to caption targets automatically write through to captions, as well as to figureCitations and tableCitations
- improved PDF glyph decoding, including correction of double mappings
- fixed occasional loading and tool application hangups
- many minor UI improvements, including fast scrolling
- added negative caching to CoL and GBIF lookups
- added handling of diacritics to bibliographic reference parser


NEW IN VERSION 2015.03.04.14.27

- added document ID extraction to document metadata extraction
- activated RefBank lookup for document meta data
- fixed table bounds generation on table block merging


NEW IN VERSION 2015.02.26.13.40

- filtering vernacular/common names from CoL lookup results


NEW IN VERSION 2015.02.26.12.31

- center alignment detection works on columns now
- added heading hierarchy assessment to document structure detection


NEW IN VERSION 2015.02.13.11.46

- parallelized catalog lookups in taxonomic name detection
- improved authority handling in taxonomic name detection
- added learning from nomenclature acts to taxonomic name detection
- added block merging functionality


NEW IN VERSION 2015.02.09.10.27

- parallelized document structure detection


NEW IN VERSION 2015.02.04.13.10

- taxonomic name detection bugfix


NEW IN VERSION 2015.02.03.04.30

- bugfix in PDF font decoding
- extended document tructure detection to connecting tables
- extended document tructure detection to marking emphases and headings
- added marking and handling of images, as well as assignment of images to captions
- completely revised taxonomic name detection


NEW IN VERSION 2015.01.19.07.00

- accelerated glyph transcription in embedded fonts of born-digital PDFs
- added glyph transcription editing for embedded fonts in born-digital PDFs
- extended document structure detection to recognizing and marking tables


NEW IN VERSION 2014.12.09.22.00

- finally got intended version of exporter to GoldenGATE Server


NEW IN VERSION 2014.12.09.18.36

- removed generic upload to GoldenGATE Server (the exporter takes its place now)
- improved object listing
- fixed document normalization issue in treatment markup and structuring


NEW IN VERSION 2014.12.09.12.08

- update to data exporter to GoldenGATE Server


NEW IN VERSION 2014.11.28.10.19

- added list view for taxonomic names


NEW IN VERSION 2014.11.28.09.41

- added document structure detector
- added list views for Image Markup Objects


NEW IN VERSION 2014.11.14.00.52

- all-round update
- added editor for document meta data
- added data exporter to GoldenGATE Server
