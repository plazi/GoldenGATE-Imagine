/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.im.imagine.plugins.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner.ParallelFor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor.SynchronizedProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.goldenGate.plugins.PluginDataProviderFileBased;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.util.CaptionCitationHandler;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImfIO;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * This plugin provides a detector for the structure of a document. The detector
 * identified page headers, page numbers, captions, and footnotes. It can also
 * be used as a component outside of GoldenGATE Imagine.
 * 
 * @author sautter
 */
public class DocumentStructureDetectorProvider extends AbstractImageMarkupToolProvider implements ImagingConstants {
	private boolean debug = false;
	
	private static final String STRUCTURE_DETECTOR_IMT_NAME = "StructureDetector";
	
	private String pageNumberPattern = "[1-9][0-9]*";
	private String ocrPageNumberPartPattern = "[0-9]+";
	private HashMap ocrPageNumberCharacterTranslations = new HashMap();
	
	private Pattern[] captionStartPatterns = new Pattern[0];
	
	private Pattern[] footnoteStartPatterns = new Pattern[0];
	
	private CaptionCitationHandler captionCitationHandler;
	
	private ImageMarkupTool structureDetector = new StructureDetector();
	
	/** public zero-argument constructor for class loading */
	public DocumentStructureDetectorProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Document Structure Detector";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load character mappings
		try {
			InputStream is = this.dataProvider.getInputStream("pageNumberCharacters.txt");
			StringVector characterLines = StringVector.loadList(is);
			is.close();
			
			TreeSet pageNumberCharacters = new TreeSet();
			for (int l = 0; l < characterLines.size(); l++) {
				String characterLine = characterLines.get(l).trim();
				if ((characterLine.length() == 0) || characterLine.startsWith("//") || (characterLine.indexOf(' ') == -1))
					continue;
				
				String digit = characterLine.substring(0, characterLine.indexOf(' ')).trim();
				if (!Gamta.isNumber(digit))
					continue;
				
				pageNumberCharacters.add(digit);
				ArrayList characterTranslations = ((ArrayList) this.ocrPageNumberCharacterTranslations.get(digit));
				if (characterTranslations == null) {
					characterTranslations = new ArrayList(2);
					this.ocrPageNumberCharacterTranslations.put(digit, characterTranslations);
				}
				characterTranslations.add(digit);
				
				String characters = characterLine.substring(characterLine.indexOf(' ')).trim();
				for (int c = 0; c < characters.length(); c++) {
					String character = characters.substring(c, (c+1));
					pageNumberCharacters.add(character);
					characterTranslations = ((ArrayList) this.ocrPageNumberCharacterTranslations.get(character));
					if (characterTranslations == null) {
						characterTranslations = new ArrayList(2);
						this.ocrPageNumberCharacterTranslations.put(character, characterTranslations);
					}
					characterTranslations.add(digit);
				}
			}
			
			for (int d = 0; d < Gamta.DIGITS.length(); d++)
				pageNumberCharacters.add(Gamta.DIGITS.substring(d, (d+1)));
			
			StringBuffer ocrPageNumberCharaterPatternBuilder = new StringBuffer();
			for (Iterator cit = pageNumberCharacters.iterator(); cit.hasNext();) {
				String ocrPageNumberCharacter = ((String) cit.next());
				ocrPageNumberCharaterPatternBuilder.append(ocrPageNumberCharacter);
				ArrayList characterTranslations = ((ArrayList) this.ocrPageNumberCharacterTranslations.get(ocrPageNumberCharacter));
				if (characterTranslations == null)
					continue;
				int[] cts = new int[characterTranslations.size()];
				for (int t = 0; t < characterTranslations.size(); t++)
					cts[t] = Integer.parseInt((String) characterTranslations.get(t));
				this.ocrPageNumberCharacterTranslations.put(ocrPageNumberCharacter, cts);
			}
			
			String ocrPageNumberCharacterPattern = ("[" + RegExUtils.escapeForRegEx(ocrPageNumberCharaterPatternBuilder.toString()) + "]");
			this.ocrPageNumberPartPattern = (ocrPageNumberCharacterPattern + "+(\\s?" + ocrPageNumberCharacterPattern + "+)*");
		} catch (IOException ioe) {}
		
		//	load caption indicators
		try {
			InputStream is = this.dataProvider.getInputStream("captionStartPatterns.txt");
			StringVector captionStartPatternStrings = StringVector.loadList(is);
			is.close();
			
			ArrayList captionStartPatterns = new ArrayList();
			for (int l = 0; l < captionStartPatternStrings.size(); l++) {
				String captionStartPattern = captionStartPatternStrings.get(l).trim();
				if ((captionStartPattern.length() == 0) || captionStartPattern.startsWith("//"))
					continue;
				try {
					captionStartPatterns.add(Pattern.compile(captionStartPattern, Pattern.CASE_INSENSITIVE));
				} catch (PatternSyntaxException pse) {}
			}
			
			this.captionStartPatterns = ((Pattern[]) captionStartPatterns.toArray(new Pattern[captionStartPatterns.size()]));
		} catch (IOException ioe) {}
		
		//	load footnote indicators
		try {
			InputStream is = this.dataProvider.getInputStream("footnoteStartPatterns.txt");
			StringVector footnoteStartPatternStrings = StringVector.loadList(is);
			is.close();
			
			ArrayList footnoteStartPatterns = new ArrayList();
			for (int l = 0; l < footnoteStartPatternStrings.size(); l++) {
				String footnoteStartPattern = footnoteStartPatternStrings.get(l).trim();
				if ((footnoteStartPattern.length() == 0) || footnoteStartPattern.startsWith("//"))
					continue;
				try {
					footnoteStartPatterns.add(Pattern.compile(footnoteStartPattern, Pattern.CASE_INSENSITIVE));
				} catch (PatternSyntaxException pse) {}
			}
			
			this.footnoteStartPatterns = ((Pattern[]) footnoteStartPatterns.toArray(new Pattern[footnoteStartPatterns.size()]));
		} catch (IOException ioe) {}
		
		//	get caption citation handler
		if (this.parent == null)
			this.captionCitationHandler = new CaptionCitationHandler();
		else this.captionCitationHandler = ((CaptionCitationHandler) this.parent.getPlugin(CaptionCitationHandler.class.getName()));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {STRUCTURE_DETECTOR_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (STRUCTURE_DETECTOR_IMT_NAME.equals(name))
			return this.structureDetector;
		else return null;
	}
	
	private class StructureDetector implements ImageMarkupTool {
		public String getLabel() {
			return "Detect Document Structure";
		}
		public String getTooltip() {
			return "Detect page headers, page numbers, captions, and footnotes, etc.";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			if (annot == null) {
				boolean documentBornDigital;
				if (idmp != null)
					documentBornDigital = idmp.documentBornDigital;
				else {
					ImPage[] pages = doc.getPages();
					int wordCount = 0;
					int fnWordCount = 0;
					for (int p = 0; p < pages.length; p++) {
						ImWord[] pageWords = pages[p].getWords();
						for (int w = 0; w < pageWords.length; w++) {
							wordCount++;
							if (pageWords[w].hasAttribute(ImWord.FONT_NAME_ATTRIBUTE))
								fnWordCount++;
						}
					}
					documentBornDigital = ((wordCount * 2) < (fnWordCount * 3));
				}
				detectDocumentStructure(doc, documentBornDigital, pm);
			}
			else pm.setStep("Cannot detect document structure on single annotation");
		}
	}
	
