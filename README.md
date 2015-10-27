# GoldenGATE-Imagine

GoldenGATE-Imagine is an environment for marking up, ehancing, and extracting text and data from PDF files. It has specific enhancements for articles containing descriptions of taxa ("taxonomic treatments") in the field of Biological Systematics, but its core features may be used for general purposes as well.

GoldenGATE-Imagine opens PDF documents, extracting or rendering page images, performing OCR or decoding embedded fonts where required, and finally segmenting pages into columns, blocks, paragraphs, and lines. Afterwards, it offers a wide range of (semi-)automated tools and manual markup and editing functionality.

### Tools include

* automated document structuring, comprising
 * elimination of page headers
 * extraction of figures and tables, together with their respective captions
 * detection of footnotes
 * detection of headings and their hierarchy
* document metadata extraction
* detection and parsing of bibliographic references, using RefParse
* markup of citations and linking to the corresponding bibliographic references
* detection, atomization, and reconciliation of taxonomic names, backed by Catalog of Life, GBIF, and IPNI
* markup of taxonomic treatments and their inner structure
* extraction and parsing of occurrence records
* tagging of trait terms, backed by ontologies.
