# GoldenGATE-Imagine

GoldenGATE-Imagine is an environment for marking up, enhancing, and extracting text and data from PDF files. It has specific enhancements for articles containing descriptions of taxa ("taxonomic treatments") in the field of Biological Systematics, but its core features may be used for general purposes as well.

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

At any stage of document processing, users can choose to export documents as XML. As soon as document metadata is extracted and taxon names and treatments are marked, they can also export a DarwinCore Archive or upload the document to the Plazi treatment repository, which allows them to share their markup work with the public. Further, users can export tables and figures, together with their captions

### Installation requirements


####REQUIRED JAVA VERSION

The program requires Java 1.6 and above, and uses the Oracle Java run time environment. 


To determine your computers current Java version, do the following.

**Windows**: go to “Command prompt” and type in `java -version`.

<pre>C:\Users\Donat>java -version
java version "1.8.0_31"
Java(TM) SE Runtime Environment (build 1.8.0_31-b13)
Java HotSpot(TM) Client VM (build 25.31-b07, mixed mode)</pre>


**Mac**: From terminal type `java /version` 

The program requires Java 1.6 and above, and uses the Oracle Java run time environment. 

**Linux**: 

GoldenGate is built on Java, a free, cross-platform software development environment. Java comes in a number of versions, so it is important that you install the version that GoldenGate requires: Java 7 or 8. GoldenGate can run with both the Java "JRE" (Java Runtime Environment). For instructions about running Java on your operating system, see Oracle's Installing Java page.
Linux users: Unfortunately, Oracle's own pages (such as the link above) focus on commercial Linux distributions. Users of other distributions are better served by distribution-specific instructions.
Special note: It is recommended to use the Oracle Java VM, this is the safest choice. The OpenJDK 7 is a good open source alternative for the Oracle JVM.
Details of the Java VM can be obtained via the following command (two results displayed):

<pre>java -version
java version "1.7.0_55"
Java(TM) SE Runtime Environment (build 1.7.0_55-b13)
Java HotSpot(TM) 64-Bit Server VM (build 24.55-b03, mixed mode)</pre>

It is NOT recommended to use "OpenJDK6" (IcedTea) and "GNU Compiler for Java" (GCJ) which are shipped by several Linux distributions.

#### To Deploy

Download the latest release at: https://github.com/plazi/GoldenGATE-Imagine/releases

#### To Run

On Windows: in the directory in which GoldenGATE-Imagine has been installed double-click `GgImagine.exe`

On Mac and Linux from the command line in the directory in which GoldenGATE-Imagine has been installed, run `GGgImagine.sh` or run `java -jar GgImagineStarter.jar`

Consult the User Manual for further instructions.