	/**
	 * Detect and mark the structure of a document. In particular, this method
	 * identifies page numbers, page headers, captions, footnotes, and tables.
	 * If the <code>documentBornDigital</code> argument is set to false, page
	 * number detection considers a wide range of characters to potentially be
	 * page number digits that have been mis-recognized by OCR.
	 * @param doc the document whose structure to detect
	 * @param documentBornDigital is the document born digital or scanned?
	 * @param spm a progress monitor observing document processing
	 */
	public void detectDocumentStructure(final ImDocument doc, boolean documentBornDigital, ProgressMonitor pm) {
		
		//	build progress monitor with synchronized methods instead of synchronizing in-code
		final ProgressMonitor spm = new SynchronizedProgressMonitor(pm);
		
		//	get pages
		final ImPage[] pages = doc.getPages();
		spm.setStep("Detecting document structure from " + pages.length + " pages");
		
		//	compute average resolution
		int pageImageDpiSum = 0;
		for (int p = 0; p < pages.length; p++)
			pageImageDpiSum += pages[p].getImageDPI();
		final int pageImageDpi = ((pageImageDpiSum + (pages.length / 2)) / pages.length);
		spm.setInfo(" - resolution is " + pageImageDpi);
		
		//	get document style
		DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
		final DocumentStyle docLayout = docStyle.getSubset("layout");
		
		//	collect blocks adjacent to page edge (top or bottom) from each page
		spm.setStep(" - gathering data");
		spm.setBaseProgress(0);
		spm.setMaxProgress(10);
		final PageData[] pageData = new PageData[pages.length];
		final BoundingBox[][] pageHeaderAreas = new BoundingBox[pages.length][];
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				
				//	get header areas (default to general page layout if page class layout not given)
				String layoutPrefix;
				if (p == 0)
					layoutPrefix = "first.";
				else if ((p % 2) == 0) // first page is odd, so we need to decide 1-based, not 0-based
					layoutPrefix = "odd.";
				else layoutPrefix = "even."; // first page is odd, so we need to decide 1-based, not 0-based
				pageHeaderAreas[p] = docLayout.getBoxListProperty(("page." + layoutPrefix + "headerAreas"), docLayout.getBoxListProperty(("page.headerAreas"), null, pageImageDpi), pageImageDpi);
				
				//	go collect header areas
				pageData[p] = getPageData(pages[p], pageImageDpi, pageHeaderAreas[p], spm);
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		/* TODO re-detect document structure (starting all the way from columns and blocks):
		 * - use margins, table grid schemes, etc. from journal style
		 * - use minimum column widths to avoid splitting numbers off keys, etc.
		 * OR
		 * - determine minimum block width and column margins on the fly from document ...
		 * - ... and re-merge side-by-side pairs of blocks that (a) have similar height, (b) add up to column width, and (c) have narrower column on left (e.g. pages 8, 9, 10 in Moore & Gosliner 2014.pdf.imf)
		 * - use detected minimum margin also on flat (up to 3 lines) sets of block that add up to page width and might have been split up due to wide word gaps 
		 * - do that before detecting captions, as it helps repair flat captions (e.g. page 3 in Moore & Gosliner 2014.pdf.imf)
		 */
		
		//	collect words from top and bottom regions, separate for even and odd pages
		final CountingSet topWordsOdd = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet topWordsEven = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet bottomWordsOdd = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet bottomWordsEven = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet allWordsOdd = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		final CountingSet allWordsEven = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		
		//	collect words that might be (parts of) page numbers
		String pageNumberPartPattern = (documentBornDigital ? this.pageNumberPattern : this.ocrPageNumberPartPattern);
		
		//	collect the words, and find possible page numbers
		spm.setBaseProgress(10);
		spm.setMaxProgress(20);
		for (int p = 0; p < pages.length; p++) {
			spm.setProgress((p * 100) / pages.length);
			ArrayList pageNumberParts = new ArrayList();
			HashSet pageBorderWordIDs = new HashSet();
			
			//	get page number area and font properties (default to general page layout if page class layout not given)
			String layoutPrefix;
			if (p == 0)
				layoutPrefix = "first.";
			else if ((p % 2) == 0) // first page is odd, so we need to decide 1-based, not 0-based
				layoutPrefix = "odd.";
			else layoutPrefix = "even."; // first page is odd, so we need to decide 1-based, not 0-based
			BoundingBox pageNumberArea = docLayout.getBoxProperty(("page." + layoutPrefix + "number.area"), docLayout.getBoxProperty(("page.number.area"), null, pageImageDpi), pageImageDpi);
			int pageNumberFontSize = docLayout.getIntProperty(("page." + layoutPrefix + "number.fontSize"), docLayout.getIntProperty(("page.number.fontSize"), -1));
			String pageNumberFontSizeStr = ((pageNumberFontSize == -1) ? null : ("" + pageNumberFontSize));
			boolean pageNumberIsBold = docLayout.getBooleanProperty(("page." + layoutPrefix + "number.isBold"), docLayout.getBooleanProperty(("page.number.isBold"), false));
			pageNumberPartPattern = docLayout.getProperty(("page." + layoutPrefix + "number.pattern"), docLayout.getProperty(("page.number.pattern"), pageNumberPartPattern));
			
			//	words from page top candidates
			TreeSet topWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int r = 0; r < pageData[p].topRegions.size(); r++) {
				ImWord[] words = ((ImRegion) pageData[p].topRegions.get(r)).getWords();
				ImUtils.sortLeftRightTopDown(words);
				for (int w = 0; w < words.length; w++) {
					if (!pageBorderWordIDs.add(words[w].getLocalID()))
						continue;
					if (pageHeaderAreas[p] == null)
						topWords.add(words[w].getString());
					if ((pageNumberArea != null) && !pageNumberArea.includes(words[w].bounds, true))
						continue;
					if (pageNumberIsBold && !words[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
						continue;
					if ((pageNumberFontSizeStr != null) && !pageNumberFontSizeStr.equals(words[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE)))
						continue;
					if (words[w].getString().matches(pageNumberPartPattern))
						pageNumberParts.add(words[w]);
				}
			}
			(((p % 2) == 0) ? topWordsEven : topWordsOdd).addAll(topWords);
			
			//	words from page bottom candidates
			TreeSet bottomWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int r = 0; r < pageData[p].bottomRegions.size(); r++) {
				ImWord[] words = ((ImRegion) pageData[p].bottomRegions.get(r)).getWords();
				ImUtils.sortLeftRightTopDown(words);
				for (int w = 0; w < words.length; w++) {
					if (!pageBorderWordIDs.add(words[w].getLocalID()))
						continue;
					if (pageHeaderAreas[p] == null)
						bottomWords.add(words[w].getString());
					if ((pageNumberArea != null) && !pageNumberArea.includes(words[w].bounds, true))
						continue;
					if (pageNumberIsBold && !words[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
						continue;
					if ((pageNumberFontSizeStr != null) && !pageNumberFontSizeStr.equals(words[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE)))
						continue;
					if (words[w].getString().matches(pageNumberPartPattern))
						pageNumberParts.add(words[w]);
				}
			}
			(((p % 2) == 0) ? bottomWordsEven : bottomWordsOdd).addAll(bottomWords);
			
			//	overall words for comparison (only necessary if we don't know where page headers are, though)
			if (pageHeaderAreas[p] == null) {
				TreeSet allWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
				ImWord[] words = pages[p].getWords();
				for (int w = 0; w < words.length; w++)
					allWords.add(words[w].getString());
				(((p % 2) == 0) ? allWordsEven : allWordsOdd).addAll(allWords);
			}
			
			//	build candidate page numbers from collected parts
			ImUtils.sortLeftRightTopDown(pageNumberParts);
			if (documentBornDigital) {
				spm.setInfo(" - got " + pageNumberParts.size() + " possible page numbers for page " + p + ":");
				for (int w = 0; w < pageNumberParts.size(); w++) {
					pageData[p].pageNumberCandidates.add(new PageNumber((ImWord) pageNumberParts.get(w)));
					spm.setInfo("   - " + ((ImWord) pageNumberParts.get(w)).getString());
				}
			}
			else {
				spm.setInfo(" - got " + pageNumberParts.size() + " parts of possible page numbers:");
				HashSet pageNumberPartIDs = new HashSet();
				for (int w = 0; w < pageNumberParts.size(); w++)
					pageNumberPartIDs.add(((ImWord) pageNumberParts.get(w)).getLocalID());
				for (int w = 0; w < pageNumberParts.size(); w++) {
					spm.setInfo("   - " + ((ImWord) pageNumberParts.get(w)).getString());
					addPageNumberCandidates(pageData[p].pageNumberCandidates, ((ImWord) pageNumberParts.get(w)), pageNumberPartIDs, pageImageDpi, spm);
					Collections.sort(pageData[p].pageNumberCandidates, pageNumberValueOrder);
				}
			}
		}
		
		//	score and select page numbers for each page
		spm.setStep(" - scoring page numbers:");
		this.scoreAndSelectPageNumbers(pageData, spm);
		
		//	check page number sequence
		spm.setStep(" - checking page number sequence:");
		this.checkPageNumberSequence(pageData, spm);
		
		//	fill in missing page numbers
		spm.setStep(" - filling in missing page numbers:");
		this.fillInMissingPageNumbers(pageData, spm);
		
		//	annotate page numbers, collecting page number words
		final HashSet pageNumberWordIDs = new HashSet();
		for (int p = 0; p < pageData.length; p++)
			this.annotatePageNumbers(pageData[p], pageNumberWordIDs);
		
		//	judge on page headers based on frequent words and on page numbers
		spm.setStep(" - detecting page headers");
		spm.setBaseProgress(20);
		spm.setMaxProgress(25);
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				
				//	use heuristics for page top regions
				for (int r = 0; r < pageData[p].topRegions.size(); r++) {
					ImRegion topRegion = ((ImRegion) pageData[p].topRegions.get(r));
					ImWord[] regionWords = topRegion.getWords();
					Arrays.sort(regionWords, ImUtils.textStreamOrder);
					if ((regionWords.length == 0) || ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(regionWords[0].getTextStreamType()))
						continue;
					spm.setInfo("Testing " + topRegion.getType() + "@" + topRegion.bounds + " for top page header" + (regionWords[0].getTextStreamId().equals(regionWords[regionWords.length - 1].getTextStreamId()) ? (": " + ImUtils.getString(regionWords[0], regionWords[regionWords.length - 1], true)) : ""));
					if ((pageHeaderAreas[p] != null) || isPageTitle(topRegion, regionWords, (pages.length / 2), (((p % 2) == 0) ? topWordsEven : topWordsOdd), (((p % 2) == 0) ? allWordsEven : allWordsOdd), pageNumberWordIDs)) {
						spm.setInfo(" ==> page header found");
						synchronized (doc) {
							ImUtils.makeStream(regionWords, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, PAGE_TITLE_TYPE);
							ImUtils.orderStream(regionWords, ImUtils.leftRightTopDownOrder);
						}
					}
					else spm.setInfo(" ==> not a page header");
				}
				
				//	use heuristics for page bottom regions
				for (int r = 0; r < pageData[p].bottomRegions.size(); r++) {
					ImRegion bottomRegion = ((ImRegion) pageData[p].bottomRegions.get(r));
					ImWord[] regionWords = bottomRegion.getWords();
					Arrays.sort(regionWords, ImUtils.textStreamOrder);
					if ((regionWords.length == 0) || ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(regionWords[0].getTextStreamType()))
						continue;
					spm.setInfo("Testing " + bottomRegion.getType() + "@" + bottomRegion.bounds + " for bottom page header" + (regionWords[0].getTextStreamId().equals(regionWords[regionWords.length - 1].getTextStreamId()) ? (": " + ImUtils.getString(regionWords[0], regionWords[regionWords.length - 1], true)) : ""));
					if ((pageHeaderAreas[p] != null) || isPageTitle(bottomRegion, regionWords, (pages.length / 2), (((p % 2) == 0) ? bottomWordsEven : bottomWordsOdd), (((p % 2) == 0) ? allWordsEven : allWordsOdd), pageNumberWordIDs)) {
						spm.setInfo(" ==> page header found");
						synchronized (doc) {
							ImUtils.makeStream(regionWords, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, PAGE_TITLE_TYPE);
							ImUtils.orderStream(regionWords, ImUtils.leftRightTopDownOrder);
						}
					}
					else spm.setInfo(" ==> not a page header");
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	turn empty tables into regular blocks
		spm.setStep(" - correcting empty tables");
		spm.setBaseProgress(25);
		spm.setMaxProgress(30);
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				ImRegion[] pageTables = pages[p].getRegions(ImRegion.TABLE_TYPE);
				for (int t = 0; t < pageTables.length; t++) {
					ImWord[] tableWords = pageTables[t].getWords();
					if (tableWords.length == 0)
						pageTables[t].setType(ImRegion.BLOCK_ANNOTATION_TYPE);
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	get table layout hints, defaulting to somewhat universal ballpark figures
		DocumentStyle tableLayout = docLayout.getSubset(ImRegion.TABLE_TYPE);
		final int minTableColMargin = tableLayout.getIntProperty("minColumnMargin", (pageImageDpi / 30), pageImageDpi);
		final int minTableRowMargin = tableLayout.getIntProperty("minRowMargin", (pageImageDpi / 50), pageImageDpi);
		
		//	detect tables
		spm.setStep(" - detecting tables");
		spm.setBaseProgress(30);
		spm.setMaxProgress(40);
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				detectTables(pages[p], pageImageDpi, minTableColMargin, minTableRowMargin, spm);
				//	TODO figure out what layout hints might be useful for tables
				
				/* TODO restrict table detection:
				 * - maxColumnWidth: maximum width of individual column, prevents putting multi-column text in tables
				 * 
				 * - maxRowHeight: maximum height for individual rows, prevents main text cross splits
				 * - maxAvgRowHeight: maximum average row height, prevents main text multi-cross splits
				 * 
				 * - minColumnCount: minimum number of columns, excludes, e.g., two-column tables
				 * - minRowCount: minimum number of rows: excludes main text cross splits
				 * - minCellCount: minimum number of cells (columns x rows): might be above product of minimums for both dimensions
				 * 
				 * - grid.horizontal: boolean indicating if there are horizontal grid lines, prevents left and right merging beyond widest block
				 * - grid.vertical: boolean indicating if there are vertical grid lines, prevents up and down merging beyond highest block
				 * - grid.fram: boolean indicating outside frame, prevents any merging
				 * ==> not sure whether or not to use these booleans, as we do want to keep grid lines (and anything but words in general) out of block detection
				 */
			}
		}, pages.length, (this.debug ? 1 : -1));
		
		//	detect artifacts (blocks with word coverage below 10%, only for scanned documents)
		if (!documentBornDigital) {
			spm.setStep(" - detecting OCR artifacts in images");
			spm.setBaseProgress(40);
			spm.setMaxProgress(45);
			ParallelJobRunner.runParallelFor(new ParallelFor() {
				public void doFor(int p) throws Exception {
					spm.setProgress((p * 100) / pages.length);
					BoundingBox contentArea = docLayout.getBoxProperty("contentArea", pages[p].bounds, pageImageDpi);
					ImRegion[] pageBlocks = pages[p].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
					for (int b = 0; b < pageBlocks.length; b++) {
						spm.setInfo("Testing block " + pageBlocks[b].bounds + " on page " + pages[p].pageId + " for artifact");
						ImWord[] blockWords = pageBlocks[b].getWords();
						if (!contentArea.includes(pageBlocks[b].bounds, false) || isArtifact(pageBlocks[b], blockWords, pageImageDpi)) {
							spm.setInfo(" ==> artifact detected");
							synchronized (doc) {
								ImUtils.makeStream(blockWords, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
								ImUtils.orderStream(blockWords, ImUtils.leftRightTopDownOrder);
								for (ImWord imw = blockWords[blockWords.length-1]; imw != null; imw = imw.getPreviousWord())
									imw.setNextWord(null);
							}
						}
						else spm.setInfo(" ==> not an artifact");
					}
				}
			}, pages.length, (this.debug ? 1 : -1));
		}
		
		//	compute document-wide main text font size
		spm.setStep(" - computing main text font size");
		ImWord[] textStreamHeads = doc.getTextStreamHeads();
		int docFontSizeSum = 0;
		int docFontSizeWordCount = 0;
		for (int h = 0; h < textStreamHeads.length; h++) {
			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			for (ImWord imw = textStreamHeads[h]; imw != null; imw = imw.getNextWord()) {
				String imwFontSize = ((String) imw.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
				if (imwFontSize != null) try {
					docFontSizeSum += Integer.parseInt(imwFontSize);
					docFontSizeWordCount++;
				} catch (NumberFormatException nfe) {}
			}
		}
		final int docFontSize = ((docFontSizeWordCount == 0) ? -1 : ((docFontSizeSum + (docFontSizeWordCount / 2)) / docFontSizeWordCount));
		spm.setInfo("Average font size is " + docFontSize + " (based on " + docFontSizeWordCount + " main text words)");
		
		//	detect caption paragraphs
		spm.setStep(" - detecting captions");
		spm.setBaseProgress(documentBornDigital ? 40 : 45);
		spm.setMaxProgress(50);
		final HashMap[] pageCaptions = new HashMap[pages.length];
		Arrays.fill(pageCaptions, null);
		final boolean captionStartIsBold = docLayout.getBooleanProperty("caption.startIsBold", false);
		final int captionFontSize = docLayout.getIntProperty("caption.fontSize", -1);
		final int captionMinFontSize = docLayout.getIntProperty("caption.minFontSize", ((captionFontSize == -1) ? 0 : captionFontSize));
		final int captionMaxFontSize = docLayout.getIntProperty("caption.maxFontSize", ((captionFontSize == -1) ? 72 : captionFontSize));
		String[] captionStartPatternStrs = docLayout.getListProperty("caption.startPatterns", null, " ");
		final Pattern[] captionStartPatterns;
		if (captionStartPatternStrs == null)
			captionStartPatterns = this.captionStartPatterns;
		else {
			captionStartPatterns = new Pattern[captionStartPatternStrs.length];
			for (int p = 0; p < captionStartPatternStrs.length; p++)
				captionStartPatterns[p] = Pattern.compile(captionStartPatternStrs[p]);
		}
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int pg) throws Exception {
				spm.setProgress((pg * 100) / pages.length);
				ImRegion[] pageParagraphs = pages[pg].getRegions(PARAGRAPH_TYPE);
				for (int p = 0; p < pageParagraphs.length; p++) {
					ImWord[] paragraphWords = getLargestTextStreamWords(pageParagraphs[p].getWords());
					if (paragraphWords.length == 0)
						continue;
					Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
					if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(paragraphWords[0].getTextStreamType()))
						continue;
					spm.setInfo("Testing paragraph " + pageParagraphs[p].bounds + " on page " + pages[pg].pageId + " for caption");
					if (isCaption(pageParagraphs[p], paragraphWords, captionStartPatterns, captionMinFontSize, captionMaxFontSize, captionStartIsBold)) {
						spm.setInfo(" ==> caption detected");
						synchronized (doc) {
							ImAnnotation caption = ImUtils.makeStream(paragraphWords, ImWord.TEXT_STREAM_TYPE_CAPTION, CAPTION_TYPE);
							ImUtils.orderStream(paragraphWords, ImUtils.leftRightTopDownOrder);
							if (caption != null) {
								if (pageCaptions[pg] == null)
									pageCaptions[pg] = new HashMap();
								pageCaptions[pg].put(pageParagraphs[p], caption);
							}
						}
					}
					else spm.setInfo(" ==> not a caption");
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	detect footnote paragraphs
		spm.setStep(" - detecting footnotes");
		spm.setBaseProgress(50);
		spm.setMaxProgress(55);
		final int footnoteFontSize = docLayout.getIntProperty("footnote.fontSize", -1);
		final int footnoteMinFontSize = docLayout.getIntProperty("footnote.minFontSize", ((footnoteFontSize == -1) ? 0 : footnoteFontSize));
		final int footnoteMaxFontSize = docLayout.getIntProperty("footnote.maxFontSize", ((footnoteFontSize == -1) ? 72 : footnoteFontSize));
		String[] footnoteStartPatternStrs = docLayout.getListProperty("footnote.startPatterns", null, " ");
		final Pattern[] footnoteStartPatterns;
		if (footnoteStartPatternStrs == null)
			footnoteStartPatterns = this.footnoteStartPatterns;
		else {
			footnoteStartPatterns = new Pattern[footnoteStartPatternStrs.length];
			for (int p = 0; p < footnoteStartPatternStrs.length; p++)
				footnoteStartPatterns[p] = Pattern.compile(footnoteStartPatternStrs[p]);
		}
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int pg) throws Exception {
				spm.setProgress((pg * 100) / pages.length);
				BoundingBox footnoteArea = docLayout.getBoxProperty("footnote.area", pages[pg].bounds, pageImageDpi);
				ImRegion[] pageParagraphs = pages[pg].getRegions(PARAGRAPH_TYPE);
				boolean newFootnoteFound;
				do {
					newFootnoteFound = false;
					for (int p = 0; p < pageParagraphs.length; p++) {
						if (!footnoteArea.includes(pageParagraphs[p].bounds, false))
							continue;
						ImWord[] paragraphWords = getLargestTextStreamWords(pageParagraphs[p].getWords());
						if (paragraphWords.length == 0)
							continue;
						Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
						if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(paragraphWords[0].getTextStreamType()))
							continue;
						spm.setInfo("Testing paragraph " + pageParagraphs[p].bounds + " on page " + pages[pg].pageId + " for footnote");
						boolean nonFootnoteBelow = false;
						for (int cp = 0; cp < pageParagraphs.length; cp++) {
							if (cp == p)
								continue;
							if ((pageParagraphs[p].bounds.right < pageParagraphs[cp].bounds.left) || (pageParagraphs[cp].bounds.right < pageParagraphs[p].bounds.left))
								continue;
							if (pageParagraphs[cp].bounds.bottom < pageParagraphs[p].bounds.top)
								continue;
							ImWord[] cRegionWords = pageParagraphs[cp].getWords();
							if (cRegionWords.length == 0)
								continue;
							Arrays.sort(cRegionWords, ImUtils.textStreamOrder);
							if (!ImWord.TEXT_STREAM_TYPE_FOOTNOTE.equals(cRegionWords[0].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(cRegionWords[0].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(cRegionWords[0].getTextStreamType())) {
								nonFootnoteBelow = true;
								break;
							}
						}
						if (nonFootnoteBelow) {
							spm.setInfo(" ==> non-footnote below");
							continue;
						}
						if (isFootnote(pageParagraphs[p], paragraphWords, docFontSize, footnoteStartPatterns, footnoteMinFontSize, footnoteMaxFontSize)) {
							spm.setInfo(" ==> footnote detected");
							synchronized (doc) {
								ImUtils.makeStream(paragraphWords, ImWord.TEXT_STREAM_TYPE_FOOTNOTE, FOOTNOTE_TYPE);
								ImUtils.orderStream(paragraphWords, ImUtils.leftRightTopDownOrder);
							}
							newFootnoteFound = true;
						}
						else spm.setInfo(" ==> not a footnote");
					}
				} while (newFootnoteFound);
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	TODO detect in-text notes (paragraphs whose average font size is significantly smaller than main text) 
		
		//	index paragraph ends (in preparation for paragraph merging)
		spm.setStep(" - indexing paragraph end words");
		spm.setBaseProgress(55);
		spm.setMaxProgress(60);
		HashMap paragraphEndWords = new HashMap();
		HashMap paragraphEndWordBlocks = new HashMap();
		for (int pg = 0; pg < pages.length; pg++) {
			spm.setProgress((pg * 100) / pages.length);
			spm.setInfo("Indexing paragraphs on page " + pages[pg].pageId);
			ImRegion[] pageParagraphs = pages[pg].getRegions(ImRegion.PARAGRAPH_TYPE);
			for (int p = 0; p < pageParagraphs.length; p++) {
				ImWord[] paragraphWords = pageParagraphs[p].getWords();
				if (paragraphWords.length == 0)
					continue;
				Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
				paragraphEndWords.put(paragraphWords[paragraphWords.length-1], pageParagraphs[p]);
			}
			ImRegion[] pageBlocks = pages[pg].getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
			for (int b = 0; b < pageBlocks.length; b++) {
				ImWord[] blockWords = pageBlocks[b].getWords();
				for (int w = 0; w < blockWords.length; w++) {
					if (paragraphEndWords.containsKey(blockWords[w]))
						paragraphEndWordBlocks.put(blockWords[w], pageBlocks[b]);
				}
			}
		}
		
		//	de-hyphenate line breaks within paragraphs
		spm.setStep(" - de-hyphenating line breaks");
		spm.setBaseProgress(60);
		spm.setMaxProgress(63);
		textStreamHeads = doc.getTextStreamHeads();
		for (int h = 0; h < textStreamHeads.length; h++) {
			spm.setProgress((h * 100) / textStreamHeads.length);
			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			for (ImWord imw = textStreamHeads[h]; imw.getNextWord() != null; imw = imw.getNextWord()) {
				ImWord nextImw = imw.getNextWord();
				
				//	these two have a paragraph break between them, we're handling these more complex cases below
				if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
					continue;
				
				//	little we can do here ...
				if ((imw.getString() == null) || (imw.getString().length() == 0))
					continue;
				
				//	not a line break
				if ((imw.bounds.left < nextImw.bounds.left) || (nextImw.bounds.top < imw.bounds.bottom))
					continue;
				
				//	this one's last in its paragraph, we're handling these more complex cases below
				ImRegion imwParagraph = ((ImRegion) paragraphEndWords.get(imw));
				if (imwParagraph != null)
					continue;
				
				//	no use checking with a sentence end
				if (Gamta.isSentenceEnd(imw.getString()))
					continue;
				else if (Gamta.isClosingBracket(imw.getString()) && (imw.getPreviousWord() != null) && (imw.getPreviousWord().getString() != null) && (imw.getPreviousWord().getString().length() != 0) && Gamta.isSentenceEnd(imw.getPreviousWord().getString()))
					continue;
				
				//	check for hyphenation
				boolean imwHyphenated = ((imw.getString() != null) && imw.getString().matches(".+[\\-\\u00AD\\u2010-\\u2015\\u2212]"));
				boolean nImwContinues = false;
				if (imwHyphenated) {
					String nextWordString = nextImw.getString();
					if (nextWordString == null) {} // little we can do here ...
					else if (nextWordString.length() == 0) {} // ... or here
					else if (nextWordString.charAt(0) == Character.toUpperCase(nextWordString.charAt(0))) {} // starting with capital letter, not a word continued
					else if ("and;or;und;oder;et;ou;y;e;o;u;ed".indexOf(nextWordString.toLowerCase()) != -1) {} // rather looks like an enumeration continued than a word (western European languages for now)
					else nImwContinues = true;
				}
				
				//	we do have a hyphenated word
				if (imwHyphenated && nImwContinues)
					imw.setNextRelation(ImWord.NEXT_RELATION_HYPHENATED);
			}
		}
		
		//	get minimum block margin from document style to estimate vertical block distance
		int minBlockMargin = docLayout.getIntProperty("minBlockMargin", 0, pageImageDpi);
		
		//	try and merge paragraphs
		spm.setStep(" - merging interrupted paragraphs");
		spm.setBaseProgress(63);
		spm.setMaxProgress(65);
		for (int h = 0; h < textStreamHeads.length; h++) {
			spm.setProgress((h * 100) / textStreamHeads.length);
			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				continue;
			for (ImWord imw = textStreamHeads[h]; imw.getNextWord() != null; imw = imw.getNextWord()) {
				ImWord nextImw = imw.getNextWord();
				
				//	these two don't have a paragraph break between them
				if (imw.getNextRelation() != ImWord.NEXT_RELATION_PARAGRAPH_END)
					continue;
				
				//	little we can do here ...
				if ((imw.getString() == null) || (imw.getString().length() == 0))
					continue;
				
				//	this one's not last in its paragraph
				ImRegion imwParagraph = ((ImRegion) paragraphEndWords.get(imw));
				if (imwParagraph == null)
					continue;
				
				//	this one's too far left in its paragraph (further than 10% of width away from right boundary)
				if (((imwParagraph.bounds.right - imw.bounds.right) * 10) > (imwParagraph.bounds.right - imwParagraph.bounds.left))
					continue;
				
				//	get parent block to compare width (helps with single-line paragraphs)
				ImRegion imwBlock = ((ImRegion) paragraphEndWordBlocks.get(imw));
				if (imwBlock == null)
					continue;
				
				//	this one's too far left in its block (further than 10% of width away from right boundary)
				if (((imwBlock.bounds.right - imw.bounds.right) * 10) > (imwBlock.bounds.right - imwBlock.bounds.left))
					continue;
				
				//	no use checking with a sentence end
				if (Gamta.isSentenceEnd(imw.getString()))
					continue;
				else if (Gamta.isClosingBracket(imw.getString()) && (imw.getPreviousWord() != null) && (imw.getPreviousWord().getString() != null) && (imw.getPreviousWord().getString().length() != 0) && Gamta.isSentenceEnd(imw.getPreviousWord().getString()))
					continue;
				
				//	test if we have to check these two
				boolean ignoreWordRelation = true;
				if (imw.pageId != nextImw.pageId) // page break
					ignoreWordRelation = false;
				else if ((imw.bounds.right < nextImw.bounds.left) && (nextImw.bounds.bottom < imw.bounds.top)) // column break (successor up and right)
					ignoreWordRelation = false;
				else if (Math.max((minBlockMargin * 2), ((imw.bounds.bottom - imw.bounds.top) + (nextImw.bounds.bottom - nextImw.bounds.top))) < (nextImw.bounds.top - imw.bounds.bottom)) // continuation after artifact (successor way down)
					ignoreWordRelation = false;
				
				//	we don't seem to have to investigate this one
				if (ignoreWordRelation)
					continue;
				
				//	check for hyphenation
				boolean imwHyphenated = ((imw.getString() != null) && imw.getString().matches(".+[\\-\\u00AD\\u2010-\\u2015\\u2212]"));
				boolean nImwContinues = false;
				if (imwHyphenated) {
					String nextWordString = nextImw.getString();
					if (nextWordString == null) {} // little we can do here ...
					else if (nextWordString.length() == 0) {} // ... or here
					else if (nextWordString.charAt(0) == Character.toUpperCase(nextWordString.charAt(0))) {} // starting with capital letter, not a word continued
					else if ("and;or;und;oder;et;ou;y;e;o;u;ed".indexOf(nextWordString.toLowerCase()) != -1) {} // rather looks like an enumeration continued than a word (western European languages for now)
					else nImwContinues = true;
				}
				
				//	we do have a hyphenated word
				if (imwHyphenated && nImwContinues)
					imw.setNextRelation(ImWord.NEXT_RELATION_HYPHENATED);
				
				//	we have two separate words
				else imw.setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
			}
		}
		
		//	TODO add in-text notes residing between table and caption to caption
		
		//	collect blocks that might be assigned captions
		spm.setStep(" - identifying caption target areas");
		spm.setBaseProgress(65);
		spm.setMaxProgress(75);
		final HashMap tablesToCaptionAnnots = new HashMap();
		final boolean figureAboveCaptions = docLayout.getBooleanProperty("caption.belowFigure", true);
		final boolean figureBelowCaptions = docLayout.getBooleanProperty("caption.aboveFigure", false);
		final boolean figureBesideCaptions = docLayout.getBooleanProperty("caption.besideFigure", true);
		final boolean tableAboveCaptions = docLayout.getBooleanProperty("caption.belowTable", true);
		final boolean tableBelowCaptions = docLayout.getBooleanProperty("caption.aboveTable", true);
		final boolean tableBesideCaptions = docLayout.getBooleanProperty("caption.besideTable", false);
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				
				//	no captions in this page
				if (pageCaptions[p] == null)
					return;
				
				//	collect possible caption target areas
				HashMap aboveCaptionTargets = new HashMap();
				HashMap belowCaptionTargets = new HashMap();
				HashMap besideCaptionTargets = new HashMap();
				for (Iterator cit = pageCaptions[p].keySet().iterator(); cit.hasNext();) {
					ImRegion caption = ((ImRegion) cit.next());
					ImAnnotation captionAnnot = ((ImAnnotation) pageCaptions[p].get(caption));
					boolean isTableCaption = captionAnnot.getFirstWord().getString().toLowerCase().startsWith("tab"); // covers English, German, French, Italian, and Spanish
					if (isTableCaption ? tableAboveCaptions : figureAboveCaptions) {
						ImRegion aboveCaptionTarget = getAboveCaptionTarget(pages[p], caption, pageImageDpi, isTableCaption);
						if (aboveCaptionTarget != null)
							aboveCaptionTargets.put(caption, aboveCaptionTarget);
					}
					if (isTableCaption ? tableBelowCaptions : figureBelowCaptions) {
						ImRegion belowCaptionTarget = getBelowCaptionTarget(pages[p], caption, pageImageDpi, isTableCaption);
						if (belowCaptionTarget != null)
							belowCaptionTargets.put(caption, belowCaptionTarget);
					}
					if (isTableCaption ? tableBesideCaptions : figureBesideCaptions) {
						ImRegion besideCaptionTarget = getBesideCaptionTarget(pages[p], caption, pageImageDpi, isTableCaption);
						if (besideCaptionTarget != null)
							besideCaptionTargets.put(caption, besideCaptionTarget);
					}
				}
				
				//	assign target areas to captions (unambiguous ones first, then remaining ones)
				ImRegion[] captions = null;
				boolean skipAmbiguousCaptions = true;
				ArrayList assignedCaptionTargets = new ArrayList();
				do {
					
					//	activate ambiguous captions if no new assignments in previous round
					if ((captions != null) && (captions.length == pageCaptions[p].size()))
						skipAmbiguousCaptions = false;
					
					//	get remaining captions
					captions = ((ImRegion[]) pageCaptions[p].keySet().toArray(new ImRegion[pageCaptions[p].size()]));
					for (int c = 0; c < captions.length; c++) {
						
						//	get candidate targets, and check if still available
						ImRegion act = ((ImRegion) aboveCaptionTargets.get(captions[c]));
						if (act != null) {
							for (int a = 0; a < assignedCaptionTargets.size(); a++)
								if (((ImRegion) assignedCaptionTargets.get(a)).bounds.overlaps(act.bounds)) {
									 aboveCaptionTargets.remove(captions[c]);
									 act = null;
									 break;
								}
						}
						ImRegion bct = ((ImRegion) belowCaptionTargets.get(captions[c]));
						if (bct != null) {
							for (int a = 0; a < assignedCaptionTargets.size(); a++)
								if (((ImRegion) assignedCaptionTargets.get(a)).bounds.overlaps(bct.bounds)) {
									 belowCaptionTargets.remove(captions[c]);
									 bct = null;
									 break;
								}
						}
						ImRegion sct = ((ImRegion) besideCaptionTargets.get(captions[c]));
						if (sct != null) {
							for (int a = 0; a < assignedCaptionTargets.size(); a++)
								if (((ImRegion) assignedCaptionTargets.get(a)).bounds.overlaps(sct.bounds)) {
									 besideCaptionTargets.remove(captions[c]);
									 sct = null;
									 break;
								}
						}
						
						//	count targets
						int ctCount = (((act == null) ? 0 : 1) + ((bct == null) ? 0 : 1) + ((sct == null) ? 0 : 1));
						
						//	no target found or left for this one
						if (ctCount == 0) {
							pageCaptions[p].remove(captions[c]);
							continue;
						}
						
						//	this one's ambiguous, save for another round
						if (skipAmbiguousCaptions && (ctCount != 1))
							continue;
						
						//	get target area (prefer above over below, and below over beside)
						ImRegion ct = ((act == null) ? ((bct == null) ? sct : bct) : act);
						
						//	get annotation and check target type
						ImAnnotation captionAnnot = ((ImAnnotation) pageCaptions[p].get(captions[c]));
						
						//	mark table caption
						if (ImRegion.TABLE_TYPE.equals(ct.getType())) {
							captionAnnot.setAttribute("targetIsTable");
							synchronized (tablesToCaptionAnnots) {
								tablesToCaptionAnnots.put(ct, captionAnnot);
							}
						}
						
						//	mark image region, and clean up contents
						else {
							ct.setType(ImRegion.IMAGE_TYPE);
							if (ct.getPage() == null) {
								pages[p].addRegion(ct);
								ImRegion[] ctRegions = ct.getRegions();
								for (int r = 0; r < ctRegions.length; r++) {
									if (ctRegions[r] != ct)
										pages[p].removeRegion(ctRegions[r]);
								}
							}
						}
						
						//	set target attributes
						captionAnnot.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + ct.pageId));
						captionAnnot.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, ct.bounds.toString());
						
						//	remember we have assigned this one
						assignedCaptionTargets.add(ct);
						
						//	clean up
						pageCaptions[p].remove(captions[c]);
						aboveCaptionTargets.remove(captions[c]);
						belowCaptionTargets.remove(captions[c]);
						besideCaptionTargets.remove(captions[c]);
					}
				}
				
				//	keep going while either new assignments happen, or ambiguous captions are left to handle
				while (captions.length > pageCaptions[p].size() || skipAmbiguousCaptions);
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	merge tables within pages, and collect large ones along the way for cross-page mergers
		spm.setStep(" - merging tables within pages");
		spm.setBaseProgress(75);
		spm.setMaxProgress(80);
		final LinkedList docTableList = new LinkedList();
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				spm.setProgress((p * 100) / pages.length);
				ImRegion[] pageTables = pages[p].getRegions(ImRegion.TABLE_TYPE);
				if (pageTables.length == 0) {
					spm.setInfo("No tables to merge on page " + pages[p].pageId);
					return;
				}
				
				//	sort out large tables (over 70% of page in each direction), and collect smaller ones
				LinkedList pageTableList = new LinkedList();
				for (int t = 0; t < pageTables.length; t++) {
					if ((((pageTables[t].bounds.right - pageTables[t].bounds.left) * 10) < ((pages[p].bounds.right - pages[p].bounds.left) * 7)) || ((pageTables[t].bounds.bottom - pageTables[t].bounds.top) * 10) < ((pages[p].bounds.bottom - pages[p].bounds.top) * 7)) {
						pageTableList.add(pageTables[t]);
						if (pageTables.length == 1)
							synchronized (docTableList) {
								docTableList.add(pageTables[t]);
							}
					}
					else synchronized (docTableList) {
						docTableList.add(pageTables[t]);
					}
				}
				
				//	anything left to handle on this page?
				if (pageTableList.size() < 2) {
					spm.setInfo("No tables to merge on page " + pages[p].pageId);
					return;
				}
				else if (pageTableList.size() < pageTables.length)
					pageTables = ((ImRegion[]) pageTableList.toArray(new ImRegion[pageTableList.size()]));
				
				//	investigate tables top-down to find row mergers (column mergers make no sense here)
				Arrays.sort(pageTables, ImUtils.topDownOrder);
				for (int t = 0; t < pageTables.length; t++) {
					ImAnnotation tableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(pageTables[t]));
					spm.setInfo("Attempting to row-merge table " + pageTables[t].bounds + " on page " + pageTables[t].pageId);
					String tableCaptionStart = ((tableCaption == null) ? null : getCaptionStartForCheck(tableCaption));
					spm.setInfo(" - caption start is " + ((tableCaptionStart == null) ? "null" : ("'" + tableCaptionStart + "'")));
					for (int c = (t+1); c < pageTables.length; c++) {
						spm.setInfo(" - comparing to table " + pageTables[c].bounds);
						if (!ImUtils.areTableRowsCompatible(pageTables[t], pageTables[c])) {
							spm.setInfo(" --> rows not compatible");
							continue;
						}
						ImAnnotation cTableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(pageTables[c]));
						String cTableCaptionStart = ((cTableCaption == null) ? null : getCaptionStartForCheck(cTableCaption));
						if ((tableCaptionStart != null) && (cTableCaptionStart != null) && !tableCaptionStart.equalsIgnoreCase(cTableCaptionStart)) {
							spm.setInfo(" --> caption start '" + cTableCaptionStart + "' not compatible");
							continue;
						}
						ImUtils.connectTableRows(pageTables[t], pageTables[c]);
						spm.setInfo(" --> table rows merged");
						break;
					}
				}
				
				//	investigate tables left-right to find column mergers (row mergers make no sense here)
				Arrays.sort(pageTables, ImUtils.leftRightOrder);
				for (int t = 0; t < pageTables.length; t++) {
					ImAnnotation tableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(pageTables[t]));
					spm.setInfo("Attempting to column-merge table " + pageTables[t].bounds + " on page " + pageTables[t].pageId);
					String tableCaptionStart = ((tableCaption == null) ? null : getCaptionStartForCheck(tableCaption));
					spm.setInfo(" - caption start is " + ((tableCaptionStart == null) ? "null" : ("'" + tableCaptionStart + "'")));
					for (int c = (t+1); c < pageTables.length; c++) {
						spm.setInfo(" - comparing to table " + pageTables[c].bounds);
						if (!ImUtils.areTableColumnsCompatible(pageTables[t], pageTables[c])) {
							spm.setInfo(" --> columns not compatible");
							continue;
						}
						ImAnnotation cTableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(pageTables[c]));
						String cTableCaptionStart = ((cTableCaption == null) ? null : getCaptionStartForCheck(cTableCaption));
						if ((tableCaptionStart != null) && (cTableCaptionStart != null) && !tableCaptionStart.equalsIgnoreCase(cTableCaptionStart)) {
							spm.setInfo(" --> caption start '" + cTableCaptionStart + "' not compatible");
							continue;
						}
						ImUtils.connectTableColumns(pageTables[t], pageTables[c]);
						spm.setInfo(" --> table columns merged");
						break;
					}
				}
			}
		}, pages.length, (this.debug ? 1 : (pages.length / 8)));
		
		//	merge tables across pages
		spm.setStep(" - merging tables across pages");
		ImRegion[] docTables;
		
		//	investigate row mergers
		docTables = ((ImRegion[]) docTableList.toArray(new ImRegion[docTableList.size()]));
		docTableList.clear();
		Arrays.sort(docTables, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImRegion reg1 = ((ImRegion) obj1);
				ImRegion reg2 = ((ImRegion) obj2);
				return ((reg1.pageId == reg2.pageId) ? ImUtils.topDownOrder.compare(reg1, reg2) : (reg1.pageId - reg2.pageId));
			}
		});
		spm.setBaseProgress(75);
		spm.setMaxProgress(80);
		for (int t = 0; t < docTables.length; t++) {
			spm.setProgress((t * 100) / docTables.length);
			ImAnnotation tableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[t]));
			spm.setInfo("Attempting to row-merge table " + docTables[t].bounds + " on page " + docTables[t].pageId);
			String tableCaptionStart = ((tableCaption == null) ? null : this.getCaptionStartForCheck(tableCaption));
			spm.setInfo(" - caption start is " + ((tableCaptionStart == null) ? "null" : ("'" + tableCaptionStart + "'")));
			for (int c = (t+1); c < docTables.length; c++) {
				spm.setInfo(" - comparing to table " + docTables[c].bounds + " on page " + docTables[c].pageId);
				if ((docTables[c].pageId - docTables[t].pageId) > 2) {
					spm.setInfo(" --> too many pages in between");
					break;
				}
				if (!ImUtils.areTableRowsCompatible(docTables[t], docTables[c])) {
					spm.setInfo(" --> rows not compatible");
					continue;
				}
				ImAnnotation cTableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[c]));
				String cTableCaptionStart = ((cTableCaption == null) ? null : this.getCaptionStartForCheck(cTableCaption));
				if ((tableCaptionStart != null) && (cTableCaptionStart != null) && !tableCaptionStart.equalsIgnoreCase(cTableCaptionStart)) {
					spm.setInfo(" --> caption start '" + cTableCaptionStart + "' not compatible");
					continue;
				}
				ImUtils.connectTableRows(docTables[t], docTables[c]);
				spm.setInfo(" --> table rows merged");
				break;
			}
			
			//	collect non-merged tables as well as leftmost tables of table grid rows
			if (!docTables[t].hasAttribute("rowsContinueFrom"))
				docTableList.add(docTables[t]);
		}
		
		//	investigate column mergers
		docTables = ((ImRegion[]) docTableList.toArray(new ImRegion[docTableList.size()]));
		docTableList.clear();
		spm.setBaseProgress(80);
		spm.setMaxProgress(85);
		for (int t = 0; t < docTables.length; t++) {
			spm.setProgress((t * 100) / docTables.length);
			ImAnnotation tableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[t]));
			spm.setInfo("Attempting to column-merge table " + docTables[t].bounds + " on page " + docTables[t].pageId);
			String tableCaptionStart = ((tableCaption == null) ? null : this.getCaptionStartForCheck(tableCaption));
			spm.setInfo(" - caption start is " + ((tableCaptionStart == null) ? "null" : ("'" + tableCaptionStart + "'")));
			ImRegion[] tableGridRow = ImUtils.getRowConnectedTables(docTables[t]);
			spm.setInfo(" - got " + tableGridRow.length + " row-connected tables");
			for (int c = (t+1); c < docTables.length; c++) {
				spm.setInfo(" - comparing to table " + docTables[c].bounds + " on page " + docTables[c].pageId);
				if (!ImUtils.areTableColumnsCompatible(docTables[t], docTables[c])) {
					spm.setInfo(" --> columns not compatible");
					continue;
				}
				ImAnnotation cTableCaption = ((ImAnnotation) tablesToCaptionAnnots.get(docTables[c]));
				String cTableCaptionStart = ((cTableCaption == null) ? null : this.getCaptionStartForCheck(cTableCaption));
				if ((tableCaptionStart != null) && (cTableCaptionStart != null) && !tableCaptionStart.equalsIgnoreCase(cTableCaptionStart)) {
					spm.setInfo(" --> caption start '" + cTableCaptionStart + "' not compatible");
					continue;
				}
				ImRegion[] cTableGridRow = ImUtils.getRowConnectedTables(docTables[c]);
				if (tableGridRow.length != cTableGridRow.length) {
					spm.setInfo(" --> row-connected tables not compatible");
					continue;
				}
				ImUtils.connectTableColumns(docTables[t], docTables[c]);
				spm.setInfo(" --> table columns merged");
				break;
			}
		}
		
		//	tag and connect caption citations
		spm.setStep(" - marking caption citations");
		spm.setBaseProgress(85);
		spm.setMaxProgress(90);
		this.captionCitationHandler.markCaptionCitations(doc, textStreamHeads);
		
		//	mark headings, as well as bold/italics emphases
		spm.setStep(" - marking headings and emphases");
		spm.setBaseProgress(90);
		spm.setMaxProgress(100);
		
		//	mark emphases
		HashSet emphasisWords = new HashSet();
		for (int h = 0; h < textStreamHeads.length; h++) {
			ImWord boldStart = null;
			ImWord italicsStart = null;
			for (ImWord imw = textStreamHeads[h]; imw != null; imw = imw.getNextWord()) {
				boolean markedBoldEmphasis = false;
				boolean markedItalicsEmphasis = false;
				
				//	get font size
				String imwFontSize = ((String) imw.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
				
				//	finish bold emphasis (for style reasons, or because paragraph ends)
				if ((boldStart != null) && (!imw.hasAttribute(ImWord.BOLD_ATTRIBUTE) || (imw.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END) || ((imwFontSize != null) && !imwFontSize.equals(boldStart.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, imwFontSize))))) {
					ImAnnotation emphasis = doc.addAnnotation(boldStart, imw.getPreviousWord(), ImAnnotation.EMPHASIS_TYPE);
					emphasis.setAttribute(ImWord.BOLD_ATTRIBUTE);
					markedBoldEmphasis = true;
					if ((italicsStart != null) && (ImUtils.textStreamOrder.compare(italicsStart, boldStart) <= 0)) {
						emphasis.setAttribute(ImWord.ITALICS_ATTRIBUTE);
						spm.setInfo("Found bold+italics emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
						if ((boldStart == italicsStart) && !imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
							italicsStart = null;
					}
					else spm.setInfo("Found bold emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
					
					//	remember emphasized words
					for (ImWord eImw = emphasis.getFirstWord(); eImw != null; eImw = eImw.getNextWord()) {
						emphasisWords.add(eImw);
						if (eImw == emphasis.getLastWord())
							break;
					} // TODO_ne reactivate regular code once bold face detection in scans more reliable
//					if (documentBornDigital || (italicsStart != null)) {
//						ImAnnotation emphasis = doc.addAnnotation(boldStart, imw.getPreviousWord(), ImAnnotation.EMPHASIS_TYPE);
//						emphasis.setAttribute(ImWord.BOLD_ATTRIBUTE);
//						markedBoldEmphasis = true;
//						if ((italicsStart == null) || (ImUtils.textStreamOrder.compare(boldStart, italicsStart) < 0))
//							spm.setInfo("Found bold emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
//						else {
//							emphasis.setAttribute(ImWord.ITALICS_ATTRIBUTE);
//							spm.setInfo("Found bold+italics emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
//							if ((boldStart == italicsStart) && !imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
//								italicsStart = null;
//						}
//						
//						//	remember emphasized words
//						for (ImWord eImw = emphasis.getFirstWord(); eImw != null; eImw = eImw.getNextWord()) {
//							emphasisWords.add(eImw);
//							if (eImw == emphasis.getLastWord())
//								break;
//						}
//					}
//					else markedBoldEmphasis = true;
				}
				
				//	finish italics emphasis (for style reasons, or because paragraph ends)
				if ((italicsStart != null) && (!imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE) || (imw.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END) || ((imwFontSize != null) && !imwFontSize.equals(italicsStart.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, imwFontSize))))) {
					ImAnnotation emphasis = doc.addAnnotation(italicsStart, imw.getPreviousWord(), ImAnnotation.EMPHASIS_TYPE);
					emphasis.setAttribute(ImWord.ITALICS_ATTRIBUTE);
					markedItalicsEmphasis = true;
					if ((boldStart != null) && (ImUtils.textStreamOrder.compare(boldStart, italicsStart) <= 0)) {
						emphasis.setAttribute(ImWord.BOLD_ATTRIBUTE);
						spm.setInfo("Found italics+bold emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
						if ((italicsStart == boldStart) && !imw.hasAttribute(ImWord.BOLD_ATTRIBUTE))
							boldStart = null;
					}
					else spm.setInfo("Found italics emphasis on '" + emphasis.getFirstWord().getTextStreamType() + "' text stream: " + ImUtils.getString(emphasis.getFirstWord(), emphasis.getLastWord(), true));
					
					//	remember emphasized words
					for (ImWord eImw = emphasis.getFirstWord(); eImw != null; eImw = eImw.getNextWord()) {
						emphasisWords.add(eImw);
						if (eImw == emphasis.getLastWord())
							break;
					}
				}
				
				//	set or reset emphasis starts (possible only now, as we need both in marking either)
				if (imw.hasAttribute(ImWord.BOLD_ATTRIBUTE)) {
					if (((boldStart == null) && (".,;:".indexOf(imw.getString()) == -1)) || markedBoldEmphasis)
						boldStart = imw;
				}
				else boldStart = null;
				if (imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE)) {
					if (((italicsStart == null) && (".,;:".indexOf(imw.getString()) == -1)) || markedItalicsEmphasis)
						italicsStart = imw;
				}
				else italicsStart = null;
			}
		}
		
		//	get heading styles
		HeadingStyleDefined[] headingStyles = null;
		DocumentStyle headingStyle = docStyle.getSubset("style.heading");
		int[] headingLevels = headingStyle.getIntListProperty("levels", null);
		if (headingLevels != null) {
			headingStyles = new HeadingStyleDefined[headingLevels.length];
			for (int l = 0; l < headingLevels.length; l++)
				headingStyles[l] = new HeadingStyleDefined(headingLevels[l], headingStyle);
		}
		
		//	extract headings using heuristics
		if (headingStyles == null) {
			
			//	mark headings
			ArrayList headings = new ArrayList();
			for (int pg = 0; pg < pages.length; pg++) {
				spm.setProgress((pg * 100) / pages.length);
				this.markHeadings(pages[pg], docFontSize, pageImageDpi, emphasisWords, spm, headings);
			}
			
			//	assess hierarchy of headings
			spm.setStep(" - assessing hierarchy of headings");
			this.assessHeadingHierarchy(headings, docFontSize, pages[0].pageId, spm);
			
		}
		
		//	extract headings using style templates
		else for (int pg = 0; pg < pages.length; pg++) {
			spm.setProgress((pg * 100) / pages.length);
			this.markHeadings(pages[pg], pageImageDpi, headingStyles, spm);
		}
		
		//	finally, we're done
		spm.setProgress(100);
	}
	
	private void assessHeadingHierarchy(ArrayList headings, int docFontSize, int docFirstPageId, ProgressMonitor pm) {
		
		//	collect properties of headings (font size, bold, all-caps, centered, numbering), as well as their global position among all headings
		Collections.sort(headings, annotationTextStreamOrder);
		TreeMap headingStylesByKey = new TreeMap(Collections.reverseOrder());
		for (int h = 0; h < headings.size(); h++) {
			ImAnnotation heading = ((ImAnnotation) headings.get(h));
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStylesByKey.get(HeadingStyleObserved.getStyleKey(heading)));
			if (headingStyle == null) {
				headingStyle = new HeadingStyleObserved(heading);
				headingStylesByKey.put(headingStyle.key, headingStyle);
			}
			else headingStyle.headings.add(heading);
		}
		ArrayList headingStyles = new ArrayList();
		for (Iterator hskit = headingStylesByKey.keySet().iterator(); hskit.hasNext();)
			headingStyles.add(headingStylesByKey.get(hskit.next()));
		pm.setInfo("Got " + headingStyles.size() + " initial heading styles");
		
		//	try and merge heading styles that are equal in everything but the font size differing by one
		for (int s = 0; s < headingStyles.size(); s++) {
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStyles.get(s));
			for (int cs = (s+1); cs < headingStyles.size(); cs++) {
				HeadingStyleObserved cHeadingStyle = ((HeadingStyleObserved) headingStyles.get(cs));
				if (headingStyle.bold != cHeadingStyle.bold)
					continue;
				if (headingStyle.allCaps != cHeadingStyle.allCaps)
					continue;
				if (headingStyle.centered != cHeadingStyle.centered)
					continue;
				if (Math.abs(headingStyle.fontSize - cHeadingStyle.fontSize) > 1)
					continue;
				if ((headingStyle.headings.size() > 1) && (cHeadingStyle.headings.size() > 1))
					continue;
				if (headingStyle.headings.size() < cHeadingStyle.headings.size()) {
					pm.setInfo("Merging " + headingStyle.key + " into " + cHeadingStyle.key);
					cHeadingStyle.headings.addAll(headingStyle.headings);
					headingStyles.remove(s--);
					break;
				}
				else {
					pm.setInfo("Merging " + cHeadingStyle.key + " into " + headingStyle.key);
					headingStyle.headings.addAll(cHeadingStyle.headings);
					headingStyles.remove(cs--);
				}
			}
		}
		
		//	score square distances between headings of each style
		for (int s = 0; s < headingStyles.size(); s++) {
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStyles.get(s));
			Collections.sort(headingStyle.headings, annotationTextStreamOrder);
			int lastPos = -1;
			int posDistSquareSum = 0;
			for (int h = 0; h < headingStyle.headings.size(); h++) {
				ImAnnotation heading = ((ImAnnotation) headingStyle.headings.get(h));
				int pos = headings.indexOf(heading);
				if (lastPos != -1)
					posDistSquareSum += ((pos - lastPos) * (pos - lastPos));
				lastPos = pos;
			}
			if (headingStyle.headings.size() > 1)
				headingStyle.avgPosDistSquare = (posDistSquareSum / (headingStyle.headings.size() - 1));
		}
		
		//	what do we got?
		pm.setInfo("Got " + headingStyles.size() + " heading styles:");
		for (int s = 0; s < headingStyles.size(); s++) {
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStyles.get(s));
			pm.setInfo(" - " + headingStyle.key + " with " + headingStyle.headings.size() + " headings:");
			for (int h = 0; h < headingStyle.headings.size(); h++) {
				ImAnnotation heading = ((ImAnnotation) headingStyle.headings.get(h));
				headingStyle.firstPageId = Math.min(headingStyle.firstPageId, heading.getFirstWord().pageId);
				headingStyle.lastPageId = Math.max(headingStyle.lastPageId, heading.getLastWord().pageId);
				pm.setInfo("   - " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true) + " (at position " + headings.indexOf(heading) + ")");
			}
			pm.setInfo(" --> average position distance is " + headingStyle.avgPosDistSquare);
			pm.setInfo(" --> first page ID is " + headingStyle.firstPageId);
			pm.setInfo(" --> last page ID is " + headingStyle.lastPageId);
			
			//	sort out document headers right away
			if (headingStyle.headings.size() > 1)
				continue;
			if ((headingStyle.firstPageId != docFirstPageId) || (headingStyle.lastPageId != docFirstPageId))
				continue;
			if (headingStyle.fontSize < docFontSize)
				continue;
			pm.setInfo(" ==> document head");
			((ImAnnotation) headingStyle.headings.get(0)).setAttribute("level", "0");
			headingStyles.remove(s--);
		}
		
		//	from that, try and establish heading style hierarchy
		Collections.sort(headingStyles, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				HeadingStyleObserved hs1 = ((HeadingStyleObserved) obj1);
				HeadingStyleObserved hs2 = ((HeadingStyleObserved) obj2);
				if (hs1.allCaps == hs2.allCaps) {
					if (hs1.fontSize != hs2.fontSize)
						return (hs2.fontSize - hs1.fontSize);
					else if (hs1.bold != hs2.bold)
						return (hs1.bold ? -1 : 1);
					else if ((hs2.avgPosDistSquare != -1) && (hs1.avgPosDistSquare != -1))
						return (hs2.avgPosDistSquare - hs1.avgPosDistSquare);
					else return 0;
				}
				else if (hs1.fontSize == hs2.fontSize) {
					if ((hs1.allCaps != hs2.allCaps) && ((hs1.bold == hs2.bold) || (hs1.bold == hs1.allCaps)))
						return (hs1.allCaps ? -1 : 1);
					else if ((hs1.bold != hs2.bold) && ((hs1.allCaps == hs2.allCaps) || (hs1.allCaps == hs1.bold)))
						return (hs1.bold ? -1 : 1);
					else return 0;
				}
				else if ((hs2.avgPosDistSquare != -1) && (hs1.avgPosDistSquare != -1))
					return (hs2.avgPosDistSquare - hs1.avgPosDistSquare);
				else return 0;
			}
		});
		
		//	set heading level attributes
		pm.setInfo("Sorted " + headingStyles.size() + " heading styles:");
		for (int s = 0; s < headingStyles.size(); s++) {
			HeadingStyleObserved headingStyle = ((HeadingStyleObserved) headingStyles.get(s));
			pm.setInfo(" - level " + (s+1) + ": " + headingStyle.key + " with " + headingStyle.headings.size() + " headings:");
			for (int h = 0; h < headingStyle.headings.size(); h++) {
				ImAnnotation heading = ((ImAnnotation) headingStyle.headings.get(h));
				heading.setAttribute("level", ("" + (s+1)));
				pm.setInfo("   - " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true) + " (at position " + headings.indexOf(heading) + ")");
			}
		}
	}
	
	private static class HeadingStyleDefined {
		final int level;
		int minFontSize;
		int maxFontSize;
		boolean isBold;
		boolean isAllCaps;
		Pattern[] startPatterns;
		boolean isLeftAligned;
		boolean isRightAligned;
		HeadingStyleDefined(int level, DocumentStyle style) {
			this.level = level;
			int fontSize = style.getIntProperty((this.level + ".fontSize"), -1);
			this.minFontSize = style.getIntProperty((this.level + ".minFontSize"), ((fontSize == -1) ? 0 : fontSize));
			this.maxFontSize = style.getIntProperty((this.level + ".maxFontSize"), ((fontSize == -1) ? 72 : fontSize));
			this.isBold = style.getBooleanProperty((this.level + ".isBold"), false);
			this.isAllCaps = style.getBooleanProperty((this.level + ".isAllCaps"), false);
			String[] startPatternStrs = style.getListProperty((this.level + ".startPatterns"), null, " ");
			if (startPatternStrs != null) try {
				Pattern[] startPatterns = new Pattern[startPatternStrs.length];
				for (int p = 0; p < startPatternStrs.length; p++)
					startPatterns[p] = Pattern.compile(startPatternStrs[p]);
				this.startPatterns = startPatterns;
			} catch (PatternSyntaxException pse) {}
			String alignment = style.getProperty((this.level + ".alignment"), "");
			this.isLeftAligned = "left".equals(alignment);
			this.isRightAligned = "right".equals(alignment);
		}
		boolean matches(ImRegion line, ImWord[] lineWords, boolean isFlushLeft, boolean isFlushRight) {
			
			//	check alignment first
			if (this.isLeftAligned && !isFlushLeft)
				return false;
			if (this.isRightAligned && !isFlushRight)
				return false;
			if (!this.isLeftAligned && !this.isRightAligned && (isFlushLeft != isFlushRight))
				return false;
			
			//	concatenate words, checking style along the way
			StringBuffer lineWordString = new StringBuffer();
			for (int w = 0; w < lineWords.length; w++) {
				if (this.isBold && !lineWords[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
					return false;
				String wordStr = lineWords[w].getString();
				if (this.isAllCaps && !wordStr.equals(wordStr.toUpperCase()))
					return false;
				String wordFontSizeStr = ((String) lineWords[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
				if (wordFontSizeStr != null) try {
					int wfs = Integer.parseInt(wordFontSizeStr);
					if (wfs < this.minFontSize)
						return false;
					if (this.maxFontSize < wfs)
						return false;
				} catch (NumberFormatException nfe) {}
				lineWordString.append(wordStr);
				if ((w+1) == lineWords.length)
					break;
				if (lineWords[w].getNextWord() != lineWords[w+1])
					lineWordString.append(" ");
				else if ((lineWords[w].getNextRelation() == ImWord.NEXT_RELATION_SEPARATE) && Gamta.insertSpace(lineWords[w].getString(), lineWords[w+1].getString()))
					lineWordString.append(" ");
				else if (lineWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
					lineWordString.append(" ");
				else if ((lineWords[w].getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) && (lineWordString.length() != 0))
					lineWordString.deleteCharAt(lineWordString.length()-1);
			}
			
			//	no patterns to match, this one looks good
			if (this.startPatterns == null)
				return true;
			
			//	check against start patterns
			for (int p = 0; p < this.startPatterns.length; p++) {
				if (this.startPatterns[p].matcher(lineWordString).matches())
					return true;
			}
			
			//	no pattern matched
			return false;
		}
	}
	
	private void markHeadings(ImPage page, int pageImageDpi, HeadingStyleDefined[] headingStyles, ProgressMonitor pm) {
		ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	get block words
		ImWord[][] blockWords = new ImWord[pageBlocks.length][];
		for (int b = 0; b < pageBlocks.length; b++) {
			blockWords[b] = pageBlocks[b].getWords();
			Arrays.sort(blockWords[b], ImUtils.textStreamOrder);
		}
		
		/* TODO compute _fractional_ column width over whole document main text, not per page
		 * - articles, not even books, tend to change their column layout ...
		 * - using fraction abstracts from page image resolution, helps if latter is inconsistent
		 * ==> mitigates effect of single-column article header sitting on top of two-column / multi-column layout
		 * ==> consider leaving first page out of this computation altogether if document long enough
		 * 
		 * - use ZJLS_Hertach2015.pdf to test this (main text lines on first page mistaken for headings because mistaken for short due to very large single-column document head)
		 */
		
		//	get block parent columns
		ImRegion[] pageColumns = page.getRegions(ImRegion.COLUMN_ANNOTATION_TYPE);
		ImRegion[] blockParentColumns = new ImRegion[pageBlocks.length];
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			
			//	try and get marked parent column
			for (int c = 0; c < pageColumns.length; c++)
				if (pageColumns[c].bounds.includes(pageBlocks[b].bounds, false)) {
					blockParentColumns[b] = pageColumns[c];
					break;
				}
			if (blockParentColumns[b] != null)
				continue;
			
			//	synthesize parent column from blocks above and below
			int pcLeft = pageBlocks[b].bounds.left;
			int pcRight = pageBlocks[b].bounds.right;
			int pcTop = pageBlocks[b].bounds.top;
			int pcBottom = pageBlocks[b].bounds.bottom;
			for (int cb = 0; cb < pageBlocks.length; cb++) {
				if (cb == b)
					continue;
				if (pageBlocks[cb].bounds.right <= pcLeft)
					continue;
				if (pcRight <= pageBlocks[cb].bounds.left)
					continue;
				if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
					continue;
				pcLeft = Math.min(pcLeft, pageBlocks[cb].bounds.left);
				pcRight = Math.max(pcRight, pageBlocks[cb].bounds.right);
				pcTop = Math.min(pcTop, pageBlocks[cb].bounds.top);
				pcBottom = Math.max(pcBottom, pageBlocks[cb].bounds.bottom);
			}
			blockParentColumns[b] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(pcLeft, pcRight, pcTop, pcBottom), "blockColumn");
		}
		
		//	compute average main text block width (both non-weighted and weighted by block height)
		int mainTextBlockCount = 0;
		int mainTextBlockWidthSum = 0;
		int mainTextBlockHeightSum = 0;
		int mainTextBlockAreaSum = 0;
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			mainTextBlockCount++;
			mainTextBlockWidthSum += (pageBlocks[b].bounds.right - pageBlocks[b].bounds.left);
			mainTextBlockHeightSum += (pageBlocks[b].bounds.bottom - pageBlocks[b].bounds.top);
			mainTextBlockAreaSum += ((pageBlocks[b].bounds.right - pageBlocks[b].bounds.left) * (pageBlocks[b].bounds.bottom - pageBlocks[b].bounds.top));
		}
		if (mainTextBlockCount == 0) {
			pm.setInfo("No main text blocks on page " + page.pageId);
			return;
		}
		int mainTextBlockWidthC = (mainTextBlockWidthSum / mainTextBlockCount);
		int mainTextBlockWidthA = (mainTextBlockAreaSum / mainTextBlockHeightSum);
		pm.setInfo("Average main text block width on page " + page.pageId + " is " + mainTextBlockWidthC + " (based on " + mainTextBlockCount + " blocks) / " + mainTextBlockWidthA + " (based on " + mainTextBlockHeightSum + " block pixel rows)");
		
		//	assess block lines
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			pm.setInfo("Assessing lines in block " + pageBlocks[b].bounds);
			pm.setInfo(" - parent column is " + blockParentColumns[b].bounds + ("blockColumn".equals(blockParentColumns[b].getType()) ? " (synthesized)" : ""));
			ImRegion[] blockLines = pageBlocks[b].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			Arrays.sort(blockLines, ImUtils.topDownOrder);
			
			//	measure line positions
			ImWord[][] lineWords = new ImWord[blockLines.length][];
			boolean[] lineIsFlushLeft = new boolean[blockLines.length];
			boolean[] lineIsFlushRight = new boolean[blockLines.length];
			boolean[] lineIsShort = new boolean[blockLines.length];
			for (int l = 0; l < blockLines.length; l++) {
				lineWords[l] = getLargestTextStreamWords(blockLines[l].getWords());
				if (lineWords[l].length == 0)
					continue;
				Arrays.sort(lineWords[l], ImUtils.textStreamOrder);
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				pm.setInfo(" - line " + blockLines[l].bounds + ": " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
				
				//	assess line position and length (center alignment against surrounding column to catch short blocks)
				int leftDist = (blockLines[l].bounds.left - blockParentColumns[b].bounds.left);
				lineIsFlushLeft[l] = ((leftDist * 25) < pageImageDpi);
				int rightDist = (blockParentColumns[b].bounds.right - blockLines[l].bounds.right);
				lineIsFlushRight[l] = ((rightDist * 25) < pageImageDpi);
				lineIsShort[l] = (((blockLines[l].bounds.right - blockLines[l].bounds.left) * 3) < (mainTextBlockWidthA * 2));
				if (lineIsShort[l])
					pm.setInfo(" --> short");
				
				//	no use looking for headings more than 3 lines down the block
				if (l == 3)
					break;
			}
			
			//	find headings (first three lines at most)
			HeadingStyleDefined[] lineHeadingStyles = new HeadingStyleDefined[blockLines.length]; 
			Arrays.fill(lineHeadingStyles, null);
			for (int l = 0; l < Math.min(blockLines.length, 4); l++) {
				if (lineWords[l].length == 0)
					continue;
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				
				//	check against styles
				for (int s = 0; s < headingStyles.length; s++)
					if (headingStyles[s].matches(blockLines[l], lineWords[l], lineIsFlushLeft[l], lineIsFlushRight[l])) {
						lineHeadingStyles[l] = headingStyles[s];
						break;
					}
				
				//	no use looking for headings below non-headings
				if (lineHeadingStyles[l] == null)
					break;
			}
			
			//	annotate headings (subsequent lines with same heading reason together, unless line is short)
			ImWord headingStart = null;
			ImWord headingEnd = null;
			HeadingStyleDefined headingStyle = null;
			int headingStartLineIndex = -1;
			for (int l = 0; l < Math.min(blockLines.length, 4); l++) {
				if ((l == 0) && (lineHeadingStyles[l] == null))
					break;
				if (lineWords[l].length == 0)
					continue;
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				if (l == 0) {
					headingStart = lineWords[l][0];
					headingEnd = lineWords[l][lineWords[l].length-1];
					headingStyle = lineHeadingStyles[l];
					headingStartLineIndex = l;
				}
				else if ((lineHeadingStyles[l] == lineHeadingStyles[l-1]) && !lineIsShort[l-1])
					headingEnd = lineWords[l][lineWords[l].length-1];
				else {
					if (headingStyle != null) {
						ImAnnotation heading = page.getDocument().addAnnotation(headingStart, headingEnd, ImAnnotation.HEADING_TYPE);
						heading.setAttribute("reason", ("" + headingStyle.level));
						heading.setAttribute("level", ("" + headingStyle.level));
						if (!lineIsFlushLeft[headingStartLineIndex] && !lineIsFlushRight[headingStartLineIndex])
							heading.setAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
						if (headingStyle.isAllCaps)
							heading.setAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
						if (headingStyle.isBold)
							heading.setAttribute(ImAnnotation.BOLD_ATTRIBUTE);
						if (((headingStyle.minFontSize + headingStyle.maxFontSize) / 2) > 6)
							heading.setAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, ("" + ((headingStyle.minFontSize + headingStyle.maxFontSize) / 2)));
						this.straightenHeadingStreamStructure(heading);
						pm.setInfo(" ==> marked heading level " + headingStyle.level + ": " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true));
					}
					headingStart = lineWords[l][0];
					headingEnd = lineWords[l][lineWords[l].length-1];
					headingStyle = lineHeadingStyles[l];
					headingStartLineIndex = l;
				}
				if (lineHeadingStyles[l] == null) {
					headingStart = null;
					headingEnd = null;
					headingStyle = null;
					headingStartLineIndex = -1;
					break;
				}
			}
			if (headingStyle != null) {
				ImAnnotation heading = page.getDocument().addAnnotation(headingStart, headingEnd, ImAnnotation.HEADING_TYPE);
				heading.setAttribute("reason", ("" + headingStyle.level));
				heading.setAttribute("level", ("" + headingStyle.level));
				if (!lineIsFlushLeft[headingStartLineIndex] && !lineIsFlushRight[headingStartLineIndex])
					heading.setAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
				if (headingStyle.isAllCaps)
					heading.setAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
				if (headingStyle.isBold)
					heading.setAttribute(ImAnnotation.BOLD_ATTRIBUTE);
				if (((headingStyle.minFontSize + headingStyle.maxFontSize) / 2) > 6)
					heading.setAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, ("" + ((headingStyle.minFontSize + headingStyle.maxFontSize) / 2)));
				this.straightenHeadingStreamStructure(heading);
				pm.setInfo(" ==> marked heading level " + headingStyle.level + ": " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true));
			}
		}
	}
	
	private static class HeadingStyleObserved {
		/* TODO also observe numbering schemes
		 * - non-repeating numbers --> higher in hierarchy
		 * - repeating numbers --> lower in hierarchy
		 * - numbers with separating dots (e.g. '1.2 Methods') --> lower in hierarchy
		 */
		int fontSize;
		boolean bold;
		boolean allCaps;
		boolean centered;
		String key;
		ArrayList headings = new ArrayList();
		int avgPosDistSquare = -1;
		int firstPageId = Integer.MAX_VALUE;
		int lastPageId = 0;
		HeadingStyleObserved(ImAnnotation heading) {
			this.fontSize = Integer.parseInt((String) heading.getAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, "-1"));
			this.bold = heading.hasAttribute(ImAnnotation.BOLD_ATTRIBUTE);
			this.allCaps = heading.hasAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
			this.centered = heading.hasAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
			this.key = getStyleKey(heading);
			this.headings.add(heading);
		}
		static String getStyleKey(ImAnnotation heading) {
			StringBuffer styleKey = new StringBuffer("style");
			int fontSize = Integer.parseInt((String) heading.getAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, "-1"));
			if (fontSize != -1)
				styleKey.append("-" + ((fontSize < 10) ? "0" : "") + fontSize);
			if (heading.hasAttribute(ImAnnotation.BOLD_ATTRIBUTE))
				styleKey.append("-" + ImAnnotation.BOLD_ATTRIBUTE);
			if (heading.hasAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE))
				styleKey.append("-" + ImAnnotation.ALL_CAPS_ATTRIBUTE);
			if (heading.hasAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED))
				styleKey.append("-" + ImAnnotation.TEXT_ORIENTATION_CENTERED);
			return styleKey.toString();
		}
	}
	
	private static final Comparator annotationTextStreamOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			ImWord imw1 = ((ImAnnotation) obj1).getFirstWord();
			ImWord imw2 = ((ImAnnotation) obj2).getFirstWord();
			return ImUtils.textStreamOrder.compare(imw1, imw2);
		}
	};
	
	private void markHeadings(ImPage page, int docFontSize, int pageImageDpi, HashSet emphasisWords, ProgressMonitor pm, ArrayList headings) {
		ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	get block words
		ImWord[][] blockWords = new ImWord[pageBlocks.length][];
		for (int b = 0; b < pageBlocks.length; b++) {
			blockWords[b] = pageBlocks[b].getWords();
			Arrays.sort(blockWords[b], ImUtils.textStreamOrder);
		}
		
		/* TODO compute _fractional_ column width over whole document main text, not per page
		 * - articles, not even books, tend to change their column layout ...
		 * - using fraction abstracts from page image resolution, helps if latter is inconsistent
		 * ==> mitigates effect of single-column article header sitting on top of two-column / multi-column layout
		 * ==> consider leaving first page out of this computation altogether if document long enough
		 * 
		 * - use ZJLS_Hertach2015.pdf to test this (main text lines on first page mistaken for headings because mistaken for short due to very large single-column document head)
		 */
		
		//	get block parent columns
		ImRegion[] pageColumns = page.getRegions(ImRegion.COLUMN_ANNOTATION_TYPE);
		ImRegion[] blockParentColumns = new ImRegion[pageBlocks.length];
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			
			//	try and get marked parent column
			for (int c = 0; c < pageColumns.length; c++)
				if (pageColumns[c].bounds.includes(pageBlocks[b].bounds, false)) {
					blockParentColumns[b] = pageColumns[c];
					break;
				}
			if (blockParentColumns[b] != null)
				continue;
			
			//	synthesize parent column from blocks above and below
			int pcLeft = pageBlocks[b].bounds.left;
			int pcRight = pageBlocks[b].bounds.right;
			int pcTop = pageBlocks[b].bounds.top;
			int pcBottom = pageBlocks[b].bounds.bottom;
			for (int cb = 0; cb < pageBlocks.length; cb++) {
				if (cb == b)
					continue;
				if (pageBlocks[cb].bounds.right <= pcLeft)
					continue;
				if (pcRight <= pageBlocks[cb].bounds.left)
					continue;
				if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
					continue;
				pcLeft = Math.min(pcLeft, pageBlocks[cb].bounds.left);
				pcRight = Math.max(pcRight, pageBlocks[cb].bounds.right);
				pcTop = Math.min(pcTop, pageBlocks[cb].bounds.top);
				pcBottom = Math.max(pcBottom, pageBlocks[cb].bounds.bottom);
			}
			blockParentColumns[b] = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(pcLeft, pcRight, pcTop, pcBottom), "blockColumn");
		}
		
		//	compute average main text block width (both non-weighted and weighted by block height)
		int mainTextBlockCount = 0;
		int mainTextBlockWidthSum = 0;
		int mainTextBlockHeightSum = 0;
		int mainTextBlockAreaSum = 0;
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			mainTextBlockCount++;
			mainTextBlockWidthSum += (pageBlocks[b].bounds.right - pageBlocks[b].bounds.left);
			mainTextBlockHeightSum += (pageBlocks[b].bounds.bottom - pageBlocks[b].bounds.top);
			mainTextBlockAreaSum += ((pageBlocks[b].bounds.right - pageBlocks[b].bounds.left) * (pageBlocks[b].bounds.bottom - pageBlocks[b].bounds.top));
		}
		if (mainTextBlockCount == 0) {
			pm.setInfo("No main text blocks on page " + page.pageId);
			return;
		}
		int mainTextBlockWidthC = (mainTextBlockWidthSum / mainTextBlockCount);
		int mainTextBlockWidthA = (mainTextBlockAreaSum / mainTextBlockHeightSum);
		pm.setInfo("Average main text block width on page " + page.pageId + " is " + mainTextBlockWidthC + " (based on " + mainTextBlockCount + " blocks) / " + mainTextBlockWidthA + " (based on " + mainTextBlockHeightSum + " block pixel rows)");
		
		//	assess block lines
		for (int b = 0; b < pageBlocks.length; b++) {
			if ((blockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[b][0].getTextStreamType()))
				continue;
			pm.setInfo("Assessing lines in block " + pageBlocks[b].bounds);
			pm.setInfo(" - parent column is " + blockParentColumns[b].bounds + ("blockColumn".equals(blockParentColumns[b].getType()) ? " (synthesized)" : ""));
			ImRegion[] blockLines = pageBlocks[b].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			Arrays.sort(blockLines, ImUtils.topDownOrder);
			
			//	classify lines
			ImWord[][] lineWords = new ImWord[blockLines.length][];
			boolean[] lineIsShort = new boolean[blockLines.length];
			boolean[] lineIsCentered = new boolean[blockLines.length];
			boolean[] lineIsAllCaps = new boolean[blockLines.length];
			boolean[] lineHasEmphasis = new boolean[blockLines.length];
			boolean[] lineIsEmphasized = new boolean[blockLines.length];
			boolean[] lineHasBoldEmphasis = new boolean[blockLines.length];
			boolean[] lineIsBold = new boolean[blockLines.length];
			int[] lineFontSize = new int[blockLines.length];
			boolean[] lineIsLargeFont = new boolean[blockLines.length];
			for (int l = 0; l < blockLines.length; l++) {
				lineWords[l] = getLargestTextStreamWords(blockLines[l].getWords());
				if (lineWords[l].length == 0)
					continue;
				Arrays.sort(lineWords[l], ImUtils.textStreamOrder);
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				pm.setInfo(" - line " + blockLines[l].bounds + ": " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
				
				//	assess line measurements (center alignment against surrounding column to catch short blocks)
				lineIsShort[l] = (((blockLines[l].bounds.right - blockLines[l].bounds.left) * 3) < (mainTextBlockWidthA * 2));
				if (lineIsShort[l])
					pm.setInfo(" --> short");
				int leftDist = (blockLines[l].bounds.left - blockParentColumns[b].bounds.left);
				int rightDist = (blockParentColumns[b].bounds.right - blockLines[l].bounds.right);
				lineIsCentered[l] = ((((leftDist * 9) < (rightDist * 10)) && ((rightDist * 9) < (leftDist * 10))) || (((leftDist * 25) < pageImageDpi) && ((rightDist * 25) < pageImageDpi)));
				if (lineIsCentered[l])
					pm.setInfo(" --> centered");
				
				//	assess line words
				int lineWordCount = 0;
				int lineAllCapWordCount = 0;
				int lineEmphasisWordCount = 0;
				int lineBoldWordCount = 0;
				int lineFontSizeSum = 0;
				int lineFontSizeWordCount = 0;
				for (int w = 0; w < lineWords[l].length; w++) {
					String lineWordString = lineWords[l][w].getString();
					if (lineWordString == null)
						continue;
					if (Gamta.isWord(lineWordString)) {
						lineWordCount++;
						if (lineWordString.equals(lineWordString.toUpperCase()))
							lineAllCapWordCount++;
						if (emphasisWords.contains(lineWords[l][w]))
							lineEmphasisWordCount++;
						if (lineWords[l][w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
							lineBoldWordCount++;
					}
					String imwFontSize = ((String) lineWords[l][w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
					if (imwFontSize != null) try {
						lineFontSizeSum += Integer.parseInt(imwFontSize);
						lineFontSizeWordCount++;
					} catch (NumberFormatException nfe) {}
				}
				lineIsAllCaps[l] = ((lineWordCount != 0) && (lineAllCapWordCount == lineWordCount));
				if (lineIsAllCaps[l])
					pm.setInfo(" --> all caps");
				lineHasEmphasis[l] = (lineEmphasisWordCount != 0);
				if (lineHasEmphasis[l])
					pm.setInfo(" --> has emphasis");
				lineIsEmphasized[l] = ((lineWordCount != 0) && (lineEmphasisWordCount == lineWordCount));
				if (lineIsEmphasized[l])
					pm.setInfo(" --> emphasized");
				lineHasBoldEmphasis[l] = (lineBoldWordCount != 0);
				if (lineHasBoldEmphasis[l])
					pm.setInfo(" --> has bold emphasis");
				lineIsBold[l] = ((lineWordCount != 0) && (lineBoldWordCount == lineWordCount));
				if (lineIsBold[l])
					pm.setInfo(" --> bold");
				lineFontSize[l] = ((lineFontSizeWordCount == 0) ? -1 : ((lineFontSizeSum + (lineFontSizeWordCount / 2)) / lineFontSizeWordCount));
				lineIsLargeFont[l] = ((docFontSize > 6) && (lineFontSize[l] > docFontSize));
				if (lineIsLargeFont[l])
					pm.setInfo(" --> large font " + lineFontSize[l]);
				
				//	no use looking for headings more than 3 lines down the block
				if (l == 3)
					break;
			}
			
			//	find headings (first three lines at most)
			int[] lineIsHeadingBecause = new int[blockLines.length]; 
			Arrays.fill(lineIsHeadingBecause, -1);
			for (int l = 0; l < Math.min(blockLines.length, 4); l++) {
				if (lineWords[l].length == 0)
					continue;
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				
				//	no heading without words
				boolean noWords = true;
				for (int w = 0; w < lineWords[l].length; w++)
					if ((lineWords[l][w].getString().length() > 2) && Gamta.isWord(lineWords[l][w].getString())) {
						noWords = false;
						break;
					}
				if (noWords)
					break;
				
				//	no heading starts with a citation
				ImAnnotation[] lineStartAnnots = page.getDocument().getAnnotations(lineWords[l][0], null);
				boolean gotCitationStart = false;
				for (int a = 0; a < lineStartAnnots.length; a++)
					if (lineStartAnnots[a].getType().endsWith("Citation")) {
						gotCitationStart = true;
						break;
					}
				if (gotCitationStart)
					break;
				
				//	TODO refine this
				
				//	below average font size + not all caps + not bold ==> not a heading
				if ((lineFontSize[l] < docFontSize) && !lineIsBold[l] && !lineIsAllCaps[l])
					break;
				
				//	short + large-font ==> heading
				if (lineIsBold[l] && lineIsLargeFont[l]) {
					pm.setInfo(" ==> heading (0): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 0;
					continue;
				}
				
				//	short + large-font ==> heading
				if (lineIsShort[l] && lineIsLargeFont[l]) {
					pm.setInfo(" ==> heading (1): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 1;
					continue;
				}
				
				//	short + bold + single-line-block ==> heading
				if (lineIsShort[l] && lineIsBold[l] && (blockLines.length == 1)) {
					pm.setInfo(" ==> heading (2): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 2;
					continue;
				}
				
				//	short + all-caps + single-line-block ==> heading
				if (lineIsShort[l] && lineIsAllCaps[l] && (blockLines.length == 1)) {
					pm.setInfo(" ==> heading (3): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 3;
					continue;
				}
				
				//	short + centered ==> heading
				if (lineIsShort[l] && lineIsCentered[l]) {
					pm.setInfo(" ==> heading (4): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 4;
					continue;
				}
				
				//	centered + all-caps ==> heading
				if (lineIsCentered[l] && lineIsAllCaps[l]) {
					pm.setInfo(" ==> heading (5): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 5;
					continue;
				}
				
				//	short + bold + block-top-line ==> heading
//				if (lineIsShort[l] && lineIsBold[l] && (l == 0)) {
				if (lineIsShort[l] && lineIsBold[l]) {
					pm.setInfo(" ==> heading (6): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 6;
					continue;
				}
				
				//	centered + emphasis + block-top-line + short-centered-line-below ==> heading
				if (lineIsCentered[l] && lineHasEmphasis[l] && (l == 0) && (blockLines.length > 1) && lineIsShort[l+1] && lineIsCentered[l+1]) {
					pm.setInfo(" ==> heading (7): " + ImUtils.getString(lineWords[l][0], lineWords[l][lineWords[l].length-1], true));
					lineIsHeadingBecause[l] = 4; // conflate with 4, as this is the two-line case of 4
					continue;
				}
				
				//	no use looking for headings below non-headings
				break;
			}
			
			
			//	rule out lines if lines further down the block exhibit same properties (not for large font, though ... document titles may have 4 or more lines)
			if ((blockLines.length >= 4) && !lineIsLargeFont[0] && (lineIsHeadingBecause[3] != -1)) {
				pm.setInfo(" ==> ignoring potential headings for style continuing through block");
				for (int l = 2; l >= 0; l--) {
					if (lineIsHeadingBecause[l] == lineIsHeadingBecause[3])
						lineIsHeadingBecause[l] = -1;
					else break;
				}
				if (lineIsHeadingBecause[0] == -1)
					continue;
			}
			
			//	annotate headings (subsequent lines with same heading reason together, unless line is short)
			ImWord headingStart = null;
			ImWord headingEnd = null;
			int headingReason = -1;
			int headingStartLineIndex = -1;
			for (int l = 0; l < Math.min(blockLines.length, 4); l++) {
				if ((l == 0) && (lineIsHeadingBecause[l] == -1))
					break;
				if (lineWords[l].length == 0)
					continue;
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(lineWords[l][0].getTextStreamType()))
					continue;
				if (l == 0) {
					headingStart = lineWords[l][0];
					headingEnd = lineWords[l][lineWords[l].length-1];
					headingReason = lineIsHeadingBecause[l];
					headingStartLineIndex = l;
				}
				else if ((lineIsHeadingBecause[l] == lineIsHeadingBecause[l-1]) && !lineIsShort[l-1])
					headingEnd = lineWords[l][lineWords[l].length-1];
				else {
					if (headingReason != -1) {
						ImAnnotation heading = page.getDocument().addAnnotation(headingStart, headingEnd, ImAnnotation.HEADING_TYPE);
						heading.setAttribute("reason", ("" + headingReason));
						if (lineIsCentered[headingStartLineIndex])
							heading.setAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
						if (lineIsAllCaps[headingStartLineIndex])
							heading.setAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
						if (lineIsBold[headingStartLineIndex])
							heading.setAttribute(ImAnnotation.BOLD_ATTRIBUTE);
						if (lineFontSize[headingStartLineIndex] > 6)
							heading.setAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, ("" + lineFontSize[headingStartLineIndex]));
						headings.add(heading);
						this.straightenHeadingStreamStructure(heading);
						pm.setInfo(" ==> marked heading: " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true));
					}
					headingStart = lineWords[l][0];
					headingEnd = lineWords[l][lineWords[l].length-1];
					headingReason = lineIsHeadingBecause[l];
					headingStartLineIndex = l;
				}
				if (lineIsHeadingBecause[l] == -1) {
					headingStart = null;
					headingEnd = null;
					headingReason = -1;
					headingStartLineIndex = -1;
					break;
				}
			}
			if (headingReason != -1) {
				ImAnnotation heading = page.getDocument().addAnnotation(headingStart, headingEnd, ImAnnotation.HEADING_TYPE);
				heading.setAttribute("reason", ("" + headingReason));
				if (lineIsCentered[headingStartLineIndex])
					heading.setAttribute(ImAnnotation.TEXT_ORIENTATION_CENTERED);
				if (lineIsAllCaps[headingStartLineIndex])
					heading.setAttribute(ImAnnotation.ALL_CAPS_ATTRIBUTE);
				if (lineIsBold[headingStartLineIndex])
					heading.setAttribute(ImAnnotation.BOLD_ATTRIBUTE);
				if (lineFontSize[headingStartLineIndex] > 6)
					heading.setAttribute(ImAnnotation.FONT_SIZE_ATTRIBUTE, ("" + lineFontSize[headingStartLineIndex]));
				headings.add(heading);
				this.straightenHeadingStreamStructure(heading);
				pm.setInfo(" ==> marked heading: " + ImUtils.getString(heading.getFirstWord(), heading.getLastWord(), true));
			}
		}
	}
	
	private void straightenHeadingStreamStructure(ImAnnotation heading) {
		if (heading.getFirstWord().getPreviousWord() != null)
			heading.getFirstWord().getPreviousWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		for (ImWord imw = heading.getFirstWord(); (imw != heading.getLastWord()) && (imw != null); imw = imw.getNextWord()) {
			if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				imw.setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
		}
		heading.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
	}
	
	private String getCaptionStartForCheck(ImAnnotation caption) {
		for (ImWord imw = caption.getFirstWord(); imw != null; imw = imw.getNextWord()) {
			String imwString = imw.getString();
			if ((imwString != null) && imwString.matches("([0-9]+(\\s*[a-z])?)|[IiVvXxLl]+|[a-zA-Z]"))
				return ImUtils.getString(caption.getFirstWord(), imw, true);
		}
		return ImUtils.getString(caption.getFirstWord(), caption.getLastWord(), true);
	}
	
	private static final boolean DEBUG_IS_ARTIFACT = true;
	private boolean isArtifact(ImRegion block, ImWord[] blockWords, int dpi) {
		if (DEBUG_IS_ARTIFACT) System.out.println("Assessing block " + block.bounds + " on page " + block.pageId + " for artifact");
		if (blockWords.length == 0) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> no words at all");
			return false;
		}
		
		//	check if cut-out still possible
		Arrays.sort(blockWords, ImUtils.textStreamOrder);
		if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(blockWords[0].getTextStreamType())) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> already cut from main text");
			return false;
		}
		
		//	check dimensions
		if ((block.bounds.right - block.bounds.left) < dpi) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> too narrow");
			return false;
		}
		if ((block.bounds.bottom - block.bounds.top) < dpi) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> too low");
			return false;
		}
		
		//	get block area
		int blockArea = ((block.bounds.right - block.bounds.left) * (block.bounds.bottom - block.bounds.top));
		
		//	assess word distribution (we really have to do it pixel by pixel in order to not miss word overlap)
		int[][] wordDistribution = new int[block.bounds.right - block.bounds.left][block.bounds.bottom - block.bounds.top];
		for (int c = 0; c < wordDistribution.length; c++)
			Arrays.fill(wordDistribution[c], 0);
		for (int w = 0; w < blockWords.length; w++)
			for (int c = blockWords[w].bounds.left; c < blockWords[w].bounds.right; c++) {
				for (int r = blockWords[w].bounds.top; r < blockWords[w].bounds.bottom; r++)
					wordDistribution[c - block.bounds.left][r - block.bounds.top]++;
			}
		int maxWordOverlap = 0;
		for (int c = 0; c < wordDistribution.length; c++) {
			for (int r = 0; r < wordDistribution[c].length; r++)
				maxWordOverlap = Math.max(maxWordOverlap, wordDistribution[c][r]);
		}
		if (DEBUG_IS_ARTIFACT) System.out.println(" - maximum word overlap is " + maxWordOverlap);
		int[] wordOverlapDistribution = new int[maxWordOverlap + 1];
		for (int c = 0; c < wordDistribution.length; c++) {
			for (int r = 0; r < wordDistribution[c].length; r++)
				wordOverlapDistribution[wordDistribution[c][r]]++;
		}
		if (DEBUG_IS_ARTIFACT) System.out.println(" - word overlap distribution:");
		int blockWordArea = 0;
		for (int c = 0; c < wordOverlapDistribution.length; c++) {
			if (DEBUG_IS_ARTIFACT) System.out.println("   - " + c + ": " + wordOverlapDistribution[c] + ", " + ((wordOverlapDistribution[c] * 100) / blockArea) + "%");
			if (c != 0)
				blockWordArea += wordOverlapDistribution[c];
		}
		if (DEBUG_IS_ARTIFACT) System.out.println(" - block word ratio is " + ((blockWordArea * 100) / blockArea) + "% (" + blockWordArea + "/" + blockArea + ")");
		
		//	sparse block (less than 10% covered with words)
		if ((blockWordArea * 10) < blockArea) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> artifact for very sparse words");
			return true;
		}
		
		//	words chaotic (at least 10% of word area with overlap)
		if ((1 < maxWordOverlap) && (wordOverlapDistribution[1] < (wordOverlapDistribution[2] * 10))) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> artifact for word overlap");
			return true;
		}
		
		//	words very chaotic (maximum overlap of three or more)
		if (2 < maxWordOverlap) {
			if (DEBUG_IS_ARTIFACT) System.out.println(" ==> artifact for multi-word overlap");
			return true;
		}
		
		//	this one looks at least marginally normal
		if (DEBUG_IS_ARTIFACT) System.out.println(" ==> not an artifact");
		return false;
	}
	
	private void detectTables(ImPage page, int pageImageDpi, int minColMargin, int minRowMargin, ProgressMonitor pm) {
		ImRegion[] pageBlocks = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
		Arrays.sort(pageBlocks, ImUtils.topDownOrder);
		
		/* sort page blocks in text stream order based on first word:
		 * - in a single-column layout, this is as good as top-down
		 * - in multi-column layout, this maintains distance between in-column tables
		 * 
		 * BUT THEN, top-down should do in multi-column layouts as well ...
		 * ... safe for the unlikely or constructed oddjob ...
		 * ... e.g. two in-column tables with aligned lines */
		
		//	split blocks with varying line distances (might be tables with multi-line rows) at larger line gaps, and let merging do its magic
		ArrayList pageBlockList = new ArrayList();
		HashMap pageBlocksBySubBlocks = new HashMap();
		for (int b = 0; b < pageBlocks.length; b++) {
			if (DEBUG_IS_TABLE) System.out.println("Testing irregular line gap split in block " + pageBlocks[b].bounds + " on page " + page.pageId + " for table detection");
			
			//	get block lines
			ImRegion[] blockLines = pageBlocks[b].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			if (blockLines.length < 5) {
				pageBlockList.add(pageBlocks[b]);
				if (DEBUG_IS_TABLE) System.out.println(" ==> too few lines to assess gaps");
				continue;
			}
			Arrays.sort(blockLines, ImUtils.topDownOrder);
			if (DEBUG_IS_TABLE) System.out.println(" --> got " + blockLines.length + " lines");
			
			//	compute line gaps
			int minLineGap = (pageBlocks[b].bounds.bottom - pageBlocks[b].bounds.top);
			int maxLineGap = 0;
			int lineGapCount = 0;
			int lineGapSum = 0;
			for (int l = 1; l < blockLines.length; l++) {
				int lineGap = (blockLines[l].bounds.top - blockLines[l-1].bounds.bottom);
				if (lineGap < 0)
					continue;
				minLineGap = Math.min(minLineGap, lineGap);
				maxLineGap = Math.max(maxLineGap, lineGap);
				lineGapSum += lineGap;
				lineGapCount++;
			}
			if (lineGapCount < 4) {
				pageBlockList.add(pageBlocks[b]);
				if (DEBUG_IS_TABLE) System.out.println(" ==> too few non-overlapping lines to assess gaps");
				continue;
			}
			int avgLineGap = ((lineGapSum + (lineGapCount / 2)) / lineGapCount);
			if (DEBUG_IS_TABLE) System.out.println(" --> measured " + lineGapCount + " line gaps, min is " + minLineGap + ", max is " + maxLineGap + ", avg is " + avgLineGap);
			
			//	line gaps too regular to use
			if ((minLineGap * 4) > (maxLineGap * 3)) {
				pageBlockList.add(pageBlocks[b]);
				if (DEBUG_IS_TABLE) System.out.println(" ==> line gaps too regular");
				continue;
			}
			
			//	count above-average line gaps
			int aboveAvgLineGapCount = 0;
			for (int l = 1; l < blockLines.length; l++) {
				int lineGap = (blockLines[l].bounds.top - blockLines[l-1].bounds.bottom);
				if (lineGap > avgLineGap)
					aboveAvgLineGapCount++;
			}
			
			//	too few above-average line gaps
			if ((aboveAvgLineGapCount * 5) < blockLines.length) {
				pageBlockList.add(pageBlocks[b]);
				if (DEBUG_IS_TABLE) System.out.println(" ==> too few above-average line gaps");
				continue;
			}
			
			//	create sub blocks at above-average line gaps
			int subBlockStartLine = 0;
			for (int l = 1; l < blockLines.length; l++) {
				int lineGap = (blockLines[l].bounds.top - blockLines[l-1].bounds.bottom);
				if (lineGap < avgLineGap)
					continue;
				ImRegion subBlock = new ImRegion(pageBlocks[b].getDocument(), pageBlocks[b].pageId, new BoundingBox(pageBlocks[b].bounds.left, pageBlocks[b].bounds.right, blockLines[subBlockStartLine].bounds.top, blockLines[l-1].bounds.bottom), ImRegion.BLOCK_ANNOTATION_TYPE);
				pageBlockList.add(subBlock);
				pageBlocksBySubBlocks.put(subBlock, pageBlocks[b]);
				if (DEBUG_IS_TABLE) System.out.println(" --> got sub block at " + subBlock.bounds + " with " + (l - subBlockStartLine) + " lines");
				subBlockStartLine = l;
			}
			if (subBlockStartLine != 0) {
				ImRegion subBlock = new ImRegion(pageBlocks[b].getDocument(), pageBlocks[b].pageId, new BoundingBox(pageBlocks[b].bounds.left, pageBlocks[b].bounds.right, blockLines[subBlockStartLine].bounds.top, pageBlocks[b].bounds.bottom), ImRegion.BLOCK_ANNOTATION_TYPE);
				pageBlockList.add(subBlock);
				pageBlocksBySubBlocks.put(subBlock, pageBlocks[b]);
				if (DEBUG_IS_TABLE) System.out.println(" --> got sub block at " + subBlock.bounds + " with " + (blockLines.length - subBlockStartLine) + " lines");
			}
			
			//	increase minimum row margin to average line gap TODO_ne assess if this doesn't wreck havoc in some situations ==> seems OK with line gap width distribution catch
			minRowMargin = Math.max(minRowMargin, avgLineGap);
		}
		
		//	update page blocks if sub blocks added (do NOT re-sort, so sub blocks of same original block stay together)
		if (pageBlocks.length < pageBlockList.size())
			pageBlocks = ((ImRegion[]) pageBlockList.toArray(new ImRegion[pageBlockList.size()]));
		
		//	collect words and columns for each block, and measure width
		boolean[] isPageBlockNarrow = new boolean[pageBlocks.length];
		ImWord[][] pageBlockWords = new ImWord[pageBlocks.length][];
		ImRegion[][] pageBlockCols = new ImRegion[pageBlocks.length][];
		ImRegion[][] pageBlockRows = new ImRegion[pageBlocks.length][];
		for (int b = 0; b < pageBlocks.length; b++) {
			isPageBlockNarrow[b] = (((pageBlocks[b].bounds.right - pageBlocks[b].bounds.left) * 5) < (page.bounds.right - page.bounds.left));
			pageBlockWords[b] = page.getWordsInside(pageBlocks[b].bounds);
			System.out.println("Testing block " + pageBlocks[b].bounds + " on page " + page.pageId + " for table or table part");
			if (pageBlockWords[b].length == 0) {
				System.out.println(" ==> no words");
				continue;
			}
			Arrays.sort(pageBlockWords[b], ImUtils.textStreamOrder);
			if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[b][0].getTextStreamType()))
				continue;
			pm.setInfo("Testing block " + pageBlocks[b].bounds + " on page " + page.pageId + " for table or table part");
			pageBlockCols[b] = this.getTableColumns(pageBlocks[b], minColMargin);
			if (pageBlockCols[b] != null)
				pm.setInfo(" ==> columns OK");
			pageBlockRows[b] = this.getTableRows(pageBlocks[b], minRowMargin);
		}
		
		//	assess possible block mergers
		for (int b = 0; b < pageBlocks.length; b++) {
			if (pageBlocks[b] == null)
				continue;
			if ((pageBlockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[b][0].getTextStreamType()))
				continue;
			
			//	collect merge result
			ImRegion mergedBlock = null;
			ImRegion[] mergedBlockCols = null;
			int mergedBlockRowCount = 3;
			int mergedBlockCellCount = 0;
			
			//	try merging downward from narrow block (might be table column cut off others)
			if (isPageBlockNarrow[b]) {
				
				//	find block with columns
				int mergeBlockIndex = -1;
				for (int l = (b+1); l < pageBlocks.length; l++) {
					if (pageBlocks[l] == null)
						continue;
					if ((pageBlockWords[l].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[l][0].getTextStreamType()))
						continue;
					if (isPageBlockNarrow[l])
						continue;
					if (pageBlocks[l].bounds.top < pageBlocks[b].bounds.bottom)
						continue;
					if (pageBlockCols[l] != null)
						mergeBlockIndex = l;
					break;
				}
				if (mergeBlockIndex == -1)
					continue;
				
				//	attempt merger
				pm.setInfo("Attempting merger of " + pageBlocks[b].bounds + " and " + pageBlocks[mergeBlockIndex].bounds + " (" + pageBlockCols[mergeBlockIndex].length + " columns)");
				BoundingBox mergeTestBlockBounds = new BoundingBox(Math.min(pageBlocks[b].bounds.left, pageBlocks[mergeBlockIndex].bounds.left), Math.max(pageBlocks[b].bounds.right, pageBlocks[mergeBlockIndex].bounds.right), pageBlocks[b].bounds.top, pageBlocks[mergeBlockIndex].bounds.bottom);
				int mtbbLeft = mergeTestBlockBounds.left;
				int mtbbRight = mergeTestBlockBounds.right;
				for (int tb = 0; tb < pageBlocks.length; tb++) {
					if (pageBlocks[tb] == null)
						continue;
					if (pageBlocks[tb].bounds.liesIn(mergeTestBlockBounds, false))
						continue;
					if (!pageBlocks[tb].bounds.liesIn(mergeTestBlockBounds, true))
						continue;
					mtbbLeft = Math.min(mtbbLeft, pageBlocks[tb].bounds.left);
					mtbbRight = Math.max(mtbbRight, pageBlocks[tb].bounds.right);
				}
				if ((mtbbLeft < mergeTestBlockBounds.left) || (mergeTestBlockBounds.right < mtbbRight))
					mergeTestBlockBounds = new BoundingBox(mtbbLeft, mtbbRight, mergeTestBlockBounds.top, mergeTestBlockBounds.bottom);
				ImRegion mergeTestBlock = new ImRegion(page.getDocument(), page.pageId, mergeTestBlockBounds, ImRegion.BLOCK_ANNOTATION_TYPE);
				pm.setInfo(" - merged block is " + mergeTestBlock.bounds);
				ImRegion[] mergeTestBlockCols = this.getTableColumns(mergeTestBlock, minColMargin);
				if (mergeTestBlockCols == null) {
					pm.setInfo(" ==> foo few columns");
					continue;
				}
				
				//	allow tolerance of one column lost if 9 or more columns in main block ...
				if (mergeTestBlockCols.length < pageBlockCols[mergeBlockIndex].length) {
					if ((pageBlockCols[mergeBlockIndex].length > 8) && ((mergeTestBlockCols.length + 1) == pageBlockCols[mergeBlockIndex].length))
						pm.setInfo(" - one column loss tolerated");
					else {
						pm.setInfo(" ==> too few columns (" + mergeTestBlockCols.length + ")");
						break;
					}
				}
				pm.setInfo(" - columns OK");
				ImRegion[] mergeTestBlockRows = this.getTableRows(mergeTestBlock, minRowMargin);
				if (mergeTestBlockRows == null) {
					pm.setInfo(" ==> too few rows");
					continue;
				}
				pm.setInfo(" - rows OK");
				
				//	... but only if more total cells in merge result than in main block
				if ((pageBlockRows[mergeBlockIndex] != null) && ((mergeTestBlockCols.length * mergeTestBlockRows.length) < (pageBlockCols[mergeBlockIndex].length * pageBlockCols[mergeBlockIndex].length))) {
					pm.setInfo(" ==> too few cells (1, " + mergeTestBlockCols.length + "x" + mergeTestBlockRows.length + " vs. " + pageBlockCols[b].length + "x" + pageBlockRows[b].length + ")");
					continue;
				}
				else if ((mergeTestBlockCols.length * mergeTestBlockRows.length) < mergedBlockCellCount) {
					pm.setInfo(" ==> too few cells (2, " + mergeTestBlockCols.length + "x" +  mergeTestBlockRows.length + " vs. " + mergedBlockCellCount + ")");
					continue;
				}
				ImRegion[][] mergeTestBlockCells = this.getTableCells(page, mergeTestBlock, mergeTestBlockRows, mergeTestBlockCols, true);
				if (mergeTestBlockCells == null) {
					pm.setInfo(" ==> cells incomplete");
					continue;
				}
				pm.setInfo(" - cells OK");
				mergedBlock = mergeTestBlock;
				mergedBlockCols = mergeTestBlockCols;
				mergedBlockCellCount = (mergeTestBlockCols.length * mergeTestBlockRows.length);
			}
			
			//	try merging downward from block with viable column gaps
			else if (pageBlockCols[b] != null) {
				
				//	try merging downward
				for (int l = (b+1); l < pageBlocks.length; l++) {
					if (pageBlocks[l] == null)
						continue;
					if ((pageBlockWords[l].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[l][0].getTextStreamType()))
						continue;
					if (pageBlocks[l].bounds.top < pageBlocks[b].bounds.bottom)
						continue;
					
					//	attempt merger
					pm.setInfo("Attempting merger of " + pageBlocks[b].bounds + " (" + pageBlockCols[b].length + " columns) and " + pageBlocks[l].bounds);
					BoundingBox mergeTestBlockBounds = new BoundingBox(Math.min(pageBlocks[b].bounds.left, pageBlocks[l].bounds.left), Math.max(pageBlocks[b].bounds.right, pageBlocks[l].bounds.right), pageBlocks[b].bounds.top, pageBlocks[l].bounds.bottom);
					int mtbbLeft = mergeTestBlockBounds.left;
					int mtbbRight = mergeTestBlockBounds.right;
					for (int tb = 0; tb < pageBlocks.length; tb++) {
						if (pageBlocks[tb] == null)
							continue;
						if (pageBlocks[tb].bounds.liesIn(mergeTestBlockBounds, false))
							continue;
						if (!pageBlocks[tb].bounds.liesIn(mergeTestBlockBounds, true))
							continue;
						mtbbLeft = Math.min(mtbbLeft, pageBlocks[tb].bounds.left);
						mtbbRight = Math.max(mtbbRight, pageBlocks[tb].bounds.right);
					}
					if ((mtbbLeft < mergeTestBlockBounds.left) || (mergeTestBlockBounds.right < mtbbRight))
						mergeTestBlockBounds = new BoundingBox(mtbbLeft, mtbbRight, mergeTestBlockBounds.top, mergeTestBlockBounds.bottom);
					ImRegion mergeTestBlock = new ImRegion(page.getDocument(), page.pageId, mergeTestBlockBounds, ImRegion.BLOCK_ANNOTATION_TYPE);
					pm.setInfo(" - merged block is " + mergeTestBlock.bounds);
					ImRegion[] mergeTestBlockCols = this.getTableColumns(mergeTestBlock, minColMargin);
					if (mergeTestBlockCols == null) {
						pm.setInfo(" ==> too few columns");
						break;
					}
					
					//	allow tolerance of one column lost if 9 or more columns in main block ...
					if (mergeTestBlockCols.length < pageBlockCols[b].length) {
						if ((pageBlockCols[b].length > 8) && ((mergeTestBlockCols.length + 1) == pageBlockCols[b].length))
							pm.setInfo(" - one column loss tolerated");
						else {
							pm.setInfo(" ==> too few columns (" + mergeTestBlockCols.length + ")");
							break;
						}
					}
					else pm.setInfo(" - columns OK");
					ImRegion[] mergeTestBlockRows = this.getTableRows(mergeTestBlock, minRowMargin, mergedBlockRowCount);
					if (mergeTestBlockRows == null) {
						pm.setInfo(" ==> too few rows");
						continue;
					}
					pm.setInfo(" - rows OK");
					
					//	... but only if more total cells in merge result than in main block
					if ((pageBlockRows[b] != null) && ((mergeTestBlockCols.length * mergeTestBlockRows.length) < (pageBlockCols[b].length * pageBlockRows[b].length))) {
						pm.setInfo(" ==> too few cells (1, " + mergeTestBlockCols.length + "x" + mergeTestBlockRows.length + " vs. " + pageBlockCols[b].length + "x" + pageBlockRows[b].length + ")");
						continue;
					}
					else if ((mergeTestBlockCols.length * mergeTestBlockRows.length) < mergedBlockCellCount) {
						pm.setInfo(" ==> too few cells (2, " + mergeTestBlockCols.length + "x" +  mergeTestBlockRows.length + " vs. " + mergedBlockCellCount + ")");
						continue;
					}
					ImRegion[][] mergeTestBlockCells = this.getTableCells(page, mergeTestBlock, mergeTestBlockRows, mergeTestBlockCols, true);
					if (mergeTestBlockCells == null) {
						pm.setInfo(" ==> cells incomplete");
						continue;
					}
					pm.setInfo(" - cells OK");
					mergedBlock = mergeTestBlock;
					mergedBlockCols = mergeTestBlockCols;
					mergedBlockRowCount = mergeTestBlockRows.length;
					mergedBlockCellCount = (mergeTestBlockCols.length * mergeTestBlockRows.length);
				}
			}
			
			//	any success?
			if (mergedBlock == null)
				continue;
			
			//	store merged block, and clean up all blocks inside
			for (int c = 0; c < pageBlocks.length; c++) {
				if (pageBlocks[c] == null)
					continue;
				if (!mergedBlock.bounds.includes(pageBlocks[c].bounds, true))
					continue;
				page.removeRegion(pageBlocks[c]);
				pageBlocksBySubBlocks.remove(pageBlocks[c]);
				pageBlocks[c] = null;
				pageBlockCols[c] = null;
				pageBlockWords[c] = null;
				isPageBlockNarrow[c] = false;
			}
			
			//	add merged block
			page.addRegion(mergedBlock);
			pageBlocks[b] = mergedBlock;
			pageBlockCols[b] = mergedBlockCols;
			pageBlockWords[b] = mergedBlock.getWords();
			Arrays.sort(pageBlockWords[b], ImUtils.textStreamOrder);
			isPageBlockNarrow[b] = false;
			pm.setInfo(" - got merged block " + mergedBlock.bounds);
		}
		
		//	merge adjacent sub blocks of same parent block that were not merged to form a table
		ArrayList subBlockList = new ArrayList();
		ImRegion subBlockParent = null;
		int subBlockStartIndex = -1;
		for (int b = 0; b <= pageBlocks.length; b++) {
			if ((b < pageBlocks.length) && (pageBlocks[b] == null))
				continue;
			
			//	get parent block to assess what to do
			ImRegion pageBlockParent = null;
			
			//	we're in the last run, just end whatever we have
			if (b == pageBlocks.length) {}
			
			//	this one's original, or a merger result, just end whatever we have
			else if (pageBlocks[b].getPage() != null) {}
			
			//	we have a sub block
			else {
				pageBlockParent = ((ImRegion) pageBlocksBySubBlocks.remove(pageBlocks[b]));
				if (pageBlockParent == null)
					continue; // highly unlikely, but let's have this safety net
				
				//	same parent as previous sub block(s), add to list
				if (subBlockParent == pageBlockParent) {
					subBlockList.add(pageBlocks[b]);
					continue;
				}
			}
			
			//	merge collected sub blocks and clean up (if we get here, we're not continuing current sub block)
			if (subBlockList.size() != 0) {
				BoundingBox subBlockBounds = ImLayoutObject.getAggregateBox((ImRegion[]) subBlockList.toArray(new ImRegion[subBlockList.size()]));
				BoundingBox subBlockWordBounds = ImLayoutObject.getAggregateBox(page.getWordsInside(subBlockBounds));
				pageBlocks[subBlockStartIndex] = new ImRegion(page, ((subBlockWordBounds == null) ? subBlockBounds : subBlockWordBounds), ImRegion.BLOCK_ANNOTATION_TYPE);
				
				pageBlockCols[subBlockStartIndex] = null;
				pageBlockWords[subBlockStartIndex] = page.getWordsInside(pageBlocks[subBlockStartIndex].bounds);
				Arrays.sort(pageBlockWords[subBlockStartIndex], ImUtils.textStreamOrder);
				
				for (int c = (subBlockStartIndex + 1); c < b; c++) {
					pageBlocks[c] = null;
					pageBlockCols[c] = null;
					pageBlockWords[c] = null;
				}
				page.removeRegion(subBlockParent);
				
				subBlockList.clear();
				subBlockParent = null;
				subBlockStartIndex = -1;
			}
			
			//	start new sub block (if we get here and have a parent block, we are to start a new sub block)
			if (pageBlockParent != null) {
				subBlockList.add(pageBlocks[b]);
				subBlockParent = pageBlockParent;
				subBlockStartIndex = b;
			}
		}
		
		//	mark single-block tables
		for (int b = 0; b < pageBlocks.length; b++) {
			if (pageBlocks[b] == null)
				continue;
			if ((pageBlockWords[b].length == 0) || !ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(pageBlockWords[b][0].getTextStreamType()))
				continue;
			pm.setInfo("Testing block " + pageBlocks[b].bounds + " on page " + page.pageId + " for table");
			if (this.markIfIsTable(page, pageBlocks[b], pageBlockCols[b], pageBlockWords[b], pageImageDpi, minColMargin, minRowMargin)) {
				pm.setInfo(" ==> table detected");
				page.removeRegion(pageBlocks[b]);
			}
			else if (this.markIfContainsTable(page, pageBlocks[b], pageBlockWords[b], pageImageDpi, minColMargin, minRowMargin)) {
				pm.setInfo(" ==> table extracted");
				page.removeRegion(pageBlocks[b]);
			}
			else pm.setInfo(" ==> not a table");
		}
	}
	
	private static final boolean DEBUG_IS_TABLE = true;
	private boolean markIfIsTable(ImPage page, ImRegion block, ImRegion[] tableCols, ImWord[] blockWords, int dpi, int minColMargin, int minRowMargin) {
		if (DEBUG_IS_TABLE) System.out.println("Testing for table: " + block.bounds.toString() + " on page " + block.pageId + " with " + blockWords.length + " words");
		
		//	try to get table columns
		if (tableCols == null)
			tableCols = this.getTableColumns(block, minColMargin);
		if (tableCols == null)
			return false;
		if (DEBUG_IS_TABLE) System.out.println(" --> got " + tableCols.length + " columns");
		
		//	try to get table rows
		ImRegion[] tableRows = this.getTableRows(block, minRowMargin);
		if (tableRows == null)
			return false;
		if (DEBUG_IS_TABLE) System.out.println(" --> got " + tableRows.length + " rows");
		
		//	get table cells
		ImRegion[][] tableCells = this.getTableCells(page, block, tableRows, tableCols, false);
		if (tableCells == null)
			return false;
		if (DEBUG_IS_TABLE) System.out.println(" --> got cells");
		
		//	large number of rows, but incomplete row labels or sparse table body, try increasing row margin
		if ((tableRows.length >= 9) && (!this.checkRowLabels(page, tableCells) || !this.checkTableBody(page, tableCells))) {
			if (DEBUG_IS_TABLE) System.out.println(" --> re-trying with increased row margin");
			
			//	compute average margin of table rows (we only have a chance if cell internal line margin is less than table row margin)
			int minCellRowMargin = (block.bounds.bottom - block.bounds.top);
			int maxCellRowMargin = 0;
			int cellRowMarginSum = 0;
			for (int r = 1; r < tableCells.length; r++) {
				int cellRowMargin = ((tableCells[r][0].bounds.top - tableCells[r-1][0].bounds.bottom));
				minCellRowMargin = Math.min(minCellRowMargin, cellRowMargin);
				maxCellRowMargin = Math.max(maxCellRowMargin, cellRowMargin);
				cellRowMarginSum += cellRowMargin;
			}
			int avgCellRowMargin = ((cellRowMarginSum + (tableCells.length / 2)) / (tableCells.length - 1));
			if (DEBUG_IS_TABLE) System.out.println(" --> average measured row margin is " + avgCellRowMargin);
			
			//	no use trying again if minimum row margin didn't increase
			if (avgCellRowMargin <= minRowMargin) {
				if (DEBUG_IS_TABLE) System.out.println(" ==> no use re-trying");
				return false;
			}
			
			//	no use trying again if difference too small
			if (maxCellRowMargin <= (minCellRowMargin + (dpi / 72))) {
				if (DEBUG_IS_TABLE) System.out.println(" ==> differences in row margins too small");
				return false;
			}
			
			//	re-get table rows with increased margin
			tableRows = this.getTableRows(block, avgCellRowMargin);
			if (tableRows == null)
				return false;
			
			//	re-get table cells
			tableCells = this.getTableCells(page, block, tableRows, tableCols, true);
			if (tableCells == null)
				return false;
			
			//	we've salvaged this one
			if (DEBUG_IS_TABLE) System.out.println(" --> row margin " + avgCellRowMargin + " looks better than " + minRowMargin);
		}
		
		//	TODO add further checks (do we need any ?!?)
		
		//	mark table
		ImRegion table = new ImRegion(page.getDocument(), page.pageId, block.bounds, ImRegion.TABLE_TYPE);
		this.markTable(page, table, tableRows, tableCols, tableCells, blockWords);
		
		//	indicate success
		if (DEBUG_IS_TABLE) System.out.println(" ==> table marked");
		return true;
	}
	
	private boolean markIfContainsTable(ImPage page, ImRegion block, ImWord[] blockWords, int dpi, int minColMargin, int minRowMargin) {
		if (DEBUG_IS_TABLE) System.out.println("Testing for contained table: " + block.bounds.toString() + " on page " + block.pageId + " with " + blockWords.length + " words");
		
		//	this one's not tall enough (less than 2 inches)
		if ((block.bounds.bottom - block.bounds.top) < (2 * dpi))
			return false;
		
		//	try to get table rows (if this fails, it's no use trying without top and bottom)
		ImRegion[] blockRows = this.getTableRows(block, minRowMargin);
		if (blockRows == null)
			return false;
		
		//	generate test block (middle two thirds of block)
		int middleBlockTop = (block.bounds.top + ((block.bounds.bottom - block.bounds.top) / 6));
		int middleBlockBottom = (block.bounds.bottom - ((block.bounds.bottom - block.bounds.top) / 6));
		ImRegion middleBlock = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(block.bounds.left, block.bounds.right, middleBlockTop, middleBlockBottom), "test");
		if (DEBUG_IS_TABLE) System.out.println(" - middle block is " + middleBlock.bounds);
		
		//	try to get table columns from test block
		ImRegion[] middleBlockCols = this.getTableColumns(middleBlock, minColMargin);
		if (middleBlockCols == null) {
			if (DEBUG_IS_TABLE) System.out.println(" ==> invalid columns in middle block");
			return false;
		}
		if (DEBUG_IS_TABLE) System.out.println(" - middle block columns OK (" + middleBlockCols.length + "):\r\n  " + getRegionBoundList(middleBlockCols));
		int middleBlockColScore = this.getTableColumnGapScore(middleBlockCols);
		if (DEBUG_IS_TABLE) System.out.println(" - middle block column score is " + middleBlockColScore);
		
		//	compute table top by removing block rows
		int tableTop = block.bounds.top;
		for (int r = 0; r < blockRows.length; r++) {
			if (middleBlockTop < blockRows[r].bounds.top)
				return false;
			ImRegion topBlock = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(block.bounds.left, block.bounds.right, blockRows[r].bounds.top, middleBlockBottom), "test");
			if (DEBUG_IS_TABLE) System.out.println(" - testing top block " + topBlock.bounds);
			
			//	check if current block top would produce valid columns
			ImRegion[] topBlockCols = this.getTableColumns(topBlock, minColMargin, middleBlockCols.length);
			if (topBlockCols == null) {
				if (DEBUG_IS_TABLE) System.out.println(" --> invalid columns");
				continue;
			}
			if (topBlockCols.length < middleBlockCols.length) {
				if (DEBUG_IS_TABLE) System.out.println(" --> fewer columns than middle block alone (" + topBlockCols.length + "):\r\n    " + getRegionBoundList(topBlockCols));
				continue;
			}
			int topBlockColScore = this.getTableColumnGapScore(topBlockCols);
			if ((topBlockColScore * 5) < (middleBlockColScore * 4)) /* allow some 20% loss in column score (column headers might be wider than entries) */ {
				if (DEBUG_IS_TABLE) System.out.println(" --> loss in column score too high (" + topBlockColScore + " vs. " + middleBlockColScore + "):\r\n    " + getRegionBoundList(topBlockCols));
				continue;
			}
			if (DEBUG_IS_TABLE) System.out.println(" - loss in column score tolerated (" + topBlockColScore + " vs. " + middleBlockColScore + ")");
			
			//	check if current block top would produce valid cells (including column headers and row labels)
			ImRegion[] topBlockRows = this.getTableRows(topBlock, minRowMargin);
			if (topBlockRows == null) {
				if (DEBUG_IS_TABLE) System.out.println(" --> invalid rows");
				continue;
			}
			ImRegion[][] topBlockCells = this.getTableCells(page, topBlock, topBlockRows, topBlockCols, true);
			if (topBlockCells != null) {
				tableTop = blockRows[r].bounds.top;
				if (DEBUG_IS_TABLE) System.out.println(" - found table top at " + tableTop);
				break;
			}
			else if (DEBUG_IS_TABLE) System.out.println(" --> invalid cells");
		}
		
		//	compute table bottom by removing block rows
		int tableBottom = block.bounds.bottom;
		for (int r = (blockRows.length-1); r >= 0; r--) {
			if (blockRows[r].bounds.bottom < middleBlockBottom)
				return false;
			ImRegion bottomBlock = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(block.bounds.left, block.bounds.right, middleBlockTop, blockRows[r].bounds.bottom), "test");
			ImRegion[] bottomBlockCols = this.getTableColumns(bottomBlock, minColMargin);
			if ((bottomBlockCols != null) && (middleBlockCols.length <= bottomBlockCols.length)) {
				tableBottom = blockRows[r].bounds.bottom;
				break;
			}
		}
		
		//	assign words to block parts
		LinkedList aboveTableWords = new LinkedList();
		LinkedList tableWords = new LinkedList();
		LinkedList belowTableWords = new LinkedList();
		for (int w = 0; w < blockWords.length; w++) {
			if (blockWords[w].centerY < tableTop)
				aboveTableWords.add(blockWords[w]);
			else if (blockWords[w].centerY > tableBottom)
				belowTableWords.add(blockWords[w]);
			else tableWords.add(blockWords[w]);
		}
		
		//	check bounds
		if (tableBottom < tableTop) {
			if (DEBUG_IS_TABLE) System.out.println(" ==> empty contained table");
			return false;
		}
		
		//	mark block above table (if any)
		if (aboveTableWords.size() != 0)
			new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) aboveTableWords.toArray(new ImWord[aboveTableWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	mark block below table (if any)
		if (belowTableWords.size() != 0)
			new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) belowTableWords.toArray(new ImWord[belowTableWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
		
		//	mark table region
		ImRegion table = new ImRegion(page.getDocument(), page.pageId, new BoundingBox(block.bounds.left, block.bounds.right, tableTop, tableBottom), ImRegion.TABLE_TYPE);
		if (DEBUG_IS_TABLE) System.out.println(" ==> contained table found at " + table.bounds);
		
		//	try to get table columns
		ImRegion[] tableCols = this.getTableColumns(table, minColMargin, middleBlockCols.length);
		if (tableCols == null)
			return false;
		
		//	try to get table rows
		ImRegion[] tableRows = this.getTableRows(table, minRowMargin);
		if (tableRows == null)
			return false;
		
		//	get table cells
		ImRegion[][] tableCells = this.getTableCells(page, table, tableRows, tableCols, false);
		if (tableCells == null)
			return false;
		
		//	test column headers
		if (!this.checkColumnHeaders(page, tableCols, tableCells))
			return false;
		
		//	large number of rows, but incomplete row labels or sparse table body, try increasing row margin
		if ((tableRows.length >= 9) && (!this.checkRowLabels(page, tableCells) || !this.checkTableBody(page, tableCells))) {
			if (DEBUG_IS_TABLE) System.out.println(" --> re-trying with increased row margin");
			
			//	compute average margin of table rows (we only have a chance if cell internal line margin is less than table row margin)
			int minCellRowMargin = (table.bounds.bottom - table.bounds.top);
			int maxCellRowMargin = 0;
			int cellRowMarginSum = 0;
			for (int r = 1; r < tableCells.length; r++) {
				int cellRowMargin = ((tableCells[r][0].bounds.top - tableCells[r-1][0].bounds.bottom));
				minCellRowMargin = Math.min(minCellRowMargin, cellRowMargin);
				maxCellRowMargin = Math.max(maxCellRowMargin, cellRowMargin);
				cellRowMarginSum += cellRowMargin;
			}
			int avgCellRowMargin = ((cellRowMarginSum + (tableCells.length / 2)) / (tableCells.length - 1));
			if (DEBUG_IS_TABLE) System.out.println(" --> average measured row margin is " + avgCellRowMargin);
			
			//	no use trying again if minimum row margin didn't increase
			if (avgCellRowMargin <= minRowMargin) {
				if (DEBUG_IS_TABLE) System.out.println(" ==> no use re-trying");
				return false;
			}
			
			//	no use trying again if difference too small
			if (maxCellRowMargin <= (minCellRowMargin + (dpi / 72))) {
				if (DEBUG_IS_TABLE) System.out.println(" ==> differences in row margins too small");
				return false;
			}
			
			//	re-get table rows with increased margin
			tableRows = this.getTableRows(table, avgCellRowMargin);
			if (tableRows == null)
				return false;
			
			//	re-get table cells
			tableCells = this.getTableCells(page, table, tableRows, tableCols, true);
			if (tableCells == null)
				return false;
			
			//	we've salvaged this one
			if (DEBUG_IS_TABLE) System.out.println(" --> row margin " + avgCellRowMargin + " looks better than " + minRowMargin);
		}
		
		//	mark table
		this.markTable(page, table, tableRows, tableCols, tableCells, ((ImWord[]) tableWords.toArray(new ImWord[tableWords.size()])));
		
		//	indicate success
		if (DEBUG_IS_TABLE) System.out.println(" ==> contained table marked at " + table.bounds);
		return true;
	}
	
	private static final String getRegionBoundList(ImRegion[] regs) {
		StringBuffer rbl = new StringBuffer();
		for (int r = 0; r < regs.length; r++)
			rbl.append(" " + regs[r].bounds);
		return rbl.toString();
	}
	
	private int getTableColumnGapScore(ImRegion[] tableCols) {
		int minColGap = Integer.MAX_VALUE;
		for (int c = 1; c < tableCols.length; c++)
			minColGap = Math.min(minColGap, (tableCols[c].bounds.left - tableCols[c-1].bounds.right));
		return (minColGap * (tableCols.length - 1));
	}
	
	private ImRegion[] getTableColumns(ImRegion block, int minColMargin) {
		return this.getTableColumns(block, minColMargin, 3);
	}
	private ImRegion[] getTableColumns(ImRegion block, int minColMargin, int minColCount) {
		
		//	try to get table columns
		ImRegion[] tableCols = ImUtils.getTableColumns(block, minColMargin, minColCount);
		if ((tableCols == null) || (tableCols.length < 3)) {
			if (DEBUG_IS_TABLE) System.out.println(" ==> too few columns");
			return null;
		}
		
		//	check column margin
		for (int c = 1; c < tableCols.length; c++) {
			if ((tableCols[c].bounds.left - tableCols[c-1].bounds.right) < minColMargin)
				return null;
		}
		
		//	check column widths
		int minColWidth = (block.bounds.right - block.bounds.left);
		int maxColWidth = 0;
		for (int c = 0; c < tableCols.length; c++) {
			minColWidth = Math.min(minColWidth, (tableCols[c].bounds.right - tableCols[c].bounds.left));
			maxColWidth = Math.max(maxColWidth, (tableCols[c].bounds.right - tableCols[c].bounds.left));
		}
		if ((maxColWidth * 2) > (block.bounds.right - block.bounds.left)) {
			if (DEBUG_IS_TABLE) System.out.println(" ==> columns too irregular");
			return null;
		}
		
		//	these look good
		return tableCols;
	}
	
	private ImRegion[] getTableRows(ImRegion block, int minRowMargin) {
		return this.getTableRows(block, minRowMargin, 3);
	}
	private ImRegion[] getTableRows(ImRegion block, int minRowMargin, int minRowCount) {
		
		//	try to get table rows
		ImRegion[] tableRows = ImUtils.getTableRows(block, minRowMargin, minRowCount);
		if ((tableRows == null) || (tableRows.length < 3)) {
			if (DEBUG_IS_TABLE) System.out.println(" ==> too few rows");
			return null;
		}
		
		//	check row heights
		int minRowHeight = (block.bounds.bottom - block.bounds.top);
		int maxRowHeight = 0;
		for (int r = 0; r < tableRows.length; r++) {
			minRowHeight = Math.min(minRowHeight, (tableRows[r].bounds.bottom - tableRows[r].bounds.top));
			maxRowHeight = Math.max(maxRowHeight, (tableRows[r].bounds.bottom - tableRows[r].bounds.top));
		}
		if ((maxRowHeight * 2) > (block.bounds.bottom - block.bounds.top)) {
			if (DEBUG_IS_TABLE) System.out.println(" ==> rows too irregular");
			return null;
		}
		
		//	these look good
		return tableRows;
	}
	
	private ImRegion[][] getTableCells(ImPage page, ImRegion block, ImRegion[] tableRows, ImRegion[] tableCols, boolean includeCellChecks) {
		
		//	get table cells
		ImRegion[][] tableCells = ImUtils.getTableCells(block, tableRows, tableCols);
		if (tableCells == null) {
			if (DEBUG_IS_TABLE) System.out.println(" ==> cell extraction failed");
			return null;
		}
		
		//	test if first row (column headers) is fully occupied, safe for first column
		if (includeCellChecks && !this.checkColumnHeaders(page, tableCols, tableCells))
			return null;
		
		//	test if first column (row labels) is fully occupied, safe for top row(s)
		if (includeCellChecks && !this.checkRowLabels(page, tableCells))
			return null;
		
		//	test if cells below and right of label rows and column have content
		if (includeCellChecks && !this.checkTableBody(page, tableCells))
			return null;
		
		//	these look good
		return tableCells;
	}
	
	private boolean checkColumnHeaders(ImPage page, ImRegion[] tableCols, ImRegion[][] tableCells) {
		for (int c = 1; c < tableCells[0].length; c++) {
			ImWord[] cellWords = page.getWordsInside(tableCells[0][c].bounds);
			if (cellWords.length == 0) {
				if (DEBUG_IS_TABLE) System.out.println(" ==> column headers incomplete");
				return false;
			}
		}
		return true;
	}
	
	private boolean checkRowLabels(ImPage page, ImRegion[][] tableCells) {
		for (int r = ((tableCells.length < 15) ? 1 : 2); r < tableCells.length; r++) {
			ImWord[] cellWords = page.getWordsInside(tableCells[r][0].bounds);
			if (cellWords.length == 0) {
				if (DEBUG_IS_TABLE) System.out.println(" ==> row labels incomplete");
				return false;
			}
		}
		return true;
	}
	
	private boolean checkTableBody(ImPage page, ImRegion[][] tableCells) {
		int tableBodyCells = 0;
		int emptyTableBodyCells = 0;
		for (int r = 1; r < tableCells.length; r++)
			for (int c = 1; c < tableCells[r].length; c++) {
				tableBodyCells++;
				ImWord[] tableCellWords = page.getWordsInside(tableCells[r][c].bounds);
				if (tableCellWords.length == 0)
					emptyTableBodyCells++;
			}
		if ((emptyTableBodyCells * 2) > tableBodyCells) {
			if (DEBUG_IS_TABLE) System.out.println(" ==> table content extremely sparse, " + emptyTableBodyCells + " empty out of " + tableBodyCells);
			return false;
		}
		return true;
	}
	
	private void markTable(ImPage page, ImRegion tableRegion, ImRegion[] tableRows, ImRegion[] tableCols, ImRegion[][] tableCells, ImWord[] tableWords) {
		
		//	add regions to page
		page.addRegion(tableRegion);
		for (int r = 0; r < tableRows.length; r++) {
			if (tableRows[r].getPage() == null)
				page.addRegion(tableRows[r]);
		}
		for (int c = 0; c < tableCols.length; c++) {
			if (tableCols[c].getPage() == null)
				page.addRegion(tableCols[c]);
		}
		for (int r = 0; r < tableCells.length; r++)
			for (int c = 0; c < tableCells[r].length; c++) {
				if (tableCells[r][c].getPage() == null)
					page.addRegion(tableCells[r][c]);
			}
		
		//	cut table out of main text
		synchronized (page.getDocument()) {
			ImUtils.makeStream(tableWords, ImWord.TEXT_STREAM_TYPE_TABLE, null);
		}
		
		//	flatten out table content
		for (int w = 0; w < tableWords.length; w++) {
			if (tableWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				tableWords[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
		}
		
		//	remove all other regions embedded in table
		ImRegion[] tableSubRegions = tableRegion.getRegions();
		for (int r = 0; r < tableSubRegions.length; r++) {
			if (!tableSubRegions[r].getType().startsWith(ImRegion.TABLE_TYPE))
				page.removeRegion(tableSubRegions[r]);
		}
		
		//	order words from each cell as a text stream
		synchronized (page.getDocument()) {
			ImUtils.orderTableWords(tableCells);
		}
	}
	
	private static final boolean DEBUG_IS_FOOTNOTE = false;
	private boolean isFootnote(ImRegion paragraph, ImWord[] paragraphWords, int docFontSize, Pattern[] startPatterns, int minFontSize, int maxFontSize) {
		
		//	compute font size
		int fontSizeSum = 0;
		int fontSizeWordCount = 0;
		for (int w = 0; w < paragraphWords.length; w++) {
			String wfsStr = ((String) paragraphWords[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
			if (wfsStr != null) try {
				int wfs = Integer.parseInt(wfsStr);
				if (wfs < minFontSize)
					return false;
				if (maxFontSize < wfs)
					return false;
				fontSizeSum += wfs;
				fontSizeWordCount++;
			} catch (NumberFormatException nfe) {}
		}
		int paragraphFontSize = ((fontSizeWordCount == 0) ? -1 : (fontSizeSum / fontSizeWordCount));
		
		//	if footnote is large (> 50% of page width, or > 30 words), require font size to be lower than in main text
		int paragraphWidth = (paragraph.bounds.right - paragraph.bounds.left);
		int pageWidth = (paragraph.getPage().bounds.right - paragraph.getPage().bounds.left);
		if ((((paragraphWidth * 2) > pageWidth) || (paragraphWords.length > 30)) && (paragraphFontSize >= docFontSize)) {
			if (DEBUG_IS_FOOTNOTE) System.out.println(" ==> font too large for big footnote (" + ((paragraphWidth * 100) / pageWidth) + "% of page width, " + paragraphWords.length + " words, font size " + paragraphFontSize + " vs. " + docFontSize + " in doc)");
			return false;
		}
		
		//	find end of first line
		ImWord firstLineEnd = paragraphWords[0];
		for (int w = 0; w < paragraphWords.length; w++) {
			firstLineEnd = paragraphWords[w];
			if (firstLineEnd.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				break;
			ImWord firstLineEndNext = firstLineEnd.getNextWord();
			if (firstLineEndNext == null)
				break;
			if (firstLineEndNext.bounds.right < firstLineEnd.bounds.left)
				break;
			if (firstLineEnd.bounds.bottom < firstLineEndNext.bounds.top)
				break;
			if (firstLineEndNext.bounds.bottom < firstLineEnd.bounds.top)
				break;
		}
		
		//	concatenate first line of paragraph
		String paragraphFirstLine = ImUtils.getString(paragraphWords[0], firstLineEnd, true);
		if (DEBUG_IS_FOOTNOTE) System.out.println(" - footnote test first line is " + paragraphFirstLine);
		
		//	test paragraph start against detector patterns
		for (int p = 0; p < startPatterns.length; p++)
			if (startPatterns[p].matcher(paragraphFirstLine).matches()) {
				if (DEBUG_IS_FOOTNOTE) System.out.println(" ==> pattern match");
				return true;
			}
		
		//	no match found
		if (DEBUG_IS_FOOTNOTE) System.out.println(" ==> no pattern match");
		return false;
	}
	
	private static final boolean DEBUG_IS_CAPTION = true;
	private boolean isCaption(ImRegion paragraph, ImWord[] paragraphWords, Pattern[] startPatterns, int minFontSize, int maxFontSize, boolean startIsBold) {
		
		//	test bold property and font size first thing
		if (startIsBold && !paragraphWords[0].hasAttribute(ImWord.BOLD_ATTRIBUTE)) {
			if (DEBUG_IS_CAPTION) System.out.println(" ==> not bold at " + paragraphWords[0].getString());
			return false;
		}
		if ((0 < minFontSize) || (maxFontSize < 72)) try {
			for (int w = 0; w < paragraphWords.length; w++) {
				int wfs = Integer.parseInt((String) paragraphWords[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, "-1"));
				if (wfs < 0)
					continue;
				if (wfs < minFontSize) {
					if (DEBUG_IS_CAPTION) System.out.println(" ==> font smaller than " + minFontSize + " at " + wfs + " for " + paragraphWords[w].getString());
					return false;
				}
				if (maxFontSize < wfs) {
					if (DEBUG_IS_CAPTION) System.out.println(" ==> font larger than " + maxFontSize + " at " + wfs + " for " + paragraphWords[w].getString());
					return false;
				}
			}
		}
		catch (NumberFormatException nfe) {
			return false;
		}
		
		//	find end of first line
		ImWord firstLineEnd = paragraphWords[0];
		for (int w = 0; w < paragraphWords.length; w++) {
			firstLineEnd = paragraphWords[w];
			if (firstLineEnd.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				break;
			ImWord firstLineEndNext = firstLineEnd.getNextWord();
			if (firstLineEndNext == null)
				break;
			if (firstLineEndNext.bounds.right < firstLineEnd.bounds.left)
				break;
			if (firstLineEnd.bounds.bottom < firstLineEndNext.bounds.top)
				break;
			if (firstLineEndNext.bounds.bottom < firstLineEnd.bounds.top)
				break;
		}
		
		//	concatenate first line of paragraph
		String paragraphFirstLine = ImUtils.getString(paragraphWords[0], firstLineEnd, true);
		if (DEBUG_IS_CAPTION) System.out.println("Caption test first line is " + paragraphFirstLine);
		
		//	test first line against detector patterns
		boolean noCaptionStart = true;
		for (int p = 0; p < startPatterns.length; p++)
			if (startPatterns[p].matcher(paragraphFirstLine).matches()) {
				noCaptionStart = false;
				break;
			}
		if (noCaptionStart) {
			if (DEBUG_IS_CAPTION) System.out.println(" ==> no pattern match");
			return false;
		}
		
		//	count words and numbers in first line
		int firstLineWordCount = 0;
		int firstLineNumberCount = 0;
		for (int w = 1; w < paragraphWords.length; w++) {
			String wordString = paragraphWords[w].getString();
			if ((wordString == null) || (wordString.length() == 0))
				continue;
			if (Gamta.isNumber(wordString)) {
				if (wordString.length() < 4)
					firstLineNumberCount++;
				else firstLineWordCount++;
			}
			if (Gamta.isRomanNumber(wordString))
				firstLineNumberCount++;
			if (Gamta.isWord(wordString)) {
				if (wordString.length() > 1)
					firstLineWordCount++;
				else if (wordString.matches("[a-zA-Z]"))
					firstLineNumberCount++;
			}
			if (paragraphWords[w] == firstLineEnd)
				break;
		}
		
		//	more numbers than words ==> reference to caption
		if (firstLineNumberCount == 0) {
			if (DEBUG_IS_CAPTION) System.out.println(" ==> no numbers at all, not a caption");
			return false;
		}
		
		//	more numbers than words ==> reference to caption (we can cut some more slack if we have other clues)
		if ((startIsBold || (startPatterns != this.captionStartPatterns)) ? ((firstLineWordCount * 3) < (firstLineNumberCount * 2)) : (firstLineWordCount < firstLineNumberCount)) {
			if (DEBUG_IS_CAPTION) System.out.println(" ==> " + firstLineWordCount + " words vs. " + firstLineNumberCount + " numbers, caption reference");
			return false;
		}
		
		//	more words than numbers ==> actual caption
		if (DEBUG_IS_CAPTION) System.out.println(" ==> " + firstLineWordCount + " words vs. " + firstLineNumberCount + " numbers, caption");
		return true;
	}
	
	private ImRegion getAboveCaptionTarget(ImPage page, ImRegion caption, int dpi, boolean isTableCaption) {
		
		//	get regions
		ImRegion[] pageRegions = page.getRegions(isTableCaption ? ImRegion.TABLE_TYPE : null);
		
		//	seek suitable target regions
		ImRegion targetRegion = null;
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (isTableCaption != ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one's too far down (we need to cut this a little slack, as images can be masked to smaller sizes in born-digital PDFs)
//			if (caption.bounds.top < pageRegions[r].bounds.bottom)
//				continue;
			if (((caption.bounds.top + caption.bounds.bottom) / 2) < pageRegions[r].bounds.bottom)
				continue;
			
			//	check if size is sufficient
			if (((pageRegions[r].bounds.bottom - pageRegions[r].bounds.top) * 2) < dpi)
				continue;
			
			//	check alignment
			if (!ImUtils.isCaptionBelowTargetMatch(caption.bounds, pageRegions[r].bounds, dpi))
				continue;
			
			//	check if candidate target contains words that cannot belong to a caption target area, and find lower edge
			ImWord[] regionWords = page.getWordsInside(pageRegions[r].bounds);
			boolean gotNonCaptionableWords = false;
			for (int w = 0; w < regionWords.length; w++) {
				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(regionWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(regionWords[w].getTextStreamType()))
					continue;
				if (isTableCaption && ImWord.TEXT_STREAM_TYPE_TABLE.equals(regionWords[w].getTextStreamType()))
					continue;
				gotNonCaptionableWords = true;
				break;
			}
			if (gotNonCaptionableWords)
				continue;
			
			//	this one looks good
			if (isTableCaption) // return target table right away
				return pageRegions[r];
			else { // store target figure (might be part of actual figure, to be restored below)
				targetRegion = pageRegions[r];
				break;
			}
		}
		
		//	do we have a target region to start with?
		if (targetRegion == null)
			return null;
		
		//	try and restore images cut apart horizontally by page structure detection (vertical splits result in the parent region being retained, so we'll find the latter first)
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one lies inside what we already have
			if (targetRegion.bounds.includes(pageRegions[r].bounds, false))
				continue;
			
			//	this one's too far down (we need to cut this a little slack, as images can be masked to smaller sizes in born-digital PDFs)
//			if (caption.bounds.top < pageRegions[r].bounds.bottom)
//				continue;
			if (((caption.bounds.top + caption.bounds.bottom) / 2) < pageRegions[r].bounds.bottom)
				continue;
			
			//	check if candidate aggregate target contains words that cannot belong to a caption target area, and find lower edge
			BoundingBox aggregateBounds = new BoundingBox(Math.min(targetRegion.bounds.left, pageRegions[r].bounds.left), Math.max(targetRegion.bounds.right, pageRegions[r].bounds.right), Math.min(targetRegion.bounds.top, pageRegions[r].bounds.top), Math.max(targetRegion.bounds.bottom, pageRegions[r].bounds.bottom));
			ImWord[] aggregateWords = page.getWordsInside(aggregateBounds);
			boolean gotNonCaptionableWords = false;
			for (int w = 0; w < aggregateWords.length; w++) {
				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(aggregateWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(aggregateWords[w].getTextStreamType()))
					continue;
				gotNonCaptionableWords = true;
				break;
			}
			if (gotNonCaptionableWords)
				continue;
			
			//	include this one in the aggregate
			targetRegion = new ImRegion(targetRegion.getDocument(), targetRegion.pageId, aggregateBounds, ImRegion.REGION_ANNOTATION_TYPE);
		}
		
		//	return what we got
		return targetRegion;
	}
	
	private ImRegion getBelowCaptionTarget(ImPage page, ImRegion caption, int dpi, boolean isTableCaption) {
		
		//	get regions
		ImRegion[] pageRegions = page.getRegions(isTableCaption ? ImRegion.TABLE_TYPE : null);
		
		//	seek suitable target regions
		ImRegion targetRegion = null;
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (isTableCaption != ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one's too high up (we need to cut this a little slack, as images can be masked to smaller sizes in born-digital PDFs)
//			if (pageRegions[r].bounds.top < caption.bounds.bottom)
//				continue;
			if (pageRegions[r].bounds.top < ((caption.bounds.top + caption.bounds.bottom) / 2))
				continue;
			
			//	check if size is sufficient
			if (((pageRegions[r].bounds.bottom - pageRegions[r].bounds.top) * 2) < dpi)
				continue;
			
			//	check alignment
			if (!ImUtils.isCaptionAboveTargetMatch(caption.bounds, pageRegions[r].bounds, dpi))
				continue;
			
			//	check if candidate target contains words that cannot belong to a caption target area, and find lower edge
			ImWord[] pageRegionWords = page.getWordsInside(pageRegions[r].bounds);
			boolean gotNonCaptionableWords = false;
			for (int w = 0; w < pageRegionWords.length; w++) {
				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(pageRegionWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(pageRegionWords[w].getTextStreamType()))
					continue;
				if (isTableCaption && ImWord.TEXT_STREAM_TYPE_TABLE.equals(pageRegionWords[w].getTextStreamType()))
					continue;
				gotNonCaptionableWords = true;
				break;
			}
			if (gotNonCaptionableWords)
				continue;
			
			//	this one looks good
			if (isTableCaption) // return target table right away
				return pageRegions[r];
			else { // store target figure (might be part of actual figure, to be restored below)
				targetRegion = pageRegions[r];
				break;
			}
		}
		
		//	do we have a target region to start with?
		if (targetRegion == null)
			return null;
		
		//	try and restore images cut apart horizontally by page structure detection (vertical splits result in the parent region being retained, so we'll find the latter first)
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one lies inside what we already have
			if (targetRegion.bounds.includes(pageRegions[r].bounds, false))
				continue;
			
			//	this one's too high up (we need to cut this a little slack, as images can be masked to smaller sizes in born-digital PDFs)
//			if (pageRegions[r].bounds.top < caption.bounds.bottom)
//				continue;
			if (pageRegions[r].bounds.top < ((caption.bounds.top + caption.bounds.bottom) / 2))
				continue;
			
			//	check if candidate aggregate target contains words that cannot belong to a caption target area, and find lower edge
			BoundingBox aggregateBounds = new BoundingBox(Math.min(targetRegion.bounds.left, pageRegions[r].bounds.left), Math.max(targetRegion.bounds.right, pageRegions[r].bounds.right), Math.min(targetRegion.bounds.top, pageRegions[r].bounds.top), Math.max(targetRegion.bounds.bottom, pageRegions[r].bounds.bottom));
			ImWord[] aggregateWords = page.getWordsInside(aggregateBounds);
			boolean gotNonCaptionableWords = false;
			for (int w = 0; w < aggregateWords.length; w++) {
				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(aggregateWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(aggregateWords[w].getTextStreamType()))
					continue;
				gotNonCaptionableWords = true;
				break;
			}
			if (gotNonCaptionableWords)
				continue;
			
			//	include this one in the aggregate
			targetRegion = new ImRegion(targetRegion.getDocument(), targetRegion.pageId, aggregateBounds, ImRegion.REGION_ANNOTATION_TYPE);
		}
		
		//	return what we got
		return targetRegion;
	}
	
	private ImRegion getBesideCaptionTarget(ImPage page, ImRegion caption, int dpi, boolean isTableCaption) {
		
		//	get regions
		ImRegion[] pageRegions = page.getRegions(isTableCaption ? ImRegion.TABLE_TYPE : null);
		
		//	compute vertical center of caption
		int captionCenterY = ((caption.bounds.top + caption.bounds.bottom) / 2);
		
		//	seek suitable target regions
		ImRegion targetRegion = null;
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (isTableCaption != ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one's too far down
			if (captionCenterY < pageRegions[r].bounds.top)
				continue;
			
			//	this one's too high up
			if (pageRegions[r].bounds.bottom < captionCenterY)
				continue;
			
			//	check if size is sufficient
			if (((pageRegions[r].bounds.bottom - pageRegions[r].bounds.top) * 2) < dpi)
				continue;
			
			//	check if candidate target contains words that cannot belong to a caption target area, and find lower edge
			ImWord[] pageRegionWords = page.getWordsInside(pageRegions[r].bounds);
			boolean gotNonCaptionableWords = false;
			for (int w = 0; w < pageRegionWords.length; w++) {
				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(pageRegionWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(pageRegionWords[w].getTextStreamType()))
					continue;
				if (isTableCaption && ImWord.TEXT_STREAM_TYPE_TABLE.equals(pageRegionWords[w].getTextStreamType()))
					continue;
				gotNonCaptionableWords = true;
				break;
			}
			if (gotNonCaptionableWords)
				continue;
			
			//	this one looks good
			if (isTableCaption) // return target table right away
				return pageRegions[r];
			else { // store target figure (might be part of actual figure, to be restored below)
				targetRegion = pageRegions[r];
				break;
			}
		}
		
		//	do we have a target region to start with?
		if (targetRegion == null)
			return null;
		
		//	try and restore images cut apart horizontally by page structure detection (vertical splits result in the parent region being retained, so we'll find the latter first)
		for (int r = 0; r < pageRegions.length; r++) {
			
			//	ignore lines and paragraphs
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(pageRegions[r].getType()) || ImRegion.PARAGRAPH_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	ignore tables we're not out for
			if (ImRegion.TABLE_TYPE.equals(pageRegions[r].getType()))
				continue;
			
			//	this one's too far down
			if (captionCenterY < pageRegions[r].bounds.top)
				continue;
			
			//	this one's too high up
			if (pageRegions[r].bounds.bottom < captionCenterY)
				continue;
			
			//	this one lies inside what we already have
			if (targetRegion.bounds.includes(pageRegions[r].bounds, false))
				continue;
			
			//	check if candidate aggregate target contains words that cannot belong to a caption target area, and find lower edge
			BoundingBox aggregateBounds = new BoundingBox(Math.min(targetRegion.bounds.left, pageRegions[r].bounds.left), Math.max(targetRegion.bounds.right, pageRegions[r].bounds.right), Math.min(targetRegion.bounds.top, pageRegions[r].bounds.top), Math.max(targetRegion.bounds.bottom, pageRegions[r].bounds.bottom));
			ImWord[] aggregateWords = page.getWordsInside(aggregateBounds);
			boolean gotNonCaptionableWords = false;
			for (int w = 0; w < aggregateWords.length; w++) {
				if (ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(aggregateWords[w].getTextStreamType()) || ImWord.TEXT_STREAM_TYPE_DELETED.equals(aggregateWords[w].getTextStreamType()))
					continue;
				gotNonCaptionableWords = true;
				break;
			}
			if (gotNonCaptionableWords)
				continue;
			
			//	include this one in the aggregate
			targetRegion = new ImRegion(targetRegion.getDocument(), targetRegion.pageId, aggregateBounds, ImRegion.REGION_ANNOTATION_TYPE);
		}
		
		//	return what we got
		return targetRegion;
	}
	
	private static ImWord[] getLargestTextStreamWords(ImWord[] words) {
		
		//	nothing to filter here
		if (words.length < 2)
			return words;
		
		//	count text stream IDs
		CountingSet textStreamIDs = new CountingSet(new TreeMap());
		for (int w = 0; w < words.length; w++)
			textStreamIDs.add(words[w].getTextStreamId());
		
		//	nothing to sort out here
		if (textStreamIDs.elementCount() == 1)
			return words;
		
		//	find most frequent text stream ID
		String mostFrequentTextStreamId = "";
		for (Iterator tsidit = textStreamIDs.iterator(); tsidit.hasNext();) {
			String textStreamId = ((String) tsidit.next());
			if (textStreamIDs.getCount(textStreamId) > textStreamIDs.getCount(mostFrequentTextStreamId))
				mostFrequentTextStreamId = textStreamId;
		}
		
		//	extract words from largest text stream
		ImWord[] largestTextStreamWords = new ImWord[textStreamIDs.getCount(mostFrequentTextStreamId)];
		for (int w = 0, ltsw = 0; w < words.length; w++) {
			if (mostFrequentTextStreamId.equals(words[w].getTextStreamId()))
				largestTextStreamWords[ltsw++] = words[w];
		}
		
		//	finally ...
		return largestTextStreamWords;
	}
	
	private static final boolean DEBUG_IS_PAGE_TITLE = false;
	private boolean isPageTitle(ImRegion region, ImWord[] regionWords, int pageCount, CountingSet edgeWords, CountingSet allWords, HashSet pageNumberWordIDs) {
		
		//	do we have a page number?
		for (int w = 0; w < regionWords.length; w++)
			if (pageNumberWordIDs.contains(regionWords[w].getLocalID())) {
				if (DEBUG_IS_PAGE_TITLE) System.out.println(" ==> contains page number");
				return true;
			}
		
		//	check words
		TreeSet edgeWordStrings = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int w = 0; w < regionWords.length; w++) {
			String regionWordString = regionWords[w].getString();
			if (Gamta.isWord(regionWordString) || Gamta.isNumber(regionWordString))
				edgeWordStrings.add(regionWordString);
		}
		if (DEBUG_IS_PAGE_TITLE) System.out.println(" - got " + edgeWordStrings.size() + " edge word strings");
		int edgeWordFrequencySum = 0;
		for (Iterator wit = edgeWordStrings.iterator(); wit.hasNext();) {
			String edgeWord = ((String) wit.next());
			int edgeWordFrequency = edgeWords.getCount(edgeWord);
			if (edgeWordFrequency > 1) // own occurrence is not enough
				edgeWordFrequencySum += edgeWords.getCount(edgeWord);
			if (DEBUG_IS_PAGE_TITLE) System.out.println("   - " + edgeWord + " occurs " + edgeWords.getCount(edgeWord) + " times on edges");
		}
		if ((edgeWordFrequencySum * 2) > (pageCount * edgeWordStrings.size())) {
			if (DEBUG_IS_PAGE_TITLE) System.out.println(" ==> contains many frequent edge words");
			return true;
		}
		
		//	check case as last resort
		int charCount = 0;
		int capCharCount = 0;
		for (int w = 0; w < regionWords.length; w++) {
			String wordStr = regionWords[w].getString();
			if (!Gamta.isWord(wordStr))
				continue;
			charCount += wordStr.length();
			for (int c = 0; c < wordStr.length(); c++) {
				if (wordStr.charAt(c) == Character.toUpperCase(wordStr.charAt(c)))
					capCharCount++;
			}
		}
		if ((capCharCount * 2) > charCount)  {
			if (DEBUG_IS_PAGE_TITLE) System.out.println(" ==> many capital letters, likely a page title");
			return true;
		}
		else {
			if (DEBUG_IS_PAGE_TITLE) System.out.println(" ==> few capital letters, likely a not page title");
			return false;
		}
	}
	
	private PageData getPageData(ImPage page, int pageImageDpi, BoundingBox[] headerAreas, ProgressMonitor pm) {
		PageData pageData = new PageData(page);
		
		//	get regions
		ImRegion[] regions = page.getRegions();
		pm.setInfo(" - got " + regions.length + " regions in page " + page.pageId);
		
		//	collect regions of interest
		for (int r = 0; r < regions.length; r++) {
			pm.setInfo(" - assessing region " + regions[r].getType() + "@" + regions[r].bounds.toString());
			
			//	lines are not of interest here, as we are out for standalone blocks
			if (LINE_ANNOTATION_TYPE.equals(regions[r].getType())) {
				pm.setInfo(" --> ignoring line");
				continue;
			}
			
			//	no header areas defined, have to use heuristics
			if (headerAreas == null) {
				
				//	this one's too large (higher than an inch, wider than three inches) to be a page header (would just waste too much space)
				if (((regions[r].bounds.bottom - regions[r].bounds.top) > pageImageDpi) && ((regions[r].bounds.right - regions[r].bounds.left) > (pageImageDpi * 3))) {
					pm.setInfo(" --> too large");
					continue;
				}
				
				//	get words
				ImWord[] regionWords = regions[r].getWords();
				if (regionWords.length == 0)
					continue;
				Arrays.sort(regionWords, ImUtils.textStreamOrder);
				
				//	too many words for a page header
				if (regionWords.length > 30) {
					pm.setInfo(" --> too many words (" + regionWords.length + ")");
					continue;
				}
				
				//	this one has been worked on before (we still need page headers for page number detection, though)
				if (!ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(regionWords[0].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_PAGE_TITLE.equals(regionWords[0].getTextStreamType()))
					continue;
				
				//	check if any regions above or below
				boolean regionsAbove = false;
				boolean regionsBelow = false;
				for (int cr = 0; cr < regions.length; cr++) {
					if (cr == r)
						continue;
					if (LINE_ANNOTATION_TYPE.equals(regions[cr].getType()))
						continue;
					
					//	test if regions are really above/below one another
					if ((regions[r].bounds.right <= regions[cr].bounds.left) || (regions[cr].bounds.right <= regions[r].bounds.left))
						continue;
					
					//	test relative position
					if (regions[r].bounds.bottom <= regions[cr].bounds.top)
						regionsBelow = true;
					if (regions[cr].bounds.bottom <= regions[r].bounds.top)
						regionsAbove = true;
					
					//	do we need to check any further?
					if (regionsAbove && regionsBelow)
						break;
				}
				
				//	remember region of interest
				if (!regionsAbove && (regions[r].bounds.bottom < (page.bounds.bottom / 2))) {
					pageData.topRegions.add(regions[r]);
					pm.setInfo(" --> found top region");
				}
				if (!regionsBelow && (regions[r].bounds.top > (page.bounds.bottom / 2))) {
					pageData.bottomRegions.add(regions[r]);
					pm.setInfo(" --> found bottom region");
				}
			}
			
			//	use header areas
			else for (int h = 0; h < headerAreas.length; h++) {
				if (!headerAreas[h].includes(regions[r].bounds, false))
					continue;
				
				//	use only top region list if we know header areas
				pageData.topRegions.add(regions[r]);
				pm.setInfo(" --> found header region");
				
				//	we're done with this region, make sure not to add it twice
				break;
			}
		}
		
		//	finally ...
		return pageData;
	}
	
	private void addPageNumberCandidates(List pageNumberCandidates, ImWord pageNumberStartPart, HashSet pageNumberPartIDs, int dpi, ProgressMonitor pm) {
		String pageNumberString = pageNumberStartPart.getString();
		this.addPageNumberCandidates(pageNumberCandidates, pageNumberStartPart, pageNumberStartPart, pageNumberString, pm);
		for (ImWord imw = pageNumberStartPart.getNextWord(); imw != null; imw = imw.getNextWord()) {
			if (imw.pageId != pageNumberStartPart.pageId)
				break; // off page
			if ((imw.centerY < pageNumberStartPart.bounds.top) || (pageNumberStartPart.bounds.bottom < imw.centerY))
				break; // out off line
			if (imw.bounds.left < pageNumberStartPart.bounds.right)
				break; // out of left-to-right order
			if ((dpi / 13) < (imw.bounds.left - pageNumberStartPart.bounds.right))
				break; // too far away (more than 2 mm)
			if (!pageNumberPartIDs.contains(imw.getLocalID()))
				break;
			pageNumberString += imw.getString();
			boolean newPageNumbers = this.addPageNumberCandidates(pageNumberCandidates, pageNumberStartPart, imw, pageNumberString, pm);
			if (!newPageNumbers)
				break; // no matches found
		}
	}
	
	private boolean addPageNumberCandidates(List pageNumberCandidates, ImWord firstWord, ImWord lastWord, String pageNumberString, ProgressMonitor pm) {
		
		//	page numbers with six or more digits are rather improbable ...
		if (pageNumberString.length() > 5)
			return false;
		
		//	extract possible interpretations, computing ambiguity along the way
		int [][] pageNumberDigits = new int[pageNumberString.length()][];
		int ambiguity = 1;
		for (int c = 0; c < pageNumberString.length(); c++) {
			String pageNumberCharacter = pageNumberString.substring(c, (c+1));
			pageNumberDigits[c] = ((int[]) this.ocrPageNumberCharacterTranslations.get(pageNumberCharacter));
			if (pageNumberDigits[c] == null)
				return false; // invalid character
			ambiguity *= pageNumberDigits[c].length;
		}
		
		//	the first digit is never zero ...
		if ((pageNumberDigits[0].length == 1) && (pageNumberDigits[0][0] == 0))
			return false;
		
		//	create all candidate values
		TreeSet values = this.getAllPossibleValues(pageNumberDigits);
		if (values.size() == 0)
			return false;
		pm.setInfo("   - got " + values.size() + " possible values");
		
		//	compute fuzzyness for each value
		for (Iterator vit = values.iterator(); vit.hasNext();) {
			String pageNumberValue = ((String) vit.next());
			pm.setInfo("     - " + pageNumberValue);
			int fuzzyness = 0;
			for (int d = 0; d < pageNumberValue.length(); d++) {
				if (pageNumberValue.charAt(d) != pageNumberString.charAt(d))
					fuzzyness++;
			}
			pageNumberCandidates.add(new PageNumber(pageNumberString, Integer.parseInt(pageNumberValue), fuzzyness, ambiguity, firstWord, lastWord));
		}
		
		//	did we add anything?
		return true;
	}
	
	private TreeSet getAllPossibleValues(int[][] baseDigits) {
		TreeSet allPossibleValues = new TreeSet();
		int[] digits = new int[baseDigits.length];
		addAllPossibleValues(baseDigits, digits, 0, allPossibleValues);
		return allPossibleValues;
	}
	
	private void addAllPossibleValues(int[][] baseDigits, int[] digits, int pos, TreeSet values) {
		if (pos == digits.length)
			values.add(this.getValueString(digits));
		else for (int d = 0; d < baseDigits[pos].length; d++) {
			digits[pos] = baseDigits[pos][d];
			addAllPossibleValues(baseDigits, digits, (pos+1), values);
		}
	}
	
	private String getValueString(int[] digits) {
		StringBuffer valueString = new StringBuffer();
		for (int d = 0; d < digits.length; d++)
			valueString.append("" + digits[d]);
		return valueString.toString();
	}
	
	private void scoreAndSelectPageNumbers(PageData[] pageData, ProgressMonitor pm) {
		
		//	work page by page
		for (int p = 0; p < pageData.length; p++) {
			
			//	get existing page number
			int pageNumber = -1;
			if (pageData[p].page.hasAttribute(PAGE_NUMBER_ATTRIBUTE)) try {
				pageNumber = Integer.parseInt((String) pageData[p].page.getAttribute(PAGE_NUMBER_ATTRIBUTE));
			} catch (NumberFormatException nfe) {}
			
			//	score page numbers
			for (int n = 0; n < pageData[p].pageNumberCandidates.size(); n++) {
				PageNumber pn = ((PageNumber) pageData[p].pageNumberCandidates.get(n));
				
				//	reward consistency with pre-existing page number
				if (pageNumber != -1)
					pn.score += pageData.length;
				
				//	look forward
				int fMisses = 0;
				for (int l = 1; (p+l) < pageData.length; l++) {
					PageNumber cpn = null;
					for (int c = 0; c < pageData[p+l].pageNumberCandidates.size(); c++) {
						cpn = ((PageNumber) pageData[p+l].pageNumberCandidates.get(c));
						if (cpn.isConsistentWith(pn)) {
							pn.score += (((float) l) / (1 + cpn.fuzzyness));
							break;
						}
						else cpn = null;
					}
					if (cpn == null) {
						fMisses++;
						if (fMisses == 3)
							break;
					}
				}
				
				//	look backward
				int bMisses = 0;
				for (int l = 1; l <= p; l++) {
					PageNumber cpn = null;
					for (int c = 0; c < pageData[p-l].pageNumberCandidates.size(); c++) {
						cpn = ((PageNumber) pageData[p-l].pageNumberCandidates.get(c));
						if (cpn.isConsistentWith(pn)) {
							pn.score += (((float) l) / (1 + cpn.fuzzyness));
							break;
						}
						else cpn = null;
					}
					if (cpn == null) {
						bMisses++;
						if (bMisses == 3)
							break;
					}
				}
				
				//	penalize fuzzyness
				pn.score /= (pn.fuzzyness+1);
				pm.setInfo("   - " + pn.valueStr + " (as " + pn.value + ", fuzzyness " + pn.fuzzyness + ", ambiguity " + pn.ambiguity + ") on page " + p + " ==> " + pn.score);
			}
			
			//	select page number
			for (int n = 0; n < pageData[p].pageNumberCandidates.size(); n++) {
				PageNumber pn = ((PageNumber) pageData[p].pageNumberCandidates.get(n));
				if (pn.score < 1)
					continue;
				if ((pageData[p].pageNumber == null) || (pageData[p].pageNumber.score < pn.score))
					pageData[p].pageNumber = pn;
			}
			if (pageData[p].pageNumber == null)
				pm.setInfo(" --> could not determine page number of page " + p + ".");
			else pm.setInfo(" --> page number of " + p + " identified as " + pageData[p].pageNumber.value + " (score " + pageData[p].pageNumber.score + ")");
		}
	}
	
	private void checkPageNumberSequence(PageData[] pageData, ProgressMonitor pm) {
		for (int p = 0; p < pageData.length; p++) {
			if (pageData[p].pageNumber == null)
				continue;
			
			//	get adjacent page numbers
			PageNumber bpn = ((p == 0) ? null : pageData[p-1].pageNumber);
			PageNumber fpn = (((p+1) == pageData.length) ? null : pageData[p+1].pageNumber);
			if ((bpn == null) && (fpn == null))
				continue;
			
			//	check sequence consistent with what we can check against
			if (((bpn == null) || bpn.isConsistentWith(pageData[p].pageNumber)) && ((fpn == null) || fpn.isConsistentWith(pageData[p].pageNumber)))
				continue;
			
			//	correct that one oddjob in the middle
			if ((bpn != null) && (fpn != null) && bpn.isConsistentWith(fpn)) {
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " for sequence inconsistency.");
				pageData[p].pageNumber = null;
				for (int n = 0; n < pageData[p].pageNumberCandidates.size(); n++) {
					PageNumber pn = ((PageNumber) pageData[p].pageNumberCandidates.get(n));
					if (pn.isConsistentWith(bpn) && pn.isConsistentWith(fpn)) {
						pageData[p].pageNumber = pn;
						pm.setInfo("   --> re-assigned to " + pageData[p].pageNumber.value);
						break;
					}
				}
				continue;
			}
			
			//	one side not set, other inconsistent and far more secure
			if ((bpn == null) && !fpn.isConsistentWith(pageData[p].pageNumber) && ((pageData[p].pageNumber.score * 2) < fpn.score)) {
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " for sequence front edge inconsistency.");
				pageData[p].pageNumber = null;
				continue;
			}
			else if ((fpn == null) && !bpn.isConsistentWith(pageData[p].pageNumber) && ((pageData[p].pageNumber.score * 2) < bpn.score)) {
				pm.setInfo("   - eliminated page number " + pageData[p].pageNumber.value + " in page " + p + " for sequence back edge inconsistency.");
				pageData[p].pageNumber = null;
				continue;
			}
		}
	}
	
	private void fillInMissingPageNumbers(PageData[] pageData, ProgressMonitor pm) {
		
		//	make sure not to extrapolate to page numbers that are already taken
		HashSet existingPageNumbers = new HashSet();
		for (int p = 0; p < pageData.length; p++) {
			if (pageData[p].pageNumber != null)
				existingPageNumbers.add("" + pageData[p].pageNumber.value);
		}
		
		//	do backward sequence extrapolation
		PageNumber bPageNumber = null;
		for (int p = (pageData.length - 2); p >= 0; p--) {
			if (pageData[p].pageNumber != null) {
				bPageNumber = pageData[p].pageNumber;
				continue;
			}
			if (bPageNumber == null)
				continue;
			PageNumber ePageNumber = new PageNumber(p, (bPageNumber.value - bPageNumber.pageId + p));
			if (existingPageNumbers.add("" + ePageNumber.value)) {
				pageData[p].pageNumber = ePageNumber;
				pageData[p].page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + ePageNumber.value));
				bPageNumber = ePageNumber;
				pm.setInfo(" --> backward-extrapolated page number of page " + p + " to " + pageData[p].pageNumber.value);
			}
			else pm.setInfo(" --> could not backward-extrapolated page number of page " + p + ", page number " + ePageNumber.value + " already assigned");
		}
		
		//	do forward sequence extrapolation
		PageNumber fPageNumber = null;
		for (int p = 1; p < pageData.length; p++) {
			if (pageData[p].pageNumber != null) {
				fPageNumber = pageData[p].pageNumber;
				continue;
			}
			if (fPageNumber == null)
				continue;
			PageNumber ePageNumber = new PageNumber(p, (bPageNumber.value - bPageNumber.pageId + p));
			if (existingPageNumbers.add("" + ePageNumber.value)) {
				pageData[p].pageNumber = ePageNumber;
				pageData[p].page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + ePageNumber.value));
				fPageNumber = ePageNumber;
				pm.setInfo(" --> forward-extrapolated page number of page " + p + " to " + pageData[p].pageNumber.value);
			}
			else pm.setInfo(" --> could not forward-extrapolated page number of page " + p + ", page number " + ePageNumber.value + " already assigned");
		}
	}
	
	private void annotatePageNumbers(PageData pageData, HashSet pageNumberWordIDs) {
		if (pageData.pageNumber == null)
			return;
		pageData.page.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + pageData.pageNumber.value));
		if (pageData.pageNumber.firstWord == null)
			return;
		ImAnnotation pageNumber = pageData.page.getDocument().addAnnotation(pageData.pageNumber.firstWord, pageData.pageNumber.lastWord, PAGE_NUMBER_TYPE);
		pageNumber.setAttribute("value", ("" + pageData.pageNumber.value));
		pageNumber.setAttribute("score", ("" + pageData.pageNumber.score));
		pageNumber.setAttribute("ambiguity", ("" + pageData.pageNumber.ambiguity));
		pageNumber.setAttribute("fuzzyness", ("" + pageData.pageNumber.fuzzyness));
		for (ImWord imw = pageData.pageNumber.firstWord; imw != null; imw = imw.getNextWord()) {
			pageNumberWordIDs.add(imw.getLocalID());
			if (imw == pageData.pageNumber.lastWord)
				break;
		}
	}
	
	private static class PageData {
		final ImPage page;
		
		List topRegions = new ArrayList();
		List bottomRegions = new ArrayList();
		
		List pageNumberCandidates = new ArrayList(1);
		PageNumber pageNumber = null;
		
		PageData(ImPage page) {
			this.page = page;
		}
	}
	
	private class PageNumber {
		final int pageId;
		final String valueStr;
		final int value; // the integer value
		final int fuzzyness; // how many characters do not match our integer value
		final int ambiguity; // how many possible values are there for our word (sequence), including this one
		final ImWord firstWord;
		final ImWord lastWord;
		float score = 0;
		
		//	used for born-digital documents, where we have no need for interpretation
		PageNumber(ImWord word) {
			this.pageId = word.pageId;
			this.valueStr = word.getString();
			this.value = Integer.parseInt(word.getString());
			this.fuzzyness = 0;
			this.ambiguity = 1;
			this.firstWord = word;
			this.lastWord = word;
		}
		
		//	used for scanned documents
		PageNumber(String valueStr, int value, int fuzzyness, int ambiguity, ImWord firstWord, ImWord lastWord) {
			this.pageId = firstWord.pageId;
			this.valueStr = valueStr;
			this.value = value;
			this.fuzzyness = fuzzyness;
			this.ambiguity = ambiguity;
			this.firstWord = firstWord;
			this.lastWord = lastWord;
		}
		
		//	used for extrapolation
		PageNumber(int pageId, int value) {
			this.pageId = pageId;
			this.valueStr = null;
			this.value = value;
			this.fuzzyness = Integer.MAX_VALUE;
			this.ambiguity = Integer.MAX_VALUE;
			this.firstWord = null;
			this.lastWord = null;
		}
		
		public boolean equals(Object obj) {
			return (((PageNumber) obj).value == this.value);
		}
		public int hashCode() {
			return this.value;
		}
		public String toString() {
			return ("" + this.value);
		}
		boolean isConsistentWith(PageNumber pn) {
			return ((this.value - pn.value) == (this.pageId - pn.pageId));
		}
//		float getAdjustedScore() {
//			return (((this.fuzzyness == Integer.MAX_VALUE) || (this.ambiguity == Integer.MAX_VALUE)) ? 0 : (this.score / (this.fuzzyness + this.ambiguity)));
//		}
	}
	
	private static final Comparator pageNumberValueOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			PageNumber pn1 = ((PageNumber) obj1);
			PageNumber pn2 = ((PageNumber) obj2);
			return ((pn1.value == pn2.value) ? (pn1.fuzzyness - pn2.fuzzyness) : (pn1.value - pn2.value));
		}
	};
	
	/* TODO
Create HeadingStructureEditor
- show all headings, in text order, in list
- display hierarchy by means of indents (simply use spaces) ...
- ... using drop-down for heading level
--> index headings by style, and update level for all headings in same style together ...
     ... by default, but offer checkbox disabling that behavior ...
     ... and split affected indexed heading styles on update if checkbox is unchecked
- offer heading removal option
- use non-modal dialog  to allow for marking headings missed by auto-detection ...
- ... and document listener to keep in sync with user edits

- UPDATE: do NOT index headings by style ...
- ... but by level (which is based on style originally) ...
- ... and transfer headings between levels by their original group if group transfer is enabled
	 */
	
	//	TODO factor out heading detection and editing to separate plugin, and make this one dependent on the latter
	
	//	TODO back up headings with rule-based hierarchy classification
	
	public static void main(String[] args) throws Exception {
		DocumentStructureDetectorProvider dsdp = new DocumentStructureDetectorProvider();
		dsdp.setDataProvider(new PluginDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/DocumentStructureDetectorData/")));
		dsdp.init();
		dsdp.debug = true;
		
		final String testFileName;
//		testFileName = "zt00872.pdf.imf";
//		testFileName = "zt00904.pdf.test.imf";
//		testFileName = "arac-38-02-328.pdf.imf";
//		testFileName = "zt03652p155.pdf.imf";
//		testFileName = "zt03456p035.pdf.test.imf";
//		testFileName = "6860_Cutler_1990_Bul_8_105-108.imf";
//		testFileName = "dikow_2012.test.pdf.imf";
//		testFileName = "ZJLS_Hertach2015.pdf.raw.imf"; // heading detection and ranking hampered by sloppy layout, pretty rough case this one
//		testFileName = "1_5_Farkac.pdf.imf"; // heading detection and ranking hampered by sloppy layout, pretty rough case this one
//		testFileName = "zt03911p493.pdf.raw.imf"; // key entries detected as footnotes on page 4 ==> fixed via font size
//		testFileName = "ZM1967042005.pdf.imf"; // two footnotes not detected on page 0
//		testFileName = "IJSEM/19.full.pdf.raw.imf"; // caption on page 2 mistaken for table
		testFileName = "zt03881p227.pdf.imf"; // fails to detect full-page table on page 5
//		testFileName = "zt03881p227.pdf.imf"; // cuts off column headings on page 8
		FileInputStream fis = new FileInputStream(new File("E:/Testdaten/PdfExtract/" + testFileName));
		ImDocument doc = ImfIO.loadDocument(fis);
		fis.close();
		
		DocumentStyle.addProvider(new DocumentStyle.Provider() {
			public Properties getStyleFor(Attributed doc) {
//				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleProviderData/zootaxa.2007.journal_article.docStyle"));
				Settings ds = Settings.loadSettings(new File("E:/GoldenGATEv3/Plugins/DocumentStyleProviderData/ijsem.0000.journal_article.docStyle"));
				return ds.toProperties();
			}
		});
		
		dsdp.detectDocumentStructure(doc, true, ProgressMonitor.dummy);
//		
//		String[] params = DocumentStyle.getParameterNames();
//		Arrays.sort(params);
//		for (int p = 0; p < params.length; p++)
//			System.out.println(params[p] + " = \"" + DocumentStyle.getParameterValueClass(params[p]).getName() + "\";");
	}
}